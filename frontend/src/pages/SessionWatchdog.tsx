import { useEffect, useState } from "react";

const LIMIT_MS = 30 * 60 * 1000;
const WARN_MS = 2 * 60 * 1000;

export default function SessionWatchdog({ onExpire }: { onExpire: () => void }) {
  const [remaining, setRemaining] = useState(LIMIT_MS);

  useEffect(() => {
    const mark = () => localStorage.setItem("aegispay.activity", String(Date.now()));
    mark();
    const events = ["click", "keydown", "mousemove"];
    events.forEach((name) => window.addEventListener(name, mark));
    const timer = window.setInterval(() => {
      const last = Number(localStorage.getItem("aegispay.activity") || Date.now());
      const left = LIMIT_MS - (Date.now() - last);
      setRemaining(left);
      if (left <= 0) {
        onExpire();
      }
    }, 1000);
    return () => {
      events.forEach((name) => window.removeEventListener(name, mark));
      window.clearInterval(timer);
    };
  }, [onExpire]);

  if (remaining > WARN_MS) {
    return null;
  }
  const seconds = Math.max(0, Math.ceil(remaining / 1000));
  return (
    <div className="session-warn" role="status">
      Payroll session expires in {seconds}s. Move the mouse or press a key to stay signed in.
    </div>
  );
}
