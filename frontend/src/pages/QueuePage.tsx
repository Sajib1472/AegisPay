import { useEffect, useState } from "react";
import { api, type PayPeriod, type PayrollView } from "../api";

export default function QueuePage() {
  const [periods, setPeriods] = useState<PayPeriod[]>([]);
  const [view, setView] = useState<PayrollView | null>(null);
  const [reason, setReason] = useState("Reviewed source punches");
  const [error, setError] = useState("");

  useEffect(() => {
    api.periods().then(setPeriods).catch((err) => setError(err.message));
  }, []);

  async function load(periodId: string) {
    setError("");
    try {
      const latest = await api.latestRun(periodId);
      setView(latest ?? null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Load failed");
    }
  }

  async function run(periodId: string) {
    setError("");
    try {
      setView(await api.calculate(periodId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Calculate failed");
    }
  }

  async function dismiss(exceptionId: string) {
    if (!view) return;
    setError("");
    try {
      setView(await api.dismissException(view.runId, exceptionId, reason));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Dismiss failed");
    }
  }

  return (
    <div>
      <h2>Exception queue</h2>
      <p>Blockers must be fixed before approval. Warnings (meal premiums, high hours) can be dismissed with a reason.</p>
      {error ? <p className="error">{error}</p> : null}
      <div className="card">
        {periods.map((period) => (
          <p key={period.id}>
            {period.startDate} → {period.endDate}{" "}
            <span className="badge">{period.status}</span>{" "}
            <button className="ghost" onClick={() => load(period.id)}>
              Open queue
            </button>{" "}
            <button className="primary" onClick={() => run(period.id)}>
              Calculate this period
            </button>
          </p>
        ))}
        {periods.length === 0 ? <p>No pay periods yet. Create one under Payroll runs.</p> : null}
      </div>
      {view ? (
        <div className="card">
          <h3>
            Run {view.runId.slice(0, 8)} · {view.status} · engine {view.engineVersion}
          </h3>
          <label>
            Dismiss reason
            <input value={reason} onChange={(e) => setReason(e.target.value)} />
          </label>
          <table>
            <thead>
              <tr>
                <th>When</th>
                <th>Type</th>
                <th>Severity</th>
                <th>Message</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {view.exceptions.map((ex) => (
                <tr key={ex.id || `${ex.type}-${ex.workDate}`}>
                  <td>{ex.workDate}</td>
                  <td>{ex.type}</td>
                  <td>
                    <span className={`badge ${ex.blocker ? "danger" : "warn"}`}>
                      {ex.dismissed ? "dismissed" : ex.blocker ? "blocker" : ex.severity.toLowerCase()}
                    </span>
                  </td>
                  <td>{ex.message}</td>
                  <td>
                    {ex.blocker ? (
                      <span>Fix punch / enter rate</span>
                    ) : ex.dismissed ? (
                      ex.dismissReason
                    ) : (
                      <button className="ghost" onClick={() => dismiss(ex.id)}>
                        Dismiss with reason
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {view.exceptions.length === 0 ? <p>No exceptions. Ready to approve on the Payroll screen.</p> : null}
        </div>
      ) : null}
    </div>
  );
}
