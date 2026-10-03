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
  approvalIp?: string | null;
  unlockPending?: boolean;
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
    id: string;
    personId: string;
    workDate: string;
    type: string;
    severity: string;
    blocker: boolean;
    message: string;
    dismissed: boolean;
    dismissReason?: string | null;
  }[];
  register: {
    personId: string;
    legalName: string;
    employeeCode: string;
    regularHours: number;
    regularPay: number;
    otHours: number;
    otPay: number;
    dtHours: number;
    dtPay: number;
    premiums: number;
    differentials: number;
    bonus: number;
    grossEarningsSubmitted: number;
  }[];
  gustoCsv: string;
  genericCsv?: string;
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

export type BonusEntry = {
  id: string;
  personId: string;
  amount: number;
  earnedOn: string;
  discretionary: boolean;
  note?: string | null;
  payPeriodId?: string | null;
};

export type LaborReport = {
  runId?: string | null;
  byLocation: { name: string; amount: number }[];
  byJob: { name: string; amount: number }[];
  gross: number;
  otPay: number;
  premiums: number;
};

export type RiskView = {
  premiumsGenerated: number;
  overtimePay: number;
  gross: number;
  daysUntilPeriodEnd: number;
  blockerExceptions: number;
  unapprovedPeriods: number;
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
  punches: () => request<{ requestId: string; items: Punch[]; nextCursor: string | null; limit: number }>("/api/v1/punches"),
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
  latestRun: (periodId: string) =>
    request<PayrollView | null>(`/api/v1/pay-periods/${periodId}/runs/latest`),
  approve: (runId: string, confirmed: boolean) =>
    request<PayrollView>(`/api/v1/pay-runs/${runId}/approve`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ confirmed })
    }),
  exportRun: (runId: string) =>
    request<PayrollView>(`/api/v1/pay-runs/${runId}/export`, { method: "POST" }),
  requestUnlock: (runId: string) =>
    request(`/api/v1/pay-runs/${runId}/unlock-request`, { method: "POST" }),
  confirmUnlock: (runId: string) =>
    request<PayrollView>(`/api/v1/pay-runs/${runId}/unlock`, { method: "POST" }),
  dismissException: (runId: string, exceptionId: string, reason: string) =>
    request<PayrollView>(`/api/v1/pay-runs/${runId}/exceptions/${exceptionId}/dismiss`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ reason })
    }),
  bonuses: () => request<BonusEntry[]>("/api/v1/bonuses"),
  createBonus: (body: {
    personId: string;
    amount: string;
    earnedOn: string;
    discretionary: boolean;
    note: string;
    payPeriodId?: string;
  }) =>
    request<BonusEntry>("/api/v1/bonuses", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    }),
  account: () => request<AccountView>("/api/v1/account"),
  wizard: () => request<{ step: string; steps: string[]; legalName: string; vertical: string; locationCount: number; jobCodeCount: number; peopleCount: number; clockMapping: Record<string, string> }>("/api/v1/wizard"),
  jurisdictions: (city: string, region: string) =>
    request<{ timeZone: string; jurisdictions: string[]; confirm: string }>(
      `/api/v1/wizard/jurisdictions?city=${encodeURIComponent(city)}&region=${encodeURIComponent(region)}`
    ),
  catalog: () => request<{ plans: { code: string; name: string; who: string; monthlyCents: number; maxLocations: number; maxEmployees: number; support: string; auditPackCents: number }[]; noFreeForeverPlan: string; positioning: string[] }>("/api/v1/catalog"),
  leaveBalances: () => request<{ personId: string; policyCode: string; hours: number; asOf: string }[]>("/api/v1/leave/balances"),
  laborReport: () => request<LaborReport>("/api/v1/reports/labor"),
  risk: () => request<RiskView>("/api/v1/reports/risk"),
  acceptInvite: (token: string, displayName: string, password: string) =>
    request<AuthResponse>("/api/v1/auth/accept-invite", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token, displayName, password })
    }),
  checkout: (plan: string, annual: boolean) =>
    request<{ url: string }>("/api/v1/billing/checkout", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ plan, annual })
    }),
  billingPortal: () => request<{ url: string }>("/api/v1/billing/portal", { method: "POST" }),
  generateAuditPack: (runId: string) =>
    request<{ sha256: string; bytes: number; fileName: string }>(`/api/v1/pay-runs/${runId}/audit-pack`, { method: "POST" }),
  downloadAuditPack: async (runId: string) => {
    const token = getToken();
    const response = await fetch(`/api/v1/pay-runs/${runId}/audit-pack`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    });
    if (!response.ok) {
      throw new Error(await response.text());
    }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `aegispay-audit-${runId.slice(0, 8)}.pdf`;
    link.click();
    URL.revokeObjectURL(url);
  }
};
