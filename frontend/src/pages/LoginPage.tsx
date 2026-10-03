import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, setSession } from "../api";

export default function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("owner@harbordental.example");
  const [password, setPassword] = useState("HarborDental!demo");
  const [error, setError] = useState("");
  const [mode, setMode] = useState<"login" | "signup">("login");
  const [tos, setTos] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError("");
    try {
      const auth =
        mode === "login"
          ? await api.login(email, password)
          : await api.signup({
              legalName: "New Clinic Group",
              displayName: "Owner",
              email,
              password,
              vertical: "DENTAL",
              tosAccepted: tos
            });
      setSession(auth);
      navigate("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to sign in");
    }
  }

  return (
    <div className="auth card">
      <h1>AegisPay</h1>
      <p>Check every punch against wage-and-hour rules before Gusto pays it.</p>
      <form onSubmit={onSubmit}>
        <label>
          Email
          <input value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
        </label>
        {mode === "signup" ? (
          <label>
            <input type="checkbox" checked={tos} onChange={(e) => setTos(e.target.checked)} /> I agree to the Terms,
            privacy policy, and that this is calculation assistance — not legal advice. I remain the employer of record.
          </label>
        ) : null}
        {error ? <p className="error">{error}</p> : null}
        <button className="primary" type="submit" disabled={mode === "signup" && !tos}>
          {mode === "login" ? "Sign in" : "Create clinic group"}
        </button>
      </form>
      <p>
        <button className="ghost" type="button" onClick={() => setMode(mode === "login" ? "signup" : "login")}>
          {mode === "login" ? "Start a new group" : "Back to sign in"}
        </button>
      </p>
      <p className="disclaimer">
        Demo seed (local profile): owner@harbordental.example / HarborDental!demo on the Group plan. New signups start
        on a 14-day Pilot. Calculation assistance only — not legal advice. You remain the employer of record.
      </p>
    </div>
  );
}
