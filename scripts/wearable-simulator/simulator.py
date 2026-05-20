"""
Wearable Health Simulator for EpiSante.

Generates realistic vital signs for simulated patients and sends them
to the Ingest Wearables Service via HTTP. Supports multiple patients,
demographic-aware baselines, and configurable anomaly injection.
"""

import argparse
import json
import time
import uuid
from datetime import datetime, timezone
from dataclasses import dataclass, asdict

import numpy as np
import requests

# ---------------------------------------------------------------------------
# Patient profiles used by the simulator.
# In production these would come from the patient-service API.
# ---------------------------------------------------------------------------

@dataclass
class PatientProfile:
    patient_id: str
    first_name: str
    last_name: str
    age: int
    gender: str          # "MALE" | "FEMALE"
    doctor_id: str
    device_id: str

SIMULATED_PATIENTS = [
    PatientProfile(
        patient_id="550e8400-e29b-41d4-a716-446655440000",
        first_name="Alice", last_name="Dupont",
        age=8, gender="FEMALE",
        doctor_id="660e8400-e29b-41d4-a716-446655440001",
        device_id="watch-alice-01",
    ),
    PatientProfile(
        patient_id="550e8400-e29b-41d4-a716-446655440010",
        first_name="Bob", last_name="Martin",
        age=35, gender="MALE",
        doctor_id="660e8400-e29b-41d4-a716-446655440001",
        device_id="watch-bob-01",
    ),
    PatientProfile(
        patient_id="550e8400-e29b-41d4-a716-446655440020",
        first_name="Claire", last_name="Bernard",
        age=72, gender="FEMALE",
        doctor_id="660e8400-e29b-41d4-a716-446655440002",
        device_id="watch-claire-01",
    ),
    PatientProfile(
        patient_id="550e8400-e29b-41d4-a716-446655440030",
        first_name="David", last_name="Petit",
        age=45, gender="MALE",
        doctor_id="660e8400-e29b-41d4-a716-446655440002",
        device_id="watch-david-01",
    ),
    PatientProfile(
        patient_id="550e8400-e29b-41d4-a716-446655440040",
        first_name="Emma", last_name="Moreau",
        age=28, gender="FEMALE",
        doctor_id="660e8400-e29b-41d4-a716-446655440001",
        device_id="watch-emma-01",
    ),
]

# ---------------------------------------------------------------------------
# Demographic baseline ranges
# Returns (hr_mean, hr_std, bp_sys_mean, bp_sys_std, bp_dia_mean, bp_dia_std,
#          spo2_mean, spo2_std, temp_mean, temp_std)
# ---------------------------------------------------------------------------

def get_baseline(age: int, gender: str):
    if age < 2:
        hr = (120, 15); bp_sys = (85, 10); bp_dia = (50, 8)
    elif age < 12:
        hr = (95, 15); bp_sys = (95, 10); bp_dia = (60, 8)
    elif age < 18:
        hr = (80, 12); bp_sys = (105, 10); bp_dia = (68, 8)
    elif age < 60:
        hr = (72, 10) if gender == "FEMALE" else (68, 10)
        bp_sys = (115, 10); bp_dia = (75, 8)
    else:
        hr = (74, 10) if gender == "FEMALE" else (70, 10)
        bp_sys = (125, 12); bp_dia = (78, 8)

    spo2 = (98, 1.5)
    temp = (37.0, 0.3)
    return hr, bp_sys, bp_dia, spo2, temp


