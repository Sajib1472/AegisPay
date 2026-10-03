import { useEffect, useState } from "react";
import { api } from "../api";

const STEPS = [
  "Group profile",
  "Locations",
  "Pay period",
  "Job codes",
  "People import",
  "Time clock mapping",
  "Payroll destination",
  "Policies"
];

export default function WizardPage() {
  const [wizard, setWizard] = useState<{ step: string; vertical: string; legalName: string } | null>(null);
  const [preview, setPreview] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    api.wizard()
      .then(setWizard)
      .catch((err) => setError(err.message));
  }, []);

  async function previewCity() {
    try {
      const result = await api.jurisdictions("Santa Monica", "CA");
      setPreview(JSON.stringify(result, null, 2));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Preview failed");
    }
  }

  return (
    <div>
      <h2>Setup wizard</h2>
      <p>No consultant required. EIN and SSNs are not collected in v1.</p>
      <ol>
        {STEPS.map((step) => (
          <li key={step}>{step}</li>
        ))}
      </ol>
      {wizard ? (
        <p>
          Current step: {wizard.step} · {wizard.legalName} · {wizard.vertical}
        </p>
      ) : null}
      <button className="primary" onClick={previewCity}>
        Preview Santa Monica jurisdictions
      </button>
      {preview ? <pre className="csv">{preview}</pre> : null}
      {error ? <p className="error">{error}</p> : null}
    </div>
  );
}
