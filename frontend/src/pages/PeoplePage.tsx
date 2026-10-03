import { useEffect, useState } from "react";
import { api, type Person } from "../api";

export default function PeoplePage() {
  const [people, setPeople] = useState<Person[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api.people().then(setPeople).catch((err) => setError(err.message));
  }, []);

  return (
    <div>
      <h2>People & rates</h2>
      {error ? <p className="error">{error}</p> : null}
      <div className="card">
        <table>
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Email</th>
              <th>Classification</th>
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
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
