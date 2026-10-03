import { Navigate, NavLink, Route, Routes, useNavigate } from "react-router-dom";
import { clearSession, getSession } from "./api";
import LoginPage from "./pages/LoginPage";
import HomePage from "./pages/HomePage";
import QueuePage from "./pages/QueuePage";
import TimePage from "./pages/TimePage";
import PeoplePage from "./pages/PeoplePage";
import PayrollPage from "./pages/PayrollPage";
import SettingsPage from "./pages/SettingsPage";

function Shell() {
  const session = getSession();
  const navigate = useNavigate();
  if (!session) {
    return <Navigate to="/login" replace />;
  }
  return (
    <div className="shell">
      <aside className="nav">
        <h1>AegisPay</h1>
        <p>
          {session.tenantName}
          <br />
          {session.displayName} · {session.role}
        </p>
        <NavLink to="/" end>
          Risk home
        </NavLink>
        <NavLink to="/queue">Exception queue</NavLink>
        <NavLink to="/time">Time & imports</NavLink>
        <NavLink to="/people">People & rates</NavLink>
        <NavLink to="/payroll">Payroll runs</NavLink>
        <NavLink to="/settings">Settings</NavLink>
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
          <Route path="/settings" element={<SettingsPage />} />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/*" element={<Shell />} />
    </Routes>
  );
}
