"""
Job 1 — Download CSV data from external sources.

Downloads the smallest pending year from the URL config file.
Stores raw CSV in the local filesystem for Bronze ingestion.
"""

import os
import sys
import time
from pathlib import Path

import requests

# ── Config (env-configurable) ────────────────────────────────────────────────
RAW_DIR    = Path(os.environ.get("RAW_CSV_DIR", "/opt/spark-jobs/data/raw_csv"))
URLS_FILE  = Path(os.environ.get("URLS_FILE", "/opt/spark-jobs/config/urls_to_process.txt"))
PROXY_HOST = os.environ.get("PROXY_HOST", "")
PROXY_PORT = os.environ.get("PROXY_PORT", "3128")

RAW_DIR.mkdir(parents=True, exist_ok=True)

PROXIES = (
    {"http": f"http://{PROXY_HOST}:{PROXY_PORT}", "https": f"http://{PROXY_HOST}:{PROXY_PORT}"}
    if PROXY_HOST else {}
)


def get_smallest_pending_url(filepath: Path):
    pending_entries = []
    if not filepath.exists():
        raise FileNotFoundError(f"URL file not found: {filepath}")

    with filepath.open("r") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("|")
            if len(parts) != 3:
                continue
            url, year_str, status = [p.strip() for p in parts]
            if status.lower() == "pending":
                try:
                    pending_entries.append((int(year_str), url))
                except ValueError:
                    continue

    if not pending_entries:
        return None, None

    pending_entries.sort(key=lambda x: x[0])
    return pending_entries[0][1], str(pending_entries[0][0])


def download_file(url: str, dest_path: Path, chunk_size: int = 1024 * 1024) -> int:
    print(f"  Downloading: {url}")
    print(f"  Destination: {dest_path}")
    with requests.get(url, stream=True, proxies=PROXIES, timeout=600) as r:
        r.raise_for_status()
        total_size = int(r.headers.get("Content-Length", 0)) or None
        downloaded = 0
        with dest_path.open("wb") as f:
            for chunk in r.iter_content(chunk_size=chunk_size):
                if not chunk:
                    continue
                f.write(chunk)
                downloaded += len(chunk)
                if total_size:
                    pct = downloaded * 100 / total_size
                    print(f"  {downloaded / 1024 / 1024:.1f} MB / {total_size / 1024 / 1024:.1f} MB ({pct:.0f}%)", end="\r")
    print()
    return downloaded


def main():
    print("=" * 50)
    print("  Job 1 — Download CSV")
    print("=" * 50)

    try:
        url, year = get_smallest_pending_url(URLS_FILE)
    except Exception as e:
        print(f"  Error: {e}")
        sys.exit(1)

    if not url or not year:
        print("  No pending URLs to process.")
        sys.exit(0)

    print(f"  Year: {year}")
    dest_file = RAW_DIR / f"SPARCS_{year}.csv"

    if dest_file.exists():
        print(f"  File already exists: {dest_file}")

    try:
        size = download_file(url, dest_file)
        if size < 10 * 1024 * 1024:
            print(f"  Warning: file is small ({size} bytes)")
        print(f"  Done: {dest_file} ({size:,} bytes)")
    except Exception as e:
        print(f"  Download failed: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
