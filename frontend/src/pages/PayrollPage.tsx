import { useEffect, useState } from "react";
import { api, getSession, type PayPeriod, type PayrollView, type Person } from "../api";

export default function PayrollPage() {
  const session = getSession();
  const canApprove = (session?.permissions || []).includes("PAYROLL_APPROVE");
  const role = session?.role;
  const [periods, setPeriods] = useState<PayPeriod[]>([]);
  const [people, setPeople] = useState<Person[]>([]);
  const [startDate, setStartDate] = useState("2024-06-03");
  const [endDate, setEndDate] = useState("2024-06-09");
  const [view, setView] = useState<PayrollView | null>(null);
  const [confirmed, setConfirmed] = useState(false);
  const [bonusPerson, setBonusPerson] = useState("");
  const [bonusAmount, setBonusAmount] = useState("");
  const [bonusDate, setBonusDate] = useState("2024-06-07");
  const [bonusNote, setBonusNote] = useState("");
  const [discretionary, setDiscretionary] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([api.periods(), api.people()])
      .then(([p, staff]) => {
        setPeriods(p);
        setPeople(staff);
        if (staff[0]) setBonusPerson(staff[0].id);
      })
      .catch((err) => setError(err.message));
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
    setConfirmed(false);
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
      setView(await api.approve(view.runId, confirmed));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Approve failed");
    }
  }

  async function exportRun() {
    if (!view) return;
    setError("");
    try {
      setView(await api.exportRun(view.runId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Export failed");
    }
  }

  async function unlock() {
    if (!view) return;
    setError("");
    try {
      if (role === "PAYROLL_ADMIN") {
        await api.requestUnlock(view.runId);
        setView(await api.latestRun(view.periodId));
      } else {
        setView(await api.confirmUnlock(view.runId));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unlock failed");
    }
  }

  async function addBonus() {
    setError("");
    try {
      await api.createBonus({
        personId: bonusPerson,
        amount: bonusAmount,
        earnedOn: bonusDate,
        discretionary,
        note: bonusNote,
        payPeriodId: view?.periodId
      });
      setBonusAmount("");
      setBonusNote("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Bonus failed");
    }
  }

  const terminal = view && ["APPROVED", "EXPORTED", "LOCKED"].includes(view.status);

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
        <h3>Bonus entry</h3>
        <p>Person, amount, date, discretionary. The engine allocates it on the next calculate.</p>
        <div className="row">
          <label>
            Person
            <select value={bonusPerson} onChange={(e) => setBonusPerson(e.target.value)}>
              {people.map((person) => (
                <option key={person.id} value={person.id}>
                  {person.legalName}
                </option>
              ))}
            </select>
          </label>
          <label>
            Amount
            <input value={bonusAmount} onChange={(e) => setBonusAmount(e.target.value)} placeholder="250.00" />
          </label>
          <label>
            Earned on
            <input type="date" value={bonusDate} onChange={(e) => setBonusDate(e.target.value)} />
          </label>
        </div>
        <label>
          <input type="checkbox" checked={discretionary} onChange={(e) => setDiscretionary(e.target.checked)} /> Discretionary
        </label>
        <label>
          Note
          <input value={bonusNote} onChange={(e) => setBonusNote(e.target.value)} />
        </label>
        <button className="ghost" onClick={addBonus}>
          Save bonus
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
            {view.approvalIp ? ` · approved from ${view.approvalIp}` : ""}
          </h3>
          <p>
            Gross earnings submitted to payroll provider — AegisPay does not withhold tax.
          </p>
          <table>
            <thead>
              <tr>
                <th>Employee</th>
                <th>REG h / $</th>
                <th>OT h / $</th>
                <th>DT h / $</th>
                <th>Premiums</th>
                <th>Diff</th>
                <th>Bonus</th>
                <th>Gross (to provider)</th>
              </tr>
            </thead>
            <tbody>
              {(view.register || []).map((row) => (
                <tr key={row.personId}>
                  <td>
                    {row.legalName}
                    <br />
                    <small>{row.employeeCode}</small>
                  </td>
                  <td>
                    {row.regularHours} / {row.regularPay}
                  </td>
                  <td>
                    {row.otHours} / {row.otPay}
                  </td>
                  <td>
                    {row.dtHours} / {row.dtPay}
                  </td>
                  <td>{row.premiums}</td>
                  <td>{row.differentials}</td>
                  <td>{row.bonus}</td>
                  <td>{row.grossEarningsSubmitted}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {view.status !== "APPROVED" && view.status !== "EXPORTED" && view.status !== "LOCKED" && canApprove ? (
            <div>
              <label>
                <input type="checkbox" checked={confirmed} onChange={(e) => setConfirmed(e.target.checked)} /> I confirm I
                have reviewed exceptions and this run may be sent to our payroll provider.
              </label>
              <button className="primary" style={{ marginLeft: 12 }} onClick={approve} disabled={!confirmed}>
                Approve
              </button>
            </div>
          ) : null}
          {view.status === "APPROVED" ? (
            <button className="primary" onClick={exportRun}>
              Mark exported (Gusto CSV below)
            </button>
          ) : null}
          {terminal && (role === "PAYROLL_ADMIN" || role === "OWNER") ? (
            <p>
              Dual-control unlock: payroll admin requests, owner confirms.{" "}
              <button className="ghost" onClick={unlock}>
                {role === "PAYROLL_ADMIN" ? "Request unlock" : "Confirm unlock"}
              </button>
              {view.unlockPending ? <span className="badge warn">unlock pending</span> : null}
            </p>
          ) : null}
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
          <h4>Day lines</h4>
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
          {view.genericCsv ? (
            <>
              <h4>Generic CSV</h4>
              <pre className="csv">{view.genericCsv}</pre>
            </>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
