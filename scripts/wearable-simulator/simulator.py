"""
Wearable Health Simulator for EpiSante.

Generates realistic vital signs for simulated patients and sends them
to the Ingest Wearables Service via HTTP. Supports multiple patients,
demographic-aware baselines, configurable anomaly injection, and
stress testing at 100+ messages/sec.
"""

import argparse
import json
import os
import sys
import threading
import time
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from dataclasses import dataclass

import numpy as np
import requests

# ---------------------------------------------------------------------------
# Patient profiles
# ---------------------------------------------------------------------------

@dataclass
class PatientProfile:
    patient_id: str
    first_name: str
    last_name: str
    age: int
    gender: str
    doctor_id: str
    device_id: str

SIMULATED_PATIENTS = [
    PatientProfile("550e8400-e29b-41d4-a716-446655440000", "Alice", "Dupont", 8, "FEMALE", "660e8400-e29b-41d4-a716-446655440001", "watch-alice-01"),
    PatientProfile("550e8400-e29b-41d4-a716-446655440010", "Bob", "Martin", 35, "MALE", "660e8400-e29b-41d4-a716-446655440001", "watch-bob-01"),
    PatientProfile("550e8400-e29b-41d4-a716-446655440020", "Claire", "Bernard", 72, "FEMALE", "660e8400-e29b-41d4-a716-446655440002", "watch-claire-01"),
    PatientProfile("550e8400-e29b-41d4-a716-446655440030", "David", "Petit", 45, "MALE", "660e8400-e29b-41d4-a716-446655440002", "watch-david-01"),
    PatientProfile("550e8400-e29b-41d4-a716-446655440040", "Emma", "Moreau", 28, "FEMALE", "660e8400-e29b-41d4-a716-446655440001", "watch-emma-01"),
]

# ---------------------------------------------------------------------------
# Demographic baselines
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

    hr_val = int(np.clip(np.random.normal(hr[0], hr[1]), 40, 220))
    bp_sys_val = int(np.clip(np.random.normal(bp_sys[0], bp_sys[1]), 60, 250))
    bp_dia_val = int(np.clip(np.random.normal(bp_dia[0], bp_dia[1]), 30, 150))
    spo2_val = int(np.clip(np.random.normal(spo2[0], spo2[1]), 70, 100))
    temp_val = round(np.clip(np.random.normal(temp[0], temp[1]), 34.0, 42.0), 1)

    is_anomaly = np.random.random() < anomaly_rate
    if is_anomaly:
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


def is_anomaly(metric: dict) -> bool:
    return (metric["heartRate"] > 120 or metric["heartRate"] < 50 or
            metric["bloodPressureSystolic"] > 160 or
            metric["spO2"] < 90 or
            metric["temperature"] > 39.0)

# ---------------------------------------------------------------------------
# Stats tracker (thread-safe)
# ---------------------------------------------------------------------------

class Stats:
    def __init__(self):
        self._lock = threading.Lock()
        self.sent = 0
        self.errors = 0
        self.anomalies = 0
        self.per_patient = {}
        self.start_time = time.time()
        self.latencies = []

    def record(self, patient_name: str, success: bool, anomaly: bool, latency: float):
        with self._lock:
            if success:
                self.sent += 1
            else:
                self.errors += 1
            if anomaly:
                self.anomalies += 1
            self.latencies.append(latency)
            if len(self.latencies) > 1000:
                self.latencies = self.latencies[-500:]
            if patient_name not in self.per_patient:
                self.per_patient[patient_name] = {"sent": 0, "anomalies": 0}
            if success:
                self.per_patient[patient_name]["sent"] += 1
            if anomaly:
                self.per_patient[patient_name]["anomalies"] += 1

    def snapshot(self):
        with self._lock:
            elapsed = time.time() - self.start_time
            rate = self.sent / elapsed if elapsed > 0 else 0
            p50 = np.percentile(self.latencies, 50) * 1000 if self.latencies else 0
            p95 = np.percentile(self.latencies, 95) * 1000 if self.latencies else 0
            return {
                "sent": self.sent,
                "errors": self.errors,
                "anomalies": self.anomalies,
                "elapsed": elapsed,
                "rate": rate,
                "p50_ms": p50,
                "p95_ms": p95,
                "per_patient": dict(self.per_patient),
            }

# ---------------------------------------------------------------------------
# Send metric (used by both normal and stress mode)
# ---------------------------------------------------------------------------

session = requests.Session()

def send_metric(url: str, patient: PatientProfile, anomaly_rate: float, stats: Stats):
    metric = generate_metric(patient, anomaly_rate)
    anomaly = is_anomaly(metric)
    t0 = time.time()
    try:
        resp = session.post(url, json=metric, timeout=5)
        latency = time.time() - t0
        success = resp.status_code == 201
        stats.record(patient.first_name, success, anomaly, latency)
        return metric, success, anomaly
    except requests.exceptions.RequestException:
        latency = time.time() - t0
        stats.record(patient.first_name, False, anomaly, latency)
        return metric, False, anomaly

# ---------------------------------------------------------------------------
# Live dashboard (clears terminal and redraws)
# ---------------------------------------------------------------------------

