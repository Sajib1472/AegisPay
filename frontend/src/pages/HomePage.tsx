import { useEffect, useState } from "react";
import { api, getSession, type Location, type PayPeriod, type Person } from "../api";

export default function HomePage() {
  const session = getSession();
  const [people, setPeople] = useState<Person[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);
  const [periods, setPeriods] = useState<PayPeriod[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([api.people(), api.locations(), api.periods()])
      .then(([p, l, r]) => {
        setPeople(p);
        setLocations(l);
        setPeriods(r);
      })
      .catch((err) => setError(err.message));
  }, []);

  const openPeriod = periods.find((p) => p.status === "OPEN");

  return (
    <div>
      <h2>This week’s risk</h2>
      <p>
        {session?.tenantName} · {session?.plan} plan · {session?.vertical.toLowerCase()} group
      </p>
      {error ? <p className="error">{error}</p> : null}
      <div className="row">
        <div className="card stat">
          <span>Staff on file</span>
          <b>{people.length}</b>
        </div>
        <div className="card stat">
          <span>Locations</span>
          <b>{locations.length}</b>
        </div>
        <div className="card stat">
          <span>Open pay period</span>
          <b>{openPeriod ? `${openPeriod.startDate}` : "None"}</b>
        </div>
      </div>
      <div className="card">
        <h3>How payday works here</h3>
        <ol>
          <li>Import punches from the time clock CSV.</li>
          <li>Run the engine. Review meal premiums, daily OT, and dual-rate true-ups.</li>
          <li>Approve. Download the Gusto file. Keep the explanation pack if anyone asks.</li>
        </ol>
        <p className="disclaimer">
          AegisPay does not remit taxes or move money. It makes the hours you send to payroll legally explainable.
        </p>
      </div>
    </div>
  );
}
