import { Navigate, NavLink, Route, Routes, useNavigate } from "react-router-dom";
import { useCallback, useEffect, useState } from "react";
import { api, clearSession, getSession, type Location } from "./api";
import LoginPage from "./pages/LoginPage";
import InvitePage from "./pages/InvitePage";
import HomePage from "./pages/HomePage";
import QueuePage from "./pages/QueuePage";
import TimePage from "./pages/TimePage";
import PeoplePage from "./pages/PeoplePage";
import PayrollPage from "./pages/PayrollPage";
import SettingsPage from "./pages/SettingsPage";
import WizardPage from "./pages/WizardPage";
import ReportsPage from "./pages/ReportsPage";
import HelpPage from "./pages/HelpPage";
import SessionWatchdog from "./pages/SessionWatchdog";

function Shell() {
  const session = getSession();
  const navigate = useNavigate();
  const [locations, setLocations] = useState<Location[]>([]);
  const [locationId, setLocationId] = useState(localStorage.getItem("aegispay.location") || "");

  const expire = useCallback(() => {
    clearSession();
    navigate("/login");
  }, [navigate]);

  useEffect(() => {
    api.locations()
      .then((rows) => {
        setLocations(rows);
        if (!locationId && rows[0]) {
          setLocationId(rows[0].id);
          localStorage.setItem("aegispay.location", rows[0].id);
        }
      })
      .catch(() => undefined);
  }, [locationId]);

  if (!session) {
    return <Navigate to="/login" replace />;
  }
  const current = locations.find((row) => row.id === locationId) || locations[0];
  return (
    <div className="shell">
      <SessionWatchdog onExpire={expire} />
      <aside className="nav">
        <h1>AegisPay</h1>
        <p>
          {session.tenantName}
          <br />
          {session.displayName} · {session.role}
          {session.permissions?.includes("PAYROLL_APPROVE") ? " · can approve" : ""}
        </p>
        {current ? (
          <p className="location-context">You are viewing {session.tenantName} — {current.name}</p>
        ) : null}
        {locations.length > 1 ? (
          <label>
            Location
            <select
              value={current?.id || ""}
              onChange={(e) => {
                setLocationId(e.target.value);
                localStorage.setItem("aegispay.location", e.target.value);
              }}
            >
              {locations.map((row) => (
                <option key={row.id} value={row.id}>
                  {row.name}
                </option>
              ))}
            </select>
          </label>
        ) : null}
        <NavLink to="/" end>
          {session.role === "OWNER" ? "Risk home" : "Home"}
        </NavLink>
        <NavLink to="/queue">Exception queue</NavLink>
        <NavLink to="/time">Time & imports</NavLink>
        <NavLink to="/people">People & rates</NavLink>
        <NavLink to="/payroll">Payroll runs</NavLink>
        <NavLink to="/reports">Reports</NavLink>
        <NavLink to="/wizard">Setup wizard</NavLink>
        <NavLink to="/settings">Settings</NavLink>
        <NavLink to="/help">Help</NavLink>
        <div className="spacer" />
        <button
          className="ghost"
          style={{ color: "inherit", borderColor: "#3a5168" }}
          onClick={() => {
            clearSession();
            navigate("/login");
          }}
        >
          Sign out
        </button>
      </aside>
      <main className="main">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/queue" element={<QueuePage />} />
          <Route path="/time" element={<TimePage />} />
          <Route path="/people" element={<PeoplePage />} />
          <Route path="/payroll" element={<PayrollPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/wizard" element={<WizardPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/help" element={<HelpPage />} />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/invite" element={<InvitePage />} />
      <Route path="/*" element={<Shell />} />
    </Routes>
  );
}