def render_dashboard(stats: Stats, patients: list, mode: str):
    s = stats.snapshot()
    lines = []
    lines.append("\033[2J\033[H")  # clear screen
    lines.append("=" * 60)
    lines.append(f"  EpiSante Wearable Simulator — {mode.upper()} MODE")
    lines.append("=" * 60)
    lines.append("")
    lines.append(f"  Elapsed:     {s['elapsed']:.0f}s")
    lines.append(f"  Sent:        {s['sent']:,}")
    lines.append(f"  Errors:      {s['errors']:,}")
    lines.append(f"  Anomalies:   {s['anomalies']:,}")
    lines.append(f"  Throughput:  {s['rate']:.1f} req/s")
    lines.append(f"  Latency:     p50={s['p50_ms']:.0f}ms  p95={s['p95_ms']:.0f}ms")
    lines.append("")
    lines.append("  Patient          Sent    Anomalies")
    lines.append("  " + "-" * 40)
    for p in patients:
        ps = s["per_patient"].get(p.first_name, {"sent": 0, "anomalies": 0})
        lines.append(f"  {p.first_name:16s} {ps['sent']:>6,}    {ps['anomalies']:>6,}")
    lines.append("")
    lines.append("  Press Ctrl+C to stop")
    print("\n".join(lines), flush=True)

# ---------------------------------------------------------------------------
# Normal mode
# ---------------------------------------------------------------------------

def run_normal(args, patients, stats):
    print(f"  Target URL:   {args.url}")
    print(f"  Patients:     {len(patients)}")
    print(f"  Interval:     {args.interval}s")
    print(f"  Anomaly rate: {args.anomaly_rate}")
    print("-" * 50)

    try:
        while True:
            if args.duration and (time.time() - stats.start_time) >= args.duration:
                break
            batch_start = time.time()
            for patient in patients:
                metric, success, anomaly = send_metric(args.url, patient, args.anomaly_rate, stats)
                marker = " ANOMALY" if anomaly else ""
                print(
                    f"  {patient.first_name:8s} | "
                    f"HR={metric['heartRate']:3d}  "
                    f"BP={metric['bloodPressureSystolic']:3d}/{metric['bloodPressureDiastolic']:3d}  "
                    f"SpO2={metric['spO2']:2d}%  "
                    f"T={metric['temperature']:.1f}C"
                    f"{marker}"
                )
            elapsed = time.time() - batch_start
            time.sleep(max(0, args.interval - elapsed))
    except KeyboardInterrupt:
        pass

# ---------------------------------------------------------------------------
# Stress test mode
# ---------------------------------------------------------------------------

def run_stress(args, patients, stats):
    workers = args.workers
    print(f"  STRESS TEST MODE")
    print(f"  Target URL:   {args.url}")
    print(f"  Patients:     {len(patients)}")
    print(f"  Workers:      {workers}")
    print(f"  Anomaly rate: {args.anomaly_rate}")
    if args.duration:
        print(f"  Duration:     {args.duration}s")
    print("-" * 50)
    print("  Ramping up...\n")

    stop = threading.Event()

    def worker_loop():
        while not stop.is_set():
            patient = patients[np.random.randint(0, len(patients))]
            send_metric(args.url, patient, args.anomaly_rate, stats)

    try:
        with ThreadPoolExecutor(max_workers=workers) as pool:
            futures = [pool.submit(worker_loop) for _ in range(workers)]

            while not stop.is_set():
                if args.duration and (time.time() - stats.start_time) >= args.duration:
                    stop.set()
                    break
                render_dashboard(stats, patients, "stress")
                time.sleep(1)

    except KeyboardInterrupt:
        stop.set()

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="EpiSante Wearable Simulator")
    parser.add_argument("--url", default="http://localhost:9096/api/wearable/metrics",
                        help="Ingest service URL")
    parser.add_argument("--interval", type=float, default=2.0,
                        help="Seconds between batches (normal mode)")
    parser.add_argument("--anomaly-rate", type=float, default=0.1,
                        help="Probability of anomaly per reading (0-1)")
    parser.add_argument("--patients", type=int, default=None,
                        help="Number of patients to simulate (default: all)")
    parser.add_argument("--stress", action="store_true",
                        help="Enable stress test mode (100+ msg/sec)")
    parser.add_argument("--workers", type=int, default=20,
                        help="Concurrent workers in stress mode (default: 20)")
    parser.add_argument("--duration", type=int, default=None,
                        help="Run for N seconds then stop (default: infinite)")
    args = parser.parse_args()

    patients = SIMULATED_PATIENTS[:args.patients] if args.patients else SIMULATED_PATIENTS
    stats = Stats()

    print("=" * 50)
    print("  EpiSante Wearable Simulator")
    print("=" * 50)

    if args.stress:
        run_stress(args, patients, stats)
    else:
        run_normal(args, patients, stats)

    # Final summary
    s = stats.snapshot()
    print("\n" + "=" * 50)
    print("  RESULTS")
    print("=" * 50)
    print(f"  Duration:    {s['elapsed']:.0f}s")
    print(f"  Total sent:  {s['sent']:,}")
    print(f"  Errors:      {s['errors']:,}")
    print(f"  Anomalies:   {s['anomalies']:,}")
    print(f"  Throughput:  {s['rate']:.1f} req/s")
    print(f"  Latency:     p50={s['p50_ms']:.0f}ms  p95={s['p95_ms']:.0f}ms")
    print("")
    print("  Per-patient breakdown:")
    for name, ps in s["per_patient"].items():
        print(f"    {name:16s}  sent={ps['sent']:,}  anomalies={ps['anomalies']:,}")
    print("=" * 50)


if __name__ == "__main__":
    main()
