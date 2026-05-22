import { useEffect, useState, useRef } from "react";
import { alertApi, patientApi } from "../../lib/api";
import type { HealthAlertEvent } from "../../lib/api";

const severityColors: Record<string, string> = {
  CRITICAL: "bg-red-100 text-red-800 border-red-200",
  WARNING: "bg-yellow-100 text-yellow-800 border-yellow-200",
  INFO: "bg-blue-100 text-blue-800 border-blue-200",
};

const severityOrder = ["CRITICAL", "WARNING", "INFO"];

export default function AlertsPage() {
  const [alerts, setAlerts] = useState<HealthAlertEvent[]>([]);
  const [patientNames, setPatientNames] = useState<Record<string, string>>({});
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const pendingIds = useRef<Set<string>>(new Set());

  useEffect(() => {
    const fetchAlerts = async () => {
      try {
        const data = await alertApi.getAll();
        setAlerts(data);
        setLastUpdated(new Date());

        const newIds = data
          .map((a) => a.patientId)
          .filter((id) => !pendingIds.current.has(id));

        newIds.forEach((id) => {
          pendingIds.current.add(id);
          patientApi
            .getOne(id)
            .then((p) =>
              setPatientNames((prev) => ({
                ...prev,
                [id]: `${p.firstName} ${p.lastName}`,
              }))
            )
            .catch(() =>
              setPatientNames((prev) => ({
                ...prev,
                [id]: id.slice(0, 8) + "…",
              }))
            );
        });
      } catch {
        // silent
      }
    };

    fetchAlerts();
    const interval = setInterval(fetchAlerts, 5000);
    return () => clearInterval(interval);
  }, []);

  const sorted = [...alerts].sort(
    (a, b) =>
      severityOrder.indexOf(a.severity) - severityOrder.indexOf(b.severity)
  );

  const counts = {
    CRITICAL: alerts.filter((a) => a.severity === "CRITICAL").length,
    WARNING: alerts.filter((a) => a.severity === "WARNING").length,
    INFO: alerts.filter((a) => a.severity === "INFO").length,
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Health Alerts</h1>
        {lastUpdated && (
          <span className="text-xs text-gray-400 flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-green-400 animate-pulse inline-block" />
            Live · {lastUpdated.toLocaleTimeString()}
          </span>
        )}
      </div>

      <div className="grid grid-cols-3 gap-3">
        <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-center">
          <div className="text-3xl font-bold text-red-700">{counts.CRITICAL}</div>
          <div className="text-xs text-red-600 font-semibold uppercase tracking-wide mt-1">
            Critical
          </div>
        </div>
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 text-center">
          <div className="text-3xl font-bold text-yellow-700">{counts.WARNING}</div>
          <div className="text-xs text-yellow-600 font-semibold uppercase tracking-wide mt-1">
            Warning
          </div>
        </div>
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-center">
          <div className="text-3xl font-bold text-blue-700">{counts.INFO}</div>
          <div className="text-xs text-blue-600 font-semibold uppercase tracking-wide mt-1">
            Info
          </div>
        </div>
      </div>

      {sorted.length === 0 ? (
        <p className="text-gray-500 text-center py-12">
          No alerts. All vitals are normal.
        </p>
      ) : (
        <div className="space-y-3">
          {sorted.map((a) => (
            <div
              key={a.alertId}
              className={`border rounded-lg p-4 ${severityColors[a.severity] || severityColors.INFO}`}
            >
              <div className="flex items-center justify-between mb-1">
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-sm uppercase">
                    {a.severity}
                  </span>
                  <span className="text-sm font-medium opacity-70">
                    {patientNames[a.patientId] ?? "…"}
                  </span>
                </div>
                <span className="text-xs opacity-60">
                  {new Date(a.timestamp).toLocaleString()}
                </span>
              </div>
              <p className="font-medium">{a.alertType.replace(/_/g, " ")}</p>
              <p className="text-sm mt-1">{a.message}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
