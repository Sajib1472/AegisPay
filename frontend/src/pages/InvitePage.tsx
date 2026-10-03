import { FormEvent, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api, setSession } from "../api";

export default function InvitePage() {
  const [params] = useSearchParams();
  const token = useMemo(() => params.get("token") || "", [params]);
  const navigate = useNavigate();
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError("");
    try {
      const auth = await api.acceptInvite(token, displayName, password);
      setSession(auth);
      navigate("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Invite failed");
    }
  }

  return (
    <div className="auth card">
      <h1>Join this clinic group</h1>
      <p>Set a password. The invite expires in 48 hours.</p>
      <form onSubmit={onSubmit}>
        <label>
          Your name
          <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        {error ? <p className="error">{error}</p> : null}
        <button className="primary" type="submit" disabled={!token}>
          Accept invite
        </button>
      </form>
      {!token ? <p className="error">Missing invite token. Open the link from the email.</p> : null}
    </div>
  );
}
