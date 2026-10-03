const TOKEN_KEY = "aegispay.token";

export type AuthResponse = {
  accessToken: string;
  userId: string;
  tenantId: string;
  tenantName: string;
  displayName: string;
  email: string;
  role: string;
  plan: string;
  vertical: string;
  trialEndsAt?: string | null;
  permissions: string[];
};

export type Location = {
  id: string;
  name: string;
  city: string;
  region: string;
  timeZone: string;
  jurisdictions: string[];
};

export type Person = {
  id: string;
  externalEmployeeCode: string;
  legalName: string;
  email: string;
  exemptionStatus: string;
};

export type Punch = {
  id: string;
  personId: string;
  punchType: string;
  adjustedAt: string;
  source: string;
};

export type PayPeriod = {
  id: string;
  periodType: string;
  startDate: string;
  endDate: string;
  status: string;
};

export type PayrollView = {
  runId: string;
  periodId: string;
  status: string;
  engineVersion: string;
  approvedBy?: string | null;
  approvedAt?: string | null;
  regularRates: Record<string, string>;
  lines: {
    personId: string;
    workDate: string;
    bucket: string;
    hours: number;
    rate: number;
    amount: number;
    explanation: Record<string, string>;
  }[];
  exceptions: {
    personId: string;
    workDate: string;
    type: string;
    severity: string;
    blocker: boolean;
    message: string;
  }[];
  gustoCsv: string;
  snapshotSha256?: string;
  success: {
    punchesAccounted: boolean;
    everyEmployeeHasALine: boolean;
    regularRateShown: boolean;
    approvedByPayrollApprove: boolean;
    snapshotStored: boolean;
    gustoExportReady: boolean;
    pairingProblemsFlagged: boolean;
  };
};

export type AccountView = {
  tenantName: string;
  plan: string;
  status: string;
  trialEndsAt?: string | null;
  auditPack: boolean;
  locationCount: number;
  employeeCount: number;
  periodCount: number;
  maxLocations: number;
  maxEmployees: number;
  maxPayPeriods: number | null;
  rulePacks: string[];
  secondStatePack: boolean;
  writable: boolean;
  permissions: string[];
  catalog: {
    name: string;
    monthlyCents: number;
    who: string;
    support: string;
  };
  positioning: string[];
};

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setSession(auth: AuthResponse) {
  localStorage.setItem(TOKEN_KEY, auth.accessToken);
  localStorage.setItem("aegispay.session", JSON.stringify(auth));
}

export function getSession(): AuthResponse | null {
  const raw = localStorage.getItem("aegispay.session");
  return raw ? (JSON.parse(raw) as AuthResponse) : null;
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem("aegispay.session");
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const token = getToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (init.body && !headers.has("Content-Type") && typeof init.body === "string" && init.body.startsWith("{")) {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(path, { ...init, headers });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || response.statusText);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return response.json() as Promise<T>;
  }
  return response.text() as Promise<T>;
}

export const api = {
  login: (email: string, password: string) =>
    request<AuthResponse>("/api/v1/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password })
    }),
  signup: (body: { legalName: string; displayName: string; email: string; password: string; vertical: string }) =>
    request<AuthResponse>("/api/v1/auth/signup", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    }),
  me: () => request<AuthResponse>("/api/v1/auth/me"),
  locations: () => request<Location[]>("/api/v1/locations"),
  people: () => request<Person[]>("/api/v1/people"),
  punches: () => request<Punch[]>("/api/v1/punches"),
  importPunches: (locationId: string, csv: string, fileName: string) =>
    request(`/api/v1/punches/import?locationId=${locationId}&fileName=${encodeURIComponent(fileName)}`, {
      method: "POST",
      headers: { "Content-Type": "text/plain" },
      body: csv
    }),
  periods: () => request<PayPeriod[]>("/api/v1/pay-periods"),
  createPeriod: (startDate: string, endDate: string) =>
    request<PayPeriod>("/api/v1/pay-periods", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ periodType: "BIWEEKLY", startDate, endDate })
    }),
  calculate: (periodId: string) =>
    request<PayrollView>(`/api/v1/pay-periods/${periodId}/runs`, { method: "POST" }),
  approve: (runId: string) =>
    request<PayrollView>(`/api/v1/pay-runs/${runId}/approve`, { method: "POST" }),
  account: () => request<AccountView>("/api/v1/account"),
  catalog: () => request<{ plans: { code: string; name: string; who: string; monthlyCents: number; maxLocations: number; maxEmployees: number; support: string; auditPackCents: number }[]; noFreeForeverPlan: string; positioning: string[] }>("/api/v1/catalog")
};