def generate_metric(patient: PatientProfile, anomaly_rate: float) -> dict:
    hr, bp_sys, bp_dia, spo2, temp = get_baseline(patient.age, patient.gender)

    trigger_anomaly = np.random.random() < anomaly_rate

    if trigger_anomaly:
        anomaly_type = np.random.choice(["hr", "bp", "spo2", "temp"])
        if anomaly_type == "hr":
            hr_val = int(np.random.normal(hr[0] + 50, hr[1]))
        elif anomaly_type == "bp":
            bp_sys_val = int(np.random.normal(bp_sys[0] + 35, bp_sys[1]))
            bp_dia_val = int(np.random.normal(bp_dia[0] + 20, bp_dia[1]))
        elif anomaly_type == "spo2":
            spo2_val = int(np.clip(np.random.normal(spo2[0] - 10, spo2[1]), 70, 100))
        elif anomaly_type == "temp":
            temp_val = round(np.random.normal(temp[0] + 2.0, 0.5), 1)
    else:
        hr_val = int(np.clip(np.random.normal(hr[0], hr[1]), 40, 220))
        bp_sys_val = int(np.clip(np.random.normal(bp_sys[0], bp_sys[1]), 60, 250))
        bp_dia_val = int(np.clip(np.random.normal(bp_dia[0], bp_dia[1]), 30, 150))
        spo2_val = int(np.clip(np.random.normal(spo2[0], spo2[1]), 70, 100))
        temp_val = round(np.clip(np.random.normal(temp[0], temp[1]), 34.0, 42.0), 1)

    return {
        "patientId": patient.patient_id,
        "heartRate": hr_val,
        "bloodPressureSystolic": bp_sys_val,
        "bloodPressureDiastolic": bp_dia_val,
        "spO2": spo2_val,
        "temperature": temp_val,
        "timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "deviceId": patient.device_id,
    }


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="EpiSante Wearable Simulator")
    parser.add_argument("--url", default="http://localhost:9096/api/wearable/metrics",
                        help="Ingest service URL")
    parser.add_argument("--interval", type=float, default=2.0,
                        help="Seconds between batches")
    parser.add_argument("--anomaly-rate", type=float, default=0.1,
                        help="Probability of anomaly per patient per batch (0-1)")
    parser.add_argument("--patients", type=int, default=None,
                        help="Number of patients to simulate (default: all)")
    args = parser.parse_args()

    patients = SIMULATED_PATIENTS[:args.patients] if args.patients else SIMULATED_PATIENTS

    print(f" Wearable Simulator Started")
    print(f"   Target URL:   {args.url}")
    print(f"   Patients:     {len(patients)}")
    print(f"   Interval:     {args.interval}s")
    print(f"   Anomaly rate: {args.anomaly_rate}")
    print("─" * 50)

    sent = 0
    anomalies = 0
    start = time.time()

    try:
        while True:
            batch_start = time.time()
            for patient in patients:
                metric = generate_metric(patient, args.anomaly_rate)
                try:
                    resp = requests.post(args.url, json=metric, timeout=5)
                    sent += 1
                    if resp.status_code != 201:
                        print(f"⚠️  HTTP {resp.status_code} for {patient.first_name}")
                except requests.exceptions.RequestException as e:
                    print(f" Connection error: {e}")
                    time.sleep(5)
                    continue

                # Count anomalies roughly
                if (metric["heartRate"] > 120 or metric["heartRate"] < 50 or
                    metric["bloodPressureSystolic"] > 160 or
                    metric["spO2"] < 90 or
                    metric["temperature"] > 39.0):
                    anomalies += 1
                    marker = " ⚠️ ANOMALY"
                else:
                    marker = ""

                print(
                    f"  {patient.first_name:8s} | "
                    f"HR={metric['heartRate']:3d}  "
                    f"BP={metric['bloodPressureSystolic']:3d}/{metric['bloodPressureDiastolic']:3d}  "
                    f"SpO2={metric['spO2']:2d}%  "
                    f"T={metric['temperature']:.1f}°C"
                    f"{marker}"
                )

            elapsed = time.time() - batch_start
            sleep_time = max(0, args.interval - elapsed)
            time.sleep(sleep_time)

    except KeyboardInterrupt:
        duration = time.time() - start
        print(f"\n Stopped after {duration:.0f}s")
        print(f"   Total requests: {sent}")
        print(f"   Anomalies:      {anomalies}")
        print(f"   Avg rate:       {sent / duration:.1f} req/s")


if __name__ == "__main__":
    main()
