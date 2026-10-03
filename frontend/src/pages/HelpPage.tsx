export default function HelpPage() {
  return (
    <div>
      <h2>Why was this overtime?</h2>
      <div className="card">
        <h3>Federal (FLSA)</h3>
        <p>
          Hours over 40 in the workweek are overtime at 1.5× the regular rate. The regular rate includes non-discretionary
          bonuses. Open a register line — the explanation drawer cites the formula.
        </p>
      </div>
      <div className="card">
        <h3>California daily overtime</h3>
        <p>
          After 8 hours in a workday: time-and-a-half. After 12: double-time. Seventh consecutive day: first 8 at 1.5×,
          remainder at 2×. Meal and rest premiums are extra hours at the regular rate, not hours worked.
        </p>
      </div>
      <div className="card">
        <h3>How to read a line</h3>
        <ol>
          <li>Payroll register → click the dollar → day lines show code, citation, formula.</li>
          <li>Exception queue is red only for legal blockers (unpaired punch, missing rate).</li>
          <li>Audit pack PDF is the court artifact. Do not screenshot the dark nav; print from the paper-colored pages.</li>
        </ol>
      </div>
      <p className="disclaimer">Calculation assistance, not legal advice. You remain the employer of record.</p>
    </div>
  );
}
