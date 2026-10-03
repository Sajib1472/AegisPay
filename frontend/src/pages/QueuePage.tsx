import { useEffect, useState } from "react";
import { api, type PayPeriod, type PayrollView } from "../api";

export default function QueuePage() {
  const [periods, setPeriods] = useState<PayPeriod[]>([]);
  const [view, setView] = useState<PayrollView | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.periods().then(setPeriods).catch((err) => setError(err.message));
  }, []);

  async function run(periodId: string) {
    setError("");
    try {
      setView(await api.calculate(periodId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Calculate failed");
    }
  }

  return (
    <div>
      <h2>Exception queue</h2>
      <p>Blockers must be fixed before approval. Warnings (meal premiums, high hours) can be acknowledged.</p>
      {error ? <p className="error">{error}</p> : null}
      <div className="card">
        {periods.map((period) => (
          <p key={period.id}>
            {period.startDate} → {period.endDate}{" "}
            <span className="badge">{period.status}</span>{" "}
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
          <table>
            <thead>
              <tr>
                <th>When</th>
                <th>Type</th>
                <th>Severity</th>
                <th>Message</th>
              </tr>
            </thead>
            <tbody>
              {view.exceptions.map((ex, i) => (
                <tr key={i}>
                  <td>{ex.workDate}</td>
                  <td>{ex.type}</td>
                  <td>
                    <span className={`badge ${ex.blocker ? "danger" : "warn"}`}>
                      {ex.blocker ? "blocker" : ex.severity.toLowerCase()}
                    </span>
                  </td>
                  <td>{ex.message}</td>
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
