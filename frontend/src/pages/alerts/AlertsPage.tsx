import { useEffect, useState } from "react";
import { alertApi } from "../../lib/api";
import type { HealthAlertEvent } from "../../lib/api";

const severityColors: Record<string, string> = {
  CRITICAL: "bg-red-100 text-red-800 border-red-200",
  WARNING: "bg-yellow-100 text-yellow-800 border-yellow-200",
  INFO: "bg-blue-100 text-blue-800 border-blue-200",
};

const severityOrder = ["CRITICAL", "WARNING", "INFO"];

export default function AlertsPage() {
  const [alerts, setAlerts] = useState<HealthAlertEvent[]>([]);

  useEffect(() => {
    const fetchAlerts = async () => {
      try {
        const data = await alertApi.getAll();
        setAlerts(data);
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

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">
        Health Alerts
        {alerts.length > 0 && (
          <span className="ml-2 text-sm font-normal text-gray-500">
            ({alerts.length} active)
          </span>
        )}
      </h1>

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
                <span className="font-semibold text-sm uppercase">
                  {a.severity}
                </span>
                <span className="text-xs opacity-75">
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
