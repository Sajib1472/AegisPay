import { useEffect, useState } from "react";
import { api, type LaborReport } from "../api";

export default function ReportsPage() {
  const [report, setReport] = useState<LaborReport | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.laborReport().then(setReport).catch((err) => setError(err.message));
  }, []);

  return (
    <div>
      <h2>Labor cost</h2>
      <p>Gross earnings submitted to the payroll provider, grouped by location and job. Click a payroll run to see punches.</p>
      {error ? <p className="error">{error}</p> : null}
      {!report || !report.runId ? (
        <div className="card">
          <p>No calculated run yet. Import a clock CSV on Time, then run the engine on Payroll.</p>
        </div>
      ) : (
        <>
          <div className="row">
            <div className="card stat">
              <span>Gross to provider</span>
              <b>{report.gross}</b>
            </div>
            <div className="card stat">
              <span>Overtime $</span>
              <b>{report.otPay}</b>
            </div>
            <div className="card stat">
              <span>Premiums $</span>
              <b>{report.premiums}</b>
            </div>
          </div>
          <div className="card">
            <h3>By location</h3>
            <table>
              <thead>
                <tr>
                  <th>Location</th>
                  <th>Amount</th>
                </tr>
              </thead>
              <tbody>
                {report.byLocation.map((row) => (
                  <tr key={row.name}>
                    <td>{row.name}</td>
                    <td>{row.amount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="card">
            <h3>By job code</h3>
            <table>
              <thead>
                <tr>
                  <th>Job</th>
                  <th>Amount</th>
                </tr>
              </thead>
              <tbody>
                {report.byJob.map((row) => (
                  <tr key={row.name}>
                    <td>{row.name}</td>
                    <td>{row.amount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
