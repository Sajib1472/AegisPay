import { useEffect, useState } from "react";
import { api, type AccountView } from "../api";

export default function SettingsPage() {
  const [account, setAccount] = useState<AccountView | null>(null);
  const [catalog, setCatalog] = useState<string>("");
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([api.account(), api.catalog()])
      .then(([a, c]) => {
        setAccount(a);
        setCatalog(c.noFreeForeverPlan);
      })
      .catch((err) => setError(err.message));
  }, []);

  return (
    <div>
      <h2>Settings</h2>
      {error ? <p className="error">{error}</p> : null}
      {account ? (
        <>
          <div className="card">
            <p>
              <strong>Group:</strong> {account.tenantName}
            </p>
            <p>
              <strong>Plan:</strong> {account.catalog.name} — {account.catalog.who}
              {account.catalog.monthlyCents > 0 ? ` · $${(account.catalog.monthlyCents / 100).toFixed(0)}/mo` : " · $0 until day 14"}
            </p>
            <p>
              <strong>Status:</strong> {account.status}
              {account.trialEndsAt ? ` · trial ends ${account.trialEndsAt.slice(0, 10)}` : ""}
            </p>
            <p>
              Usage: {account.locationCount}/{account.maxLocations} locations · {account.employeeCount}/
              {account.maxEmployees} staff
              {account.maxPayPeriods != null ? ` · ${account.periodCount}/${account.maxPayPeriods} pilot periods` : ""}
            </p>
            <p>
              Rule packs: {account.rulePacks.join(", ")}
              {account.secondStatePack ? " · second state pack on" : " · second state pack is Network"}
            </p>
            <p>Audit pack add-on ($99/run): {account.auditPack ? "on" : "off"}</p>
            <p>Support: {account.catalog.support}</p>
            <p>
              <button className="primary" onClick={() => api.checkout("GROUP", false).then((r) => (window.location.href = r.url))}>
                Pay Group monthly ($299)
              </button>{" "}
              <button className="ghost" onClick={() => api.checkout("GROUP", true).then((r) => (window.location.href = r.url))}>
                Group annual (2 months free)
              </button>
            </p>
            <p>
              <button className="ghost" onClick={() => api.billingPortal().then((r) => (window.location.href = r.url))}>
                Update card or cancel (Stripe portal)
              </button>
            </p>
          </div>
          <div className="card">
            <h3>Who this product is for</h3>
            <ul>
              {account.positioning.map((line) => (
                <li key={line}>{line}</li>
              ))}
            </ul>
            <p className="disclaimer">{catalog}</p>
          </div>
        </>
      ) : null}
      <p className="disclaimer">
        Rule packs are versioned. Software provides calculation assistance, not legal advice. You remain the employer of
        record.
      </p>
    </div>
  );
}
