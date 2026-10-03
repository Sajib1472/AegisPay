import { useEffect, useState } from "react";
import { api, type Person } from "../api";

export default function PeoplePage() {
  const [people, setPeople] = useState<Person[]>([]);
  const [balances, setBalances] = useState<{ personId: string; policyCode: string; hours: number; asOf: string }[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([api.people(), api.leaveBalances()])
      .then(([p, b]) => {
        setPeople(p);
        setBalances(b);
      })
      .catch((err) => setError(err.message));
  }, []);

  function sick(personId: string) {
    return balances.find((row) => row.personId === personId);
  }

  return (
    <div>
      <h2>People & rates</h2>
      <p>Sick leave is a ledger. The hours column is a cache of ledger sums, not the only source of truth.</p>
      {error ? <p className="error">{error}</p> : null}
      <div className="card">
        {people.length === 0 ? <p>Import people in the wizard, then paste a clock CSV on Time.</p> : null}
        <table>
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Email</th>
              <th>Classification</th>
              <th>CA sick hours</th>
            </tr>
          </thead>
          <tbody>
            {people.map((person) => (
              <tr key={person.id}>
                <td>{person.externalEmployeeCode}</td>
                <td>{person.legalName}</td>
                <td>{person.email}</td>
                <td>
                  <span className={`badge ${person.exemptionStatus === "NON_EXEMPT" ? "ok" : "warn"}`}>
                    {person.exemptionStatus}
                  </span>
                </td>
                <td>{sick(person.id) ? `${sick(person.id)?.hours} as of ${sick(person.id)?.asOf}` : "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
