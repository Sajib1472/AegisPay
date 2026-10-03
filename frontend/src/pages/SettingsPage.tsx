import { getSession } from "../api";

export default function SettingsPage() {
  const session = getSession();
  return (
    <div>
      <h2>Settings</h2>
      <div className="card">
        <p>
          <strong>Group:</strong> {session?.tenantName}
        </p>
        <p>
          <strong>Plan:</strong> {session?.plan}
        </p>
        <p>
          <strong>Vertical:</strong> {session?.vertical}
        </p>
        <p>
          Default destination: Gusto CSV. Stripe billing, MFA for approvers, and extra state packs are next.
        </p>
        <p className="disclaimer">
          Rule packs are versioned. Recalculating a locked period must use the packs that were effective then. Software
          provides calculation assistance, not legal advice. Have employment counsel review California and FLSA packs
          before you take a paid clinic live.
        </p>
      </div>
    </div>
  );
}
