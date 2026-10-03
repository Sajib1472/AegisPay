import { useEffect, useState } from "react";
import { api, type Location, type Punch } from "../api";

export default function TimePage() {
  const [locations, setLocations] = useState<Location[]>([]);
  const [punches, setPunches] = useState<Punch[]>([]);
  const [locationId, setLocationId] = useState("");
  const [csv, setCsv] = useState(
    "employee_code,timestamp,type,location\n1002,2024-06-04 08:00,IN,downtown\n1002,2024-06-04 16:00,OUT,downtown\n"
  );
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([api.locations(), api.punches()])
      .then(([locs, rows]) => {
        setLocations(locs);
        setPunches(rows);
        if (locs[0]) setLocationId(locs[0].id);
      })
      .catch((err) => setError(err.message));
  }, []);

  async function importCsv() {
    setError("");
    try {
      const result = await api.importPunches(locationId, csv, "office-export.csv");
      setMessage(JSON.stringify(result));
      setPunches(await api.punches());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Import failed");
    }
  }

  return (
    <div>
      <h2>Time & imports</h2>
      <div className="card">
        <h3>CSV import</h3>
        <p>Columns: employee_code, timestamp (yyyy-MM-dd HH:mm), type (IN, OUT, BREAK_START, BREAK_END), location.</p>
        <label>
          Location
          <select value={locationId} onChange={(e) => setLocationId(e.target.value)}>
            {locations.map((location) => (
              <option key={location.id} value={location.id}>
                {location.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          File contents
          <textarea rows={6} value={csv} onChange={(e) => setCsv(e.target.value)} />
        </label>
        <button className="primary" onClick={importCsv} disabled={!locationId}>
          Commit import
        </button>
        {message ? <pre className="csv">{message}</pre> : null}
        {error ? <p className="error">{error}</p> : null}
      </div>
      <div className="card">
        <h3>Recent punches</h3>
        <table>
          <thead>
            <tr>
              <th>When</th>
              <th>Type</th>
              <th>Source</th>
            </tr>
          </thead>
          <tbody>
            {punches.slice(0, 40).map((punch) => (
              <tr key={punch.id}>
                <td>{punch.adjustedAt}</td>
                <td>{punch.punchType}</td>
                <td>{punch.source}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
