import { useEffect, useState } from "react";
import { api, getSession, type AccountView, type Location, type PayPeriod, type Person, type RiskView } from "../api";

export default function HomePage() {
  const session = getSession();
  const owner = session?.role === "OWNER";
  const [people, setPeople] = useState<Person[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);
  const [periods, setPeriods] = useState<PayPeriod[]>([]);
  const [account, setAccount] = useState<AccountView | null>(null);
  const [risk, setRisk] = useState<RiskView | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([api.people(), api.locations(), api.periods(), api.account(), api.risk().catch(() => null)])
      .then(([p, l, r, a, k]) => {
        setPeople(p);
        setLocations(l);
        setPeriods(r);
        setAccount(a);
        setRisk(k);
      })
      .catch((err) => setError(err.message));
  }, []);

  const openPeriod = periods.find((p) =>
    ["DRAFT", "CALCULATED", "EXCEPTIONS_PENDING", "OPEN"].includes(p.status)
  );
  const otPct = risk && Number(risk.gross) > 0
    ? ((Number(risk.overtimePay) / Number(risk.gross)) * 100).toFixed(1)
    : "0.0";

  return (
    <div>
      <h2>{owner ? "This week’s risk" : "Payroll desk"}</h2>
      <p>
        {session?.tenantName} · {session?.plan} plan · {session?.vertical.toLowerCase()} group
        {account && !account.writable ? " · read-only (convert from Pilot)" : ""}
      </p>
      {error ? <p className="error">{error}</p> : null}
      <div className="row">
        <div className="card stat">
          <span>Staff on file</span>
          <b>
            {people.length}
            {account ? <small> / {account.maxEmployees}</small> : null}
          </b>
        </div>
        <div className="card stat">
          <span>Locations</span>
          <b>
            {locations.length}
            {account ? <small> / {account.maxLocations}</small> : null}
          </b>
        </div>
        <div className="card stat">
          <span>Open pay period</span>
          <b>{openPeriod ? `${openPeriod.startDate}` : "None"}</b>
        </div>
        {owner && risk ? (
          <>
            <div className="card stat">
              <span>Premiums generated</span>
              <b>{risk.premiumsGenerated}</b>
            </div>
            <div className="card stat">
              <span>OT as % of dollars</span>
              <b>{otPct}%</b>
            </div>
            <div className="card stat">
              <span>Unapproved countdown</span>
              <b>{risk.daysUntilPeriodEnd < 0 ? "—" : `${risk.daysUntilPeriodEnd}d`}</b>
            </div>
          </>
        ) : null}
      </div>
      {owner ? (
        <div className="card">
          <h3>Owner risk dashboard</h3>
          <p>
            You sign the card and the lawsuit. Your office manager lives in the exception queue
            {risk ? ` (${risk.blockerExceptions} blockers, ${risk.unapprovedPeriods} open periods)` : ""}. Staff keep
            punching the same clock — they do not need this app in v1.
          </p>
          <p className="disclaimer">{account?.positioning[0]}</p>
        </div>
      ) : (
        <div className="card">
          <h3>How payday works here</h3>
          <ol>
            <li>Import punches from the time clock CSV.</li>
            <li>Run the engine. Review meal premiums, daily OT, and dual-rate true-ups.</li>
            <li>Someone with PAYROLL_APPROVE signs off. Download the Gusto file.</li>
          </ol>
        </div>
      )}
      {people.length === 0 ? (
        <div className="card">
          <p>Next file to upload: a people CSV in the wizard, then a clock CSV on Time. There is no empty-state illustration — just the next file.</p>
        </div>
      ) : null}
      <p className="disclaimer">
        AegisPay does not remit taxes or move money. It makes the hours you send to payroll legally explainable.
      </p>
    </div>
  );
}
