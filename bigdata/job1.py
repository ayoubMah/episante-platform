import sys
import requests
from pathlib import Path

# -------------------------
# Configuration générale
# -------------------------

PROXY_HOST = "192.168.1.60"
PROXY_PORT = 3128

BASE_DIR = Path("/Data_Sirius")
RAW_DIR = BASE_DIR / "raw_csv"
RAW_DIR.mkdir(parents=True, exist_ok=True)

URLS_FILE = Path("/home/spark/spark_jobs/config/urls_to_process.txt")

PROXIES = {
    "http": f"http://{PROXY_HOST}:{PROXY_PORT}",
    "https": f"http://{PROXY_HOST}:{PROXY_PORT}",
}

# -------------------------
# Utils lecture / choix URL
# -------------------------
def get_smallest_pending_url(filepath: Path):
    """
    Retourne (url, year) avec la plus petite année en status 'pending'.
    Si aucune URL pending, retourne (None, None).
    """
    pending_entries = []

    if not filepath.exists():
        raise FileNotFoundError(f"Fichier URLs introuvable : {filepath}")

    with filepath.open("r") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("|")
            if len(parts) != 3:
                print(f" Ligne ignorée (format invalide) : {line}")
                continue

            url, year_str, status = parts
            year_str = year_str.strip()
            status = status.strip().lower()

            if status == "pending":
                try:
                    year = int(year_str)
                    pending_entries.append((year, url))
                except ValueError:
                    print(f" Année invalide ignorée : {year_str} (ligne : {line})")

    if not pending_entries:
        return None, None

    # Tri par année et prise de la plus petite
    pending_entries.sort(key=lambda x: x[0])
    smallest_year, url = pending_entries[0]
    return url, str(smallest_year)

# -------------------------
# Téléchargement fichier
# -------------------------

def download_file(url: str, dest_path: Path, chunk_size: int = 1024 * 1024) -> int:
    """
    Télécharge un fichier en streaming via proxy et l'écrit sur disque,
    en affichant une progression (MB téléchargés / MB totaux).

    Retourne la taille finale du fichier en bytes.
    """
    print(f" Téléchargement depuis : {url}")
    print(f" Destination : {dest_path}")
    with requests.get(url, stream=True, proxies=PROXIES, timeout=600) as r:
        r.raise_for_status()

        total_size = r.headers.get("Content-Length")
        total_size = int(total_size) if total_size is not None else None
        downloaded = 0

        # Création du fichier en écriture binaire
        with dest_path.open("wb") as f:
            for chunk in r.iter_content(chunk_size=chunk_size):
                if not chunk:
                    continue
                f.write(chunk)
                downloaded += len(chunk)

                if total_size:
                    pct = downloaded * 100 / total_size
                    print(f"   Progression : {downloaded/1024/1024:.2f} MB "
                          f"/ {total_size/1024/1024:.2f} MB ({pct:.1f}%)",
                          end="\r")
                else:
                    print(f"   Progression : {downloaded/1024/1024:.2f} MB téléchargés",
                          end="\r")

    print()  # retour à la ligne
    print(f" Téléchargement terminé : {dest_path} ({downloaded} bytes)")
    return downloaded

# -------------------------
# Main
# -------------------------

def main():
    print("===== Job 1 - Téléchargement CSV SPARCS =====")

    try:
        url, year = get_smallest_pending_url(URLS_FILE)
    except Exception as e:
        print(f" Erreur lecture fichier URLs : {e}")
        sys.exit(1)

    if not url or not year:
        print(" Aucun fichier 'pending' à traiter. Rien à faire.")
        sys.exit(0)

    print(f" Fichier à traiter : année {year}")
    print(f" URL : {url}")

    dest_file = RAW_DIR / f"SPARCS_{year}.csv"

    
    if dest_file.exists():
        print(f"ℹ Fichier existe déjà : {dest_file}")
        print("   (Tu peux décider de le supprimer ou de sauter le téléchargement)")


    try:
        size = download_file(url, dest_file)


        if size < 10 * 1024 * 1024:  # < 10 MB = suspect
            print(f" ATTENTION : fichier {dest_file} très petit ({size} bytes). "
                  f"Vérifier l'URL ou le proxy.")

        print(" Job 1 terminé avec succès.")
        print(f"   Fichier téléchargé : {dest_file}")

        # Ici, on NE met PAS à jour le fichier d'URLs.
        # Ce sera géré par Airflow (XCom / task dédiée) pour rester idempotent.

    except Exception as e:
        print(f" Erreur pendant le téléchargement : {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()