import { useEffect, useState } from "react";
import { api, getSession, type PayPeriod, type PayrollView } from "../api";

export default function PayrollPage() {
  const canApprove = (getSession()?.permissions || []).includes("PAYROLL_APPROVE");
  const [periods, setPeriods] = useState<PayPeriod[]>([]);
  const [startDate, setStartDate] = useState("2024-06-03");
  const [endDate, setEndDate] = useState("2024-06-09");
  const [view, setView] = useState<PayrollView | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.periods().then(setPeriods).catch((err) => setError(err.message));
  }, []);

  async function createPeriod() {
    setError("");
    try {
      await api.createPeriod(startDate, endDate);
      setPeriods(await api.periods());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not create period");
    }
  }

  async function calculate(id: string) {
    setError("");
    try {
      setView(await api.calculate(id));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Calculate failed");
    }
  }

  async function approve() {
    if (!view) return;
    setError("");
    try {
      setView(await api.approve(view.runId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Approve failed");
    }
  }

  return (
    <div>
      <h2>Payroll runs</h2>
      {error ? <p className="error">{error}</p> : null}
      <div className="card">
        <h3>New period</h3>
        <div className="row">
          <label>
            Start
            <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          </label>
          <label>
            End
            <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
          </label>
        </div>
        <button className="primary" onClick={createPeriod}>
          Open period
        </button>
      </div>
      <div className="card">
        {periods.map((period) => (
          <p key={period.id}>
            {period.startDate} → {period.endDate} <span className="badge">{period.status}</span>{" "}
            <button className="ghost" onClick={() => calculate(period.id)}>
              Run engine
            </button>
          </p>
        ))}
      </div>
      {view ? (
        <div className="card">
          <h3>
            Register · {view.status}
            {view.status !== "APPROVED" && canApprove ? (
              <button className="primary" style={{ marginLeft: 12 }} onClick={approve}>
                Approve and lock
              </button>
            ) : null}
          </h3>
          <p>
            Regular rates (shown, including bonus true-up):{" "}
            {Object.values(view.regularRates || {}).join(", ") || "—"}
          </p>
          {view.success ? (
            <p>
              Success checklist:{" "}
              {[
                ["Punches handled", view.success.punchesAccounted],
                ["Every employee has a line", view.success.everyEmployeeHasALine],
                ["Regular rate shown", view.success.regularRateShown],
                ["PAYROLL_APPROVE signed off", view.success.approvedByPayrollApprove],
                ["Snapshot stored", view.success.snapshotStored],
                ["Gusto file ready", view.success.gustoExportReady]
              ].map(([label, ok]) => (
                <span key={String(label)} className={`badge ${ok ? "ok" : "warn"}`} style={{ marginRight: 6 }}>
                  {label}
                </span>
              ))}
            </p>
          ) : null}
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Bucket</th>
                <th>Hours</th>
                <th>Rate</th>
                <th>Amount</th>
                <th>Why</th>
              </tr>
            </thead>
            <tbody>
              {view.lines.map((line, i) => (
                <tr key={i}>
                  <td>{line.workDate}</td>
                  <td>{line.bucket}</td>
                  <td>{line.hours}</td>
                  <td>{line.rate}</td>
                  <td>{line.amount}</td>
                  <td>{line.explanation?.narrative || line.explanation?.code}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <h4>Gusto export</h4>
          <pre className="csv">{view.gustoCsv}</pre>
        </div>
      ) : null}
    </div>
  );
}
