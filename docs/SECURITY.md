# AegisPay security posture (v1)

You store staff names, emails, wages, hours, and locations. Treat it like HR data.

## We do not store

SSN, bank accounts, driver’s licenses. Clock CSVs that include those columns are rejected (`ForbiddenHrColumnGuard`).

## Controls in this repo

- TLS at the load balancer / host. App cookies are not used (JWT in Authorization).
- Encryption at rest: managed Postgres disk.
- Secrets: environment (`JWT_SECRET`, `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`). Never log them.
- Backups: daily + before migrate (see `runbooks/restore-backup.md`).
- SQL: Spring Data / parameterized only.
- File upload: CSV/TSV, 2 MiB cap, stored as punch rows not as files in the web root. Audit PDFs live under `aegispay.storage.local-dir`.
- Rate limit: login 10/min/IP, import 20/min/IP.
- CORS: `aegispay.cors.origin` only.
- Headers: nosniff, DENY frame, no-referrer, no-store.
- Logs: tenant, actor, action, requestId. Punch payloads at INFO must be ids, not names.

## HIPAA

Default: not a business associate. No PHI. Refuse EHR integrations that pull patient data. If a group demands a BAA, a lawyer reviews a narrow BAA that states no PHI will be submitted.

## Pentest

Record the date of the last OWASP ZAP / lightweight pentest on the public security page before 10 paying tenants.
