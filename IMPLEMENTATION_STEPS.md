# AegisPay — Implementation Steps

**Product:** Multi-tenant labor-compliance and earnings engine for independent outpatient clinic groups  
**Stack (locked for this project):** Java 21, Spring Boot 3, PostgreSQL, Redis, React + TypeScript  
**Status of this document:** Blueprint plus v0.1 codebase (engine, API, Harbor Dental seed, React shell). Do not treat the engine as counsel-reviewed.

---

## 0. The product you are building (read this before any coding)

### 0.1 One-sentence pitch

AegisPay is a B2B SaaS backend that takes raw punches from a clinic time clock, applies federal + state wage-and-hour rules plus clinic-specific pay structures, and outputs payroll-ready earnings, premium pay, and an audit pack the owner can defend if the Department of Labor or a plaintiff’s attorney asks questions.

### 0.2 Why this is sellable (and why not a generic payroll app)

Generic payroll (Gusto, ADP, QuickBooks Payroll) is good at tax filing and direct deposit. It is weak at the math that actually gets clinics fined:

- California daily overtime after 8 hours, double-time after 12, seventh-day overtime
- Meal-break and rest-break premium pay (one extra hour at the regular rate per missed break type per day)
- Split-shift premiums and reporting-time pay
- Regular-rate-of-pay recalculation when a hygienist also earns a monthly production bonus
- Dual rates in one day (chair-side vs front desk vs sterilization)
- On-call / callback minimums for urgent care
- Travel time between two locations of the same group
- State sick-leave accrual that differs by city (Los Angeles vs unincorporated county vs New York City)

Clinic owners already pay Gusto. They will **also** pay $199–$499/month if AegisPay is the system that **guarantees the hours Gusto is about to pay are legally correct**. You are not replacing Gusto. You sit in front of it. That is the wedge, the liability story, and the price justification.

You will **not** remit payroll taxes, file 941s, or move money in v1. Tax remittance requires money-transmitter complexity, banking partners, and insurance you cannot afford as a first product. Earnings + compliance + export is enough to sell.

### 0.3 Ideal first customer (write this on a sticky note)

- Independent dental group, PT clinic group, urgent care, dermatology, or veterinary group
- 2–12 locations, 15–120 W-2 hourly staff
- At least one location in a strict state: California, New York, Washington, Colorado, or Illinois
- Office manager currently exporting CSV from a time clock, fixing it in Excel, then uploading to Gusto
- Owner has already been scared by a wage-claim letter, a PAGA inquiry, or a missed meal-break story from a peer

Average contract: **$299/month** for up to 5 locations, **$15/employee/month** overage, annual prepay at 2 months free. Target first-year: 20 groups = ~$6k–$8k MRR. That is a real micro-SaaS, not a toy.

### 0.4 What you will never build in v1 (scope lock)

Do not add an EHR, patient scheduling, insurance billing, full HRIS, applicant tracking, benefits administration, 401(k), or in-house tax filing. Those features kill the timeline and the story. If a prospect asks, the answer is: “We make the pay legally correct and hand a clean file to the payroll provider you already use.”

### 0.5 Legal posture (non-negotiable before you take a paid customer)

- Software provides **calculation assistance**, not legal advice.
- Every rule pack is versioned, cited to a statute/regulation, and dated.
- Terms of Service: customer remains the employer of record; AegisPay is not a Professional Employer Organization.
- Buy errors-and-omissions insurance before the first paid tenant.
- Have an employment-law attorney review the California and FLSA rule packs before you market “compliance.”
- Never claim “HIPAA certified.” You can be HIPAA-aware (BAAs, encryption, audit logs) without storing clinical PHI. **Do not store diagnoses, charts, or patient names.** Staff names, hours, and pay are employment data, not PHI, unless you foolishly connect an EHR.

---

## 1. Step 1 — Validate the idea with money, not opinions

Do this before treating the app as sellable. If you cannot get a clinic manager to describe their Excel ritual, the product is a hobby.

Working files live in `step-01-validation/`. The concierge CLI is `com.aegispay.engine.concierge.ConciergeMain` (engine module). Sample CSVs are in `step-01-validation/samples/`. Fill `02-buyer-pipeline.csv` and `05-go-no-go.md` before Step 2.

### 1.1 Write a one-page offer

Create a Google Doc titled “AegisPay — Pre-payroll compliance for clinic groups” with:

- The Excel ritual you are killing (punches → OT → meal premiums → production true-up → Gusto)
- Three screenshots you will mock later (punch timeline, exception queue, payroll-ready register)
- Price: $299/month for 5 locations, 14-day pilot on their last two pay periods
- Guarantee language: “If we cannot find at least one under/overpayment in your last 60 days of punches, the pilot is free”

### 1.2 Find 30 real buyers

Sources, in order:

1. Local dental study clubs, state dental association classifieds, DSO office-manager Facebook groups
2. PT clinic multi-location groups (often 4–8 sites, production-based pay)
3. Veterinary groups that just rolled up 3 hospitals
4. Accountants and bookkeepers who run payroll for 10+ clinics (they are channel partners, even better than one clinic)

Message script (do not pitch features):

> “Office managers at multi-location clinics tell me they spend a day before each payroll fixing overtime, missed lunches, and hygienist bonuses in Excel so Gusto doesn’t underpay people. I’m building the checker that does that automatically and produces an audit file. If I ran your last two pay periods for free, would you send me a de-identified punch export?”

### 1.3 Run 8 discovery calls

Ask only:

1. What does the day before payroll look like, hour by hour?
2. Which mistakes have you actually made in the last year?
3. Which state(s) and cities are your clinics in?
4. How many pay rates can one person have in a single day?
5. Do you pay production / collections bonuses? How often? Do they get folded into overtime?
6. What time clock do you use today (Deputy, Homebase, TimeClock Plus, paper, Google Sheet)?
7. What payroll system (Gusto, ADP, Paychex, QuickBooks, Paycom)?
8. What would you pay monthly to never think about meal-break premiums again?

Stop building if fewer than 3 people will send real (de-identified) punch data. If 3+ send data, you have a product.

### 1.4 Manual concierge test (the first “sale”)

Before software exists:

1. Import their CSV into a spreadsheet.
2. Apply rules by hand for one pay period.
3. Deliver a PDF: exceptions found, dollars at risk, recommended premiums.
4. Charge $150 for the second pay period as a “done-with-you audit.”

If they pay $150 for a PDF, they will pay $299 for software. If they will not pay $150, do not write Java yet.

### 1.5 Go / no-go checklist

Proceed to Step 2 only if all are true:

- [ ] At least 3 clinics described the same Excel ritual
- [ ] At least 1 clinic paid or firmly committed to a paid pilot
- [ ] You can name the first 3 time-clock formats you must parse
- [ ] You can name the first 2 payroll-export formats (almost always Gusto CSV + generic)
- [ ] You accept that v1 rule packs are **CA + federal FLSA only**, plus one more state the first customer needs

---

## 2. Step 2 — Lock the commercial product, not the architecture

Catalog, limits, `PAYROLL_APPROVE`, and the payroll-success checklist are encoded in `com.aegispay.app.billing` (`PlanCatalog`, `EntitlementService`, `Permission`). New signups start on **Pilot** (14 days, 1 location, 2 periods). Harbor Dental seed is **Group**. There is no free-forever plan.

### 2.1 Packages you will sell

| Plan | Who | Price | Limits |
|------|-----|-------|--------|
| Pilot | One group, 14 days | $0 then convert | 2 pay periods, 1 location, you onboard them yourself |
| Group | 2–5 locations | $299 / month | 80 employees, CA+FLSA rules, Gusto export, email support |
| Network | 6–20 locations | $599 / month | 250 employees, second state pack, SSO later, Slack/email SLA |
| Audit Pack add-on | Any | $99 / payroll run | Attorney-ready PDF with statute citations and punch-level math |

Do not offer a free forever plan. Clinic software with a free plan attracts students, not buyers.

### 2.2 Buyer, user, champion

- **Buyer:** Owner or managing partner (signs the card, fears lawsuits)
- **Daily user:** Office manager / payroll coordinator
- **Influencer:** External bookkeeper or CPA
- **End users of clock:** Hygienists, assistants, front desk — they must not need training beyond “punch as today”

Design the UI for the office manager. The owner sees a weekly “risk dashboard.” Staff see almost nothing in v1 if you ingest punches from an existing clock.

### 2.3 Success metric for a paying tenant

A payroll run is successful when:

1. Every punch is either accepted, corrected with a reason code, or flagged.
2. Every employee has a line: regular hours, OT hours, DT hours, premiums, bonuses, differentials.
3. Regular rate is shown, not hidden, including the bonus true-up.
4. A manager with the `PAYROLL_APPROVE` permission signed off.
5. An immutable snapshot of inputs + rule-pack version + outputs is stored.
6. A file downloads that Gusto (or their provider) can import without reformatting.

### 2.4 Competitive positioning you will repeat on the website later

- vs Gusto: they pay people; we make the hours lawful before they do
- vs Homebase/Deputy: they schedule; they do not correctly recompute regular rate after a production bonus
- vs HR for Health: they are a full HR suite at a higher price; we are the specialist calculation layer
- vs WageRoot: they are California restaurants; we are multi-state clinics with dual rates and production pay

---

## 3. Step 3 — Architecture decisions you must freeze now

Changing these later is expensive. Freeze them in `ArchitectureFreeze`, ArchUnit tests, `RulePackResolver`, and `TenantIsolationIT`. Shared schema + `tenant_id`, modular monolith packages (`platform`, `org`, `time`, `rules`, `payroll`, `billing`), pure-Java engine, `NUMERIC(12,4)` money, versioned rule packs, handwritten mappers, Java 21 / Boot 3.3 / Postgres 16 / Redis 7.

### 3.1 Multi-tenancy model

Use **shared database, shared schema, mandatory `tenant_id` on every business table** (discriminator isolation).

Why not schema-per-tenant or database-per-tenant in v1:

- You will have dozens of tenants, not thousands, in year one
- Flyway migrations stay simple
- Reporting across tenants (for you, the operator) is possible
- You can still add schema-per-tenant later for a huge DSO if they demand it

Rules:

- Hibernate filter or Spring Data interceptor sets `tenant_id` from the authenticated JWT
- Every query is tenant-scoped; integration tests prove a tenant cannot read another tenant’s punches
- Super-admin (you) uses a separate `platform` schema for billing, feature flags, and rule-pack publishing
- Never skip `tenant_id` “just for this debug query” in production code

### 3.2 Modular monolith, not microservices

One Spring Boot app with packages:

- `platform` — identity, tenancy, billing, audit
- `org` — tenants, locations, people, jobs, rates
- `time` — punches, shifts, edits, attestations
- `rules` — versioned rule packs, evaluators
- `payroll` — pay periods, runs, earnings lines, exports
- `billing` — Stripe subscriptions

Do not split these into separate deployables until a module has an independent scaling problem. A solo founder with microservices will not ship.

### 3.3 Calculation engine is pure Java, not SQL

Wage-and-hour math does not belong in PostgreSQL functions.

- Input: immutable `WorkPeriod` (list of punches, assignments, bonuses, location jurisdiction)
- Output: immutable `EarningsResult` (lines + explanations + citations)
- Engine is deterministic: same inputs + same rule-pack version = same output, always
- Engine has **zero** Spring dependencies so you can unit-test it with plain JUnit
- Persistence happens in the application service after the engine returns

This engine is the product. Protect it.

### 3.4 Time and money types

- Store all timestamps as `timestamptz` in UTC
- Convert to the **location’s IANA time zone** before applying calendar-day overtime and meal-break windows
- Never use `float` or `double` for money
- Use `BigDecimal` with scale 4 internally, scale 2 on statements, `RoundingMode.HALF_UP` unless a jurisdiction specifies otherwise (document the exception)
- Store money as integer cents **or** `NUMERIC(12,4)` — pick `NUMERIC(12,4)` and stay consistent
- Hours stored as `NUMERIC(8,4)` (0.25 = 15 minutes). Decide whether you round punches to 1, 5, or 15 minutes **per tenant policy**, default 1 minute for compliance honesty

### 3.5 Rule-pack versioning

A rule pack is a versioned artifact, not a pile of if-statements that you edit in prod.

- `rule_pack` table: `id`, `jurisdiction` (US-FLSA, US-CA, US-CA-LOS-ANGELES, …), `version`, `effective_from`, `effective_to`, `sha256`, `status` (draft/published/retired)
- Payroll run stores `rule_pack_ids` used
- Re-running an old period uses the packs that were effective **then**, unless the user explicitly “recalculate with current law”
- Publish packs from the platform admin, not from tenant settings

### 3.6 Tech versions to generate

- Java 21 LTS
- Spring Boot 3.3+ (or current 3.x LTS at the time you start)
- Gradle (Kotlin DSL)
- PostgreSQL 16
- Flyway
- Redis 7 (job queue + rate limits)
- Spring Security resource server (JWT)
- MapStruct or handwritten mappers (pick one; do not mix)
- Testcontainers for PostgreSQL in integration tests
- OpenAPI 3 generated from controllers
- React 18+, TypeScript, Vite, React Query, a paid component library only if it saves a week (TanStack Table is enough)
- Stripe Billing
- Docker Compose for local Postgres + Redis
- Deploy target: a single $20–40 VPS or Fly.io/Railway at first; move to ECS/Cloud Run when a customer requires a BAA and a named region

### 3.7 Repository layout (create in Step 5, not now)

```
AegisPay/
  IMPLEMENTATION_STEPS.md    ← this file
  backend/                   ← Gradle Spring Boot
  frontend/                  ← Vite React
  docs/                      ← rule citations, CSV specs
  docker-compose.yml
```

---

## 4. Step 4 — Domain model (design on paper, then as Flyway)

Draw this on a whiteboard, then encode it as Flyway `V9__domain_model.sql` plus JPA in `org`, `time`, `payroll`, and `billing`. If a table does not support a payroll run, it does not exist in v1. `geo_claim` waits until v1.1. Punches are voided with `timesheet_edit`; earnings are voided with a reversing line; only person and location use `deleted_at`.

### 4.1 Platform (your company)

- `platform_user` — you and future staff; not clinic staff
- `tenant` — one paying clinic group
- `subscription` — Stripe customer id, plan, status, current period
- `feature_flag`
- `rule_pack` + `rule_pack_document` (markdown/PDF citation bundle)

### 4.2 Tenant organization

- `location` — name, address, IANA timezone, jurisdiction codes (state, city, county), opening hours (for split-shift logic)
- `person` — employee or contractor flag, legal name, hire date, termination date, exemption status (exempt salaried clinicians vs non-exempt hourly)
- `employment` — a person can have multiple employment stints
- `job_code` — RDH, RDA, front desk, assistant, kennel tech, PT aide
- `assignment` — person + location + job_code + effective dating
- `pay_rate` — assignment-scoped; types: HOURLY, SALARY_EXEMPT (engine skips OT), PER_DIEM, DIFFERENTIAL (weekend, evening)
- `compensation_plan` — PRODUCTION_PERCENT_COLLECTIONS, PRODUCTION_PERCENT_PRODUCTION, FLAT_BONUS, SHIFT_DIFF
- `leave_policy` — sick/vacation accrual method, cap, carryover, state overlay

Effective dating is mandatory. A raise on Wednesday must not rewrite Monday’s rate. Every rate row has `effective_from` / `effective_to`.

### 4.3 Time

- `punch` — person, location, type (IN, OUT, BREAK_START, BREAK_END, TRANSFER), source (CSV, API, MANUAL), original timestamp, adjusted timestamp, adjust reason, adjusted_by
- `punch_import_batch` — file name, hash, row count, status
- `timesheet_edit` — append-only; never UPDATE a punch in place without writing a compensating audit row
- `meal_attestation` — asked at clock-out: “Were you provided an uninterrupted 30-minute meal break starting before the end of the 5th hour?” answers YES/NO/WAIVED
- `geo_claim` (optional v1.1) — you may skip GPS in v1

### 4.4 Payroll

- `pay_period` — start, end, type WEEKLY/BIWEEKLY, status OPEN/LOCKED
- `pay_run` — created_by, approved_by, approved_at, engine_version, rule_pack_versions JSON, status
- `earnings_line` — person, date, bucket (REG, OT_1_5, OT_2_0, MEAL_PREMIUM, REST_PREMIUM, SPLIT_SHIFT, REPORTING_TIME, DIFFERENTIAL, BONUS_TRUE_UP), hours, rate, amount, explanation JSON
- `exception` — type, severity, person, date, blocker (yes/no for approve)
- `export_file` — Gusto CSV bytes, checksum, generated_at
- `pay_run_snapshot` — compressed JSON of all inputs used (punches, rates, bonuses) so you can reproduce the run in court

### 4.5 Audit

- `audit_event` — actor, tenant, action, entity, before/after JSON, ip, user-agent, at
- Append-only. No updates, no deletes. Partition by month later.

### 4.6 Indexes you will need on day one

- `(tenant_id, person_id, timestamp)` on punches
- `(tenant_id, pay_period_id)` on earnings
- `(tenant_id, location_id)` on people assignments
- Unique `(tenant_id, external_employee_code)` if they import from a clock

### 4.7 Soft deletes

`deleted_at` on person/location only. Punches and earnings are never deleted; they are voided with a reversing line.

---

## 5. Step 5 — Scaffold the backend the boring way

### 5.1 Create the Gradle project

Scaffold lives in `backend/` (`app` + `engine`), with Spotless, `application-local.yml` / `application-prod.yml` / `application-test.yml`, actuator health+info, Problem Details, `Idempotency-Key` on import/run, and cursor pagination on punches. Harbor Dental seed is eight people and two weeks of punches.

- `com.aegispay` base package
- Modules: `app` (Spring Boot), `engine` (pure Java, no Spring), `engine` is a dependency of `app`
- Enable: Spring Web, Validation, Security, Data JPA, Flyway, Actuator, Mail (later)
- Checkstyle or Spotless; fail the build on warnings you care about
- `.env.example` with `DATABASE_URL`, `REDIS_URL`, `JWT_ISSUER`, `STRIPE_SECRET`, `APP_BASE_URL`

### 5.2 Configuration layers

`application.yml` + `application-local.yml` + `application-prod.yml`

Never commit secrets. Use environment variables in prod.

Profiles:

- `local` — Docker Compose Postgres/Redis, verbose logs, seed data
- `test` — Testcontainers
- `prod` — JSON logs, Flyway migrate on, no seed, HTTPS only

### 5.3 Health and ops from minute one

Actuator: `/actuator/health` (DB + Redis), `/actuator/info` (git commit), `/actuator/prometheus` later.

A dead app with a pretty UI is not sellable. Health checks are part of the product.

### 5.4 Flyway baseline

First migrations:

1. `V1__platform.sql` — tenant, platform_user
2. `V2__org.sql` — location, person, assignment, pay_rate
3. `V3__time.sql` — punch, import_batch
4. `V4__rules.sql` — rule_pack
5. `V5__payroll.sql` — pay_period, pay_run, earnings_line
6. `V6__audit.sql` — audit_event

Do not use `ddl-auto=update` except in the first afternoon of hacking. Turn it off before any tenant data exists.

### 5.5 API conventions

- `/api/v1/...`
- JSON in/out
- Problem Details (`application/problem+json`) for errors
- Idempotency-Key header on imports and payroll-run creation
- Pagination: `cursor` not `page` for punches (they grow fast)
- All responses include `requestId` for support tickets

### 5.6 Seed a demo tenant

A fake “Harbor Dental Group” with 2 locations (Los Angeles, Austin), 8 employees, 3 job codes, 2 weeks of punches including: a missed lunch, a 9-hour CA day, a weekend differential, and a hygienist bonus. This demo tenant is what you will click through on sales calls. Build the seed before the real UI polish.

---

## 6. Step 6 — Identity, tenancy, and permissions

### 6.1 Authentication

v1: email + password with Spring Security, JWTs (access 15 min, refresh 7 days, rotate refresh tokens). Invites expire in 48 hours. Cross-tenant punch ids return 404. MFA for `PAYROLL_APPROVE` is coded and gated by `aegispay.mfa.payroll-approve` (off until you take cards for more than three tenants).

- Argon2id or BCrypt cost ≥ 12 for passwords
- Email verification required before the tenant can run payroll
- MFA TOTP required for anyone with `PAYROLL_APPROVE` (you can ship MFA in v1.1 if it delays the pilot, but put it on the calendar before you take card payments for >3 tenants)

Do not implement SAML in v1 unless a 20-location group makes it a written condition of a $599 plan.

### 6.2 Roles (RBAC, not a pile of booleans)

| Role | Can |
|------|-----|
| OWNER | billing, all locations, approve payroll, manage users |
| PAYROLL_ADMIN | imports, edits with reason, run calculations, cannot change billing |
| LOCATION_MANAGER | see and attest punches for their location only |
| VIEWER | bookkeeper: read runs and download exports |
| STAFF | none in v1 if you only ingest clocks (add self-service later) |
| PLATFORM_ADMIN | you: impersonate with audit, publish rule packs, freeze tenants |

Every permission check is on the server. The React UI hiding a button is not security.

### 6.3 Tenant resolution

1. User logs in
2. JWT contains `tid` (tenant uuid) and `roles`
3. `TenantFilter` sets `TenantContext` ThreadLocal
4. Hibernate filter `tenantFilter` adds `tenant_id = :tid`
5. Clear ThreadLocal in a `once-per-request` filter `finally` block (virtual threads or pooled threads will leak tenants if you forget this)

Write a test: Tenant A token + Tenant B punch id → 404, not 403 with data.

### 6.4 Invite flow

Owner invites payroll admin by email → token link 48h → set password → land in the app.

Record who invited whom. Clinics have turnover; you will be asked “who added this person?”

---

## 7. Step 7 — Organization setup wizard (the onboarding that makes it sellable)

A tool that needs a consultant to configure will not be a micro-SaaS. The wizard is a product feature (`/api/v1/wizard`, `JurisdictionResolver`, vertical JSON templates).

### 7.1 Wizard screens, in order

1. **Group profile** — legal name, EIN not required in v1 (do not collect SSNs)
2. **Locations** — address autocomplete optional; timezone inferred from address; user confirms; jurisdiction chips auto-selected (State CA, City Los Angeles)
3. **Pay period** — weekly Saturday–Friday vs biweekly; first period start date
4. **Job codes** — start from a template: Dental (RDH, RDA, Front Office, Assistant), PT, Vet, Urgent Care
5. **People import** — CSV: name, email, location, job, hourly rate, overtime eligible, hire date
6. **Time clock mapping** — which column is employee id, in, out, break
7. **Payroll destination** — Gusto / ADP / Generic
8. **Policies** — punch rounding, whether meal waivers are allowed for shifts ≤ 6 hours (CA), whether you pay rest premiums automatically or only when attested missed

### 7.2 Jurisdiction resolver

Input: location address  
Output: list of jurisdiction codes that apply, ordered most-specific last so city overrides state overrides federal.

Example: a clinic in Santa Monica, CA → `[US-FLSA, US-CA, US-CA-SANTA-MONICA]` if you have a city minimum wage pack, else `[US-FLSA, US-CA]`.

Store the resolved list on the location and re-resolve if they edit the address. Never silently change jurisdiction after a pay period is locked.

### 7.3 Templates by vertical

Ship four JSON templates that pre-fill job codes and common differentials:

- Dental
- Physical therapy
- Urgent care
- Veterinary

The vertical is a sales feature. “Built for dental groups” closes deals. The engine underneath can be generic.

---

## 8. Step 8 — Punch ingestion (garbage in is how you lose lawsuits)

### 8.1 CSV importer first, APIs second

Office managers live in CSV. Build preview (`/api/v1/punches/preview`), validate, commit, and require a reason code on every edit (`PunchReasonCode`). Pairing edge cases live in `PunchPairerTest`.

1. Upload
2. Preview first 20 rows with mapped columns
3. Validate (unknown employee, out before in, 36-hour shift, duplicate row)
4. Commit as `punch_import_batch`
5. Show a diff if the same file is uploaded twice (idempotent on file hash)

Supported clocks to document in `docs/csv-specs.md` when you write it later:

- Generic 7-column (employee_code, first, last, timestamp, type, location, tz)
- Homebase export
- Deputy export
- Time Clock Plus
- Manual Excel from front desk

You do not need official APIs for the first 10 customers. You need bulletproof CSV.

### 8.2 Punch pairing

A day is not “two rows.” Pair IN/OUT into intervals, then attach BREAK intervals inside them.

Edge cases you must handle in code and tests:

- Missing OUT (still clocked in at import time)
- Double IN
- Break start without break end
- Cross-midnight urgent-care shift (19:00–07:00) — calendar day vs workday; CA daily OT uses the workday definition the employer adopted; default to location-local calendar date of shift start and make the workday definition a tenant setting
- Clock-in at location A, clock-out at location B (transfer)
- Punch in wrong location (manager correction with reason)

### 8.3 Edits are evidence

Any change to a punch requires:

- Reason code (MISSED_PUNCH, WRONG_LOCATION, TRAINING, SYSTEM_ERROR, OTHER + text)
- Actor
- Old value / new value
- Employee acknowledgment optional in v1.1

The UI must make silent edits impossible. Silent edits are how you become Exhibit B in a lawsuit.

### 8.4 Attestations

On the manager review screen (v1) and employee clock (v1.1):

- Meal break received? waived? interrupted?
- Rest breaks received?

If the employee says interrupted, the engine **must** generate a premium even if the punch timestamps look fine. Attestation wins over inferred break windows when it is worse for the employer (that is the conservative, sellable default). Make “employer-friendly inference” an explicit setting, default off.

---

## 9. Step 9 — Build the calculation engine (this is the company)

Work in the `engine` module with TDD. No database. No Spring. Pipeline order is `EvaluationPipeline`; version `0.2.0`. Citations: `engine/src/main/resources/rules/ca/IWC_Wage_Order_4.md`.

### 9.1 Public API of the engine

```text
EarningsResult calculate(WorkPeriod period, List<RulePack> packs, EngineOptions options)
```

`WorkPeriod` contains:

- Employee id (opaque)
- Exemption status
- List of `Shift` (intervals + location + job + applicable rates)
- Bonuses allocated to this period (amount, type, discretionary flag)
- Attestations
- Tenant policy flags
- Workweek start day (critical for FLSA)

`EarningsResult` contains:

- Lines
- Exceptions
- Explanation tree (human readable + machine citations)
- Totals
- Warnings (e.g. “salary non-exempt misclassified?”)

### 9.2 Evaluation pipeline (fixed order — do not improvise per customer)

1. **Normalize** punches into intervals in location TZ
2. **Classify** each interval: regular work, paid break, unpaid meal, travel, on-call
3. **Apply rounding policy**
4. **Detect meal/rest violations** (jurisdiction specific)
5. **Compute daily buckets** (CA 8/12, 7th consecutive day)
6. **Compute weekly FLSA OT** on remaining hours after daily OT is peeled off (order matters; document it)
7. **Apply differentials** (stacking rules: percentage of base vs flat; does evening diff enter the regular rate? yes if non-discretionary)
8. **Compute regular rate** for the workweek:  
   `(all non-overtime straight-time earnings + non-discretionary bonuses + differentials that must be included) / total hours worked`  
   then OT premium is `0.5 * regularRate * OT hours` in addition to hours already paid at straight time — **or** follow the method you document and test against DOL examples. Implement the DOL example from 29 CFR 778 as golden tests.
9. **Allocate production bonuses** that arrive monthly across workweeks in that month (or the policy the tenant chose: this period only)
10. **Premium pay** for meal/rest/split-shift/reporting-time — these are generally **not** hours worked; they are additional wages. Do not feed them back into OT hours. Confirm per jurisdiction and cite it.
11. **Leave accrual** (can be a later phase if needed; see Step 12)
12. **Emit explanations**

If you change this order, version the engine (`engine_version` on the pay run).

### 9.3 Federal FLSA pack (ship first even if first customer is in CA)

Must include:

- 40-hour workweek overtime at 1.5× regular rate
- Regular rate includes non-discretionary bonuses
- Salaried non-exempt handling (fluctuating workweek — **do not enable by default**; it is a lawsuit magnet; hide behind a flag and a warning)
- Hours worked vs hours paid
- Workweek is a fixed 168-hour period; tenant chooses start day and cannot change it mid-year without a documented cutover

Golden tests: copy numeric examples from DOL Fact Sheet #23 and 29 CFR 778. There should be a test named after each example.

### 9.4 California pack (the pack that sells)

Implement with citations stored beside the code (`rules/ca/IWC_Wage_Order_4.md` etc. — clinics are often Wage Order 4, professional; dental offices may fall under Wage Order 4 or 5 depending on facts; **do not guess in marketing**. Let the tenant pick the wage order with help text and a “ask your counsel” link).

Minimum CA behaviors for v1:

- Daily OT after 8, double-time after 12
- Seventh consecutive day: first 8 hours OT, after 8 double-time
- Meal: 30 unpaid minutes by end of 5th hour; second meal by 10th hour; waiver rules for 6-hour and 12-hour shifts
- Meal premium: 1 hour at regular rate per day per missed/late/short meal (Labor Code 226.7)
- Rest: 10 paid minutes per 4 hours or major fraction; premium if not provided
- Split-shift premium when applicable (Wage Order)
- Reporting-time pay when applicable
- Regular rate for premiums (post-*Ferra* / *Naranjo* issues: premiums treated as wages; interest/waiting-time — you do not calculate waiting-time penalties automatically in v1, you flag terminated employees with unpaid premiums)

California is where your E&O insurance and attorney review matter. Budget a paid review of this pack.

### 9.5 Second state pack

Build only the state your first paying customer needs (NY, WA, CO, or IL). Do not pre-build 50 states. Selling “all 50 states” before you have CA solid is how you get a bad reputation.

### 9.6 Clinic-specific calculators (your differentiation)

Implement these as add-on evaluators that still live in `engine`:

1. **Dual/multi rate day** — weighted regular rate, not “just use the highest rate” unless policy says so; default to weighted (FLSA-correct)
2. **Production bonus true-up** — monthly collections % for hygienists/associates who are still non-exempt (many “production” people are actually exempt; the engine must respect exemption; the wizard must warn)
3. **On-call** — unpaid waiting vs engaged to wait; callback minimum hours
4. **Inter-location travel** — paid at the rate of the destination or a travel job code
5. **Weekend / evening differentials**
6. **Holiday worked** vs holiday not worked (policy, not law, except some jurisdictions)

### 9.7 Explanation objects (sellable artifact)

Every earnings line has:

- `code` (CA_DAILY_OT)
- `citation` (e.g. “IWC Wage Order 4, §3(A)”)
- `inputs` (worked 9.25 hours, threshold 8)
- `formula` (“(9.25 - 8) * 1.5 * $28.00”)
- `narrative` (“Daily overtime: 1.25 hours at time-and-a-half”)

The UI renders this as a drawer. The Audit Pack PDF prints it. This is why people pay you instead of using Excel.

### 9.8 Property tests

In addition to golden DOL examples:

- Hours and amounts never negative
- Sum of daily buckets ≤ actual hours worked (except premiums)
- Re-running is idempotent
- Changing a punch in an unlocked period changes output; locked period refuses

---

## 10. Step 10 — Pay period, run, lock, export

Lifecycle `DRAFT` → `CALCULATED` → `EXCEPTIONS_PENDING` → `APPROVED` → `EXPORTED` → `LOCKED` (`PayRunLifecycle`). Dual-control unlock is PAYROLL_ADMIN request + OWNER confirm. Gusto fixture: `engine/src/test/resources/fixtures/gusto-import.csv`.

### 10.1 Pay period lifecycle

`DRAFT` → `CALCULATED` → `EXCEPTIONS_PENDING` → `APPROVED` → `EXPORTED` → `LOCKED`

- Recalculate allowed until APPROVED
- After APPROVED, only a `PAYROLL_ADMIN` + `OWNER` dual control can unlock, and unlocking writes an audit event emailed to the owner
- LOCKED after export + 24 hours, or immediately on export if the tenant setting says so

### 10.2 Exception queue

This is the office manager’s home screen the day before payday.

Blockers (cannot approve):

- Unpaired punches
- Employee missing a rate for that date
- Jurisdiction missing a pack
- Negative hours

Warnings (can approve with acknowledgment):

- OT hours > 20 in a week (possible data error)
- Meal premium generated
- Classification exempt but hours recorded
- Bonus not yet entered for month-end week

Each exception has “fix punch,” “enter rate,” “dismiss with reason.”

### 10.3 Payroll register UI

A spreadsheet-like view:

- Employee
- REG hours / $
- OT hours / $
- DT hours / $
- Premiums $
- Diff $
- Bonus $
- Gross (non-tax) $ — label it “gross earnings submitted to payroll provider” so nobody thinks you withheld tax

Click employee → day → punch timeline with color-coded violations.

### 10.4 Approval

- Checkbox: “I confirm I have reviewed exceptions and this run may be sent to our payroll provider.”
- Store name, timestamp, IP
- Generate PDF register + CSV

### 10.5 Exports

Gusto-compatible CSV first (map earning types to Gusto custom earnings: Regular, Overtime, Double time, Bonus, Reimbursement — meal premiums often as additional earnings).

Then generic CSV:

`employee_code, earning_type, hours, amount, date, location, note`

Then ADP if a customer pays for Network plan.

Keep a fixture file of a Gusto import that you re-test every time you change export code. A broken export is a lost customer the same week.

### 10.6 Bonus entry

Simple form: person, amount, date, discretionary yes/no, allocate-to period. Engine does the rest. Do not build a full commissions module in v1.

---

## 11. Step 11 — Audit pack (the feature that justifies $99 extra)

Server PDF (`AuditPackService`, deterministic `AuditPdfRenderer`). Bytes live in `LocalObjectStore`; `pay_run.audit_pdf_sha256` must match on regenerate.

### 11.1 What the PDF contains

1. Cover: tenant, period, engine version, rule packs, approver
2. Summary dollars by earning type
3. Per-employee register
4. Per-exception list with whether dismissed
5. Punch-level appendix for anyone with a premium or OT
6. Disclaimer: not legal advice; calculations based on tenant-configured wage order and punches supplied by customer

Generate with OpenPDF or similar on the server. Do not generate in the browser if you need a stable court artifact.

### 11.2 Immutability

Store the PDF bytes and the snapshot JSON in object storage (S3/R2). The pay_run row points at checksums. Regenerating a PDF for a locked run must produce the same checksum; if it does not, you have a bug in the engine or the PDF template — fail a test.

---

## 12. Step 12 — Leave accrual (phase after first paid customer, unless they require it)

CA sick leave is a pay-run post-step (`LeaveAccrualCalculator` 1 hour / 30 worked, city overlay cap). Ledger + cached balance. Not a PTO product.

If the first customer is in CA, you will be asked about sick leave.

Minimum viable:

- CA statewide sick leave accrual (1 hour per 30 hours worked, cap, usage)
- Local overlay if they are in a city with a more generous ordinance
- Accrual runs as a post-step of the pay run
- Balances table with ledger entries (never a single mutable integer as the only source of truth)

Do not build a full PTO request/approval product until someone pays more for it. Accrual + balance + export of “sick hours to pay” is enough.

---

## 13. Step 13 — Frontend that looks like software someone pays $300/month for

Light paper theme, owner risk home, reports, help drawers, 30-minute payroll session watchdog, location context in the nav, invite-accept screen. Destructive copy stays plain English.

### 13.1 Information architecture

- **Home (owner):** this week’s risk: premiums generated, OT as % of hours, unapproved period countdown
- **Queue (manager):** exceptions
- **Time:** punch browser, imports
- **People:** rates, assignments, exemption
- **Payroll:** periods and runs
- **Reports:** labor cost by location/job
- **Settings:** locations, policies, destinations, users, billing
- **Help:** “Why was this overtime?” links into explanation drawers

### 13.2 UX rules for a compliance product

- Destructive actions named in plain English (“Unlock approved payroll”)
- Red for legal exceptions, not for decorative buttons
- Every dollar clickable back to punches
- Do not use a dark theme as default; office managers print screenshots
- Empty states tell them the next file to upload, not a cute illustration only

### 13.3 Implementation order for screens

1. Login / invite accept
2. Wizard
3. CSV import + punch list
4. Exception queue
5. Payroll register + approve
6. Export download
7. People/rates
8. Owner dashboard
9. Audit PDF
10. Billing portal (Stripe)

Do not start with a marketing landing page. The app converting a pilot is more important. The landing page is Step 18.

### 13.4 Accessibility and trust

- Keyboard usable tables
- Contrast
- Session timeout 30 minutes for payroll roles with a warning
- “You are viewing Harbor Dental — Downtown” location context always visible

---

## 14. Step 14 — Billing (if you cannot charge, it is not a SaaS)

Stripe Checkout + Customer Portal + signed webhooks (`StripeSignature`). Failed invoice: 7-day grace, then `READ_ONLY`. Annual is a separate Price. Comp features via `tenant_entitlement`.

### 14.1 Stripe

- Stripe Checkout for first subscription
- Customer Portal for card update and cancel
- Webhooks: `checkout.session.completed`, `invoice.paid`, `invoice.payment_failed`, `customer.subscription.updated/deleted`
- Map Stripe customer → tenant
- On payment failed: grace 7 days, then `tenant.status = READ_ONLY` (can export last run, cannot start a new one)
- Annual plan as a separate Price

### 14.2 Entitlements

A middleware checks plan limits: location count, employee count, which rule packs are enabled, audit-pack add-on.

Do not honor a Network feature on a Group plan even for “just this once” in code. Use a `tenant_entitlement` table so you can comp a feature for a design partner without forking plans.

### 14.3 Invoices your accountant needs

You sell software subscriptions. Collect sales tax via Stripe Tax when you have nexus. Talk to a CPA in month 1, not month 12.

---

## 15. Step 15 — Security, privacy, and “we won’t get you sued”

Controls live in `docs/SECURITY.md`: SSN/bank CSV reject, login/import rate limits, security headers, 2 MB upload cap. No PHI. No JWTs in logs.

### 15.1 Data you store

Staff names, emails, wages, hours, locations. Sensitive. Treat it like HR data.

Do **not** store SSN, bank accounts, or driver’s licenses in v1. If a clock CSV contains SSN, strip and reject the column.

### 15.2 Controls

- TLS everywhere
- Encryption at rest (disk / managed Postgres)
- Secrets in a manager (even if that is the host’s env vault)
- Backups: daily + before migrate; test restore once a month
- SQL injection: parameterized only
- File upload: size cap, type CSV/TSV, scan for zip bombs, store outside web root
- Rate limit login and import
- CORS locked to your frontend origin
- Security headers
- Dependency scanning (OWASP / GitHub Dependabot) on a schedule

### 15.3 Logging

Log tenant, actor, action, requestId. Never log punch payloads with names at INFO in prod if you can log ids only. Never log JWTs or Stripe secrets.

### 15.4 BAA / HIPAA

Default: you are not a HIPAA business associate because you do not handle PHI. If a large group demands a BAA anyway, have a lawyer review a narrow BAA that states no PHI will be submitted. Refuse EHR integrations that pull patient data.

### 15.5 Penetration reality

Before you have 10 paying tenants, pay for a lightweight pentest or at least run OWASP ZAP against staging. Put the date in your security page; buyers will ask.

---

## 16. Step 16 — Testing strategy (the engine is guilty until proven innocent)

Forever fixtures: `ca_missed_meal.csv`, `ca_nine_hour_day.csv`, `flsa_bonus_trueup.csv`, `dual_rate_hygienist.csv`, `cross_midnight_urgent_care.csv`. Playwright happy path: `frontend/e2e/payroll-happy-path.spec.ts`. Stripe signature rejection: `StripeSignatureTest`.

### 16.1 Unit tests (`engine`)

- One test class per rule
- Golden vectors from DOL and from CA DLSE examples where public
- Clinic scenarios: dual rate, bonus true-up, missed meal with attestation conflict
- Aim: engine coverage > 90%

### 16.2 Integration tests (`app`)

Testcontainers Postgres:

- Tenant isolation
- Import → calculate → approve → export
- Idempotent import
- Unlock audit trail
- Stripe webhook signature rejection (fixtures)

### 16.3 UI tests (small)

Playwright: login, import fixture CSV, see a known meal premium of $X.XX, approve, download CSV, assert bytes.

One end-to-end happy path is worth more than 50 shallow tests.

### 16.4 Fixtures you must keep forever

- `ca_missed_meal.csv`
- `ca_nine_hour_day.csv`
- `flsa_bonus_trueup.csv`
- `dual_rate_hygienist.csv`
- `cross_midnight_urgent_care.csv`

When a real customer finds a bug, add their de-identified period as a fixture named after the ticket. This becomes your moat.

---

## 17. Step 17 — Deploy, environments, and runbooks

`local` / `staging` / `prod` profiles. Compose: Postgres 16 + Redis 7. Runbooks in `runbooks/`. Observability: `/actuator/health`, structured logs, optional Sentry DSN. No SOC2 claim in year one.

### 17.1 Environments

- `local` — docker compose
- `staging` — real Stripe test mode, copy of demo tenant, you break this freely
- `prod` — paid tenants only

### 17.2 First deploy topology (cheap and honest)

- One app container
- Managed Postgres
- Managed Redis
- Object storage for CSVs, PDFs, snapshots
- SMTP (Postmark/SES) for invites and “payroll approved” mail
- Offsite backups

When a customer requires SOC2, you will rebuild this. Do not pretend you have SOC2 in year one. Say “SOC2 in progress” only if it is actually in progress.

### 17.3 Observability

- Structured logs
- Error tracker (Sentry)
- Uptime ping on `/actuator/health`
- A dashboard: pay runs per day, exception counts, failed webhooks

### 17.4 Runbooks (plain markdown in the repo later)

- Restore backup
- Rotate JWT secret
- Freeze a tenant (legal request)
- Replay a pay run from snapshot
- Stripe webhook replay

If you cannot replay a pay run from snapshot, you are not ready for a paying CA clinic.

---

## 18. Step 18 — Make it sellable: packaging, not more features

Marketing one-pager: `marketing/index.html`. Shadow CSV diff: `ShadowCsvDiff`. Clickwrap TOS on signup. Legal outlines in `legal/`.

### 18.1 Marketing site (one page is enough)

Sections:

1. Headline: “Stop fixing overtime in Excel the day before payday.”
2. Sub: “AegisPay checks every punch against wage-and-hour rules for dental, PT, urgent care, and vet groups — then sends a clean file to Gusto.”
3. Dollar story: missed meal premiums and daily OT the owner did not know they owed
4. How it works: Import → Exceptions → Approve → Export
5. Who it’s for / not for
6. Pricing
7. Disclaimer
8. Book a 20-minute payroll autopsy (Calendly)

Use real numbers from your concierge audits (anonymized): “In 14 California dental pay periods we reviewed, 11 needed meal premiums the payroll system never booked.”

### 18.2 Sales motion (you are the salesperson)

1. 20-minute call, share screen, they send a de-identified export
2. You run it in staging that night
3. You return a 2-page autopsy PDF
4. Pilot 14 days on next live payroll in parallel with Excel (shadow mode)
5. If the register matches or they prefer your register, they pay
6. You do white-glove onboarding for the first 15 customers yourself

Do not buy ads until 10 customers. Talk to office managers. Join groups. Ask CPAs for introductions. A CPA with 8 clinic clients is worth more than a landing-page rewrite.

### 18.3 Shadow mode (critical for trust)

During the pilot, they still run Excel/Gusto as today. AegisPay produces a comparison report: “Your Excel is $312 lower because CA daily OT on Thursday was not applied to Maria.” That comparison report closes the deal.

Build the comparison as a CSV diff, not a machine-learning toy.

### 18.4 Contracts

- Monthly, cancel anytime after first 60 days (or annual)
- Data export: they can always download punches and runs
- On cancel: 30 days read-only, then hard delete per policy
- DPA / privacy policy
- Clickwrap TOS on signup

Have a lawyer spend 4–8 hours on TOS + disclaimer. Cheaper than one angry employer.

### 18.5 Support you can survive alone

- In-app “Explain this line”
- Email, 1 business day
- Paid onboarding call ($0 for Network, $199 for Group — or include it to close)
- A changelog of rule-pack updates (“CA minimum wage changed on Jan 1 — pack US-CA 2026.1 published”)

Rule-pack updates are a retention feature. Email tenants when the law changes. That email reminds them why they pay you.

---

## 19. Step 19 — Implementation sequence (calendar for a solo Java developer)

Tracked in `docs/CALENDAR.md`. The codebase already follows this order; do not skip the engine tests for a prettier dashboard.

This is the order of work. Do not skip ahead to a pretty dashboard.

### Weeks 1–2 — Foundation

1. Finish Step 1 validation if not done
2. Gradle multi-module, Docker Compose, Flyway V1–V6
3. Tenant + auth + RBAC + TenantFilter tests
4. Harbor Dental seed

### Weeks 3–5 — Engine

5. FLSA pack + DOL golden tests
6. CA pack + meal/rest/daily OT tests
7. Dual rate + bonus true-up
8. Explanation objects

### Weeks 6–7 — Time + payroll API

9. CSV import + pairing
10. Pay period + calculate endpoint
11. Exception generation
12. Approve + lock + Gusto CSV

### Weeks 8–10 — UI

13. Wizard
14. Import + queue + register
15. People/rates
16. Owner home
17. PDF audit pack

### Week 11 — Money and mail

18. Stripe
19. Invites and “run ready” emails
20. Staging deploy

### Week 12 — First shadow payroll

21. Real customer file
22. Fix engine bugs (there will be some)
23. Add fixtures from production surprises
24. Convert to paid

If you are slower, cut the PDF and the owner dashboard, not the engine tests.

---

## 20. Step 20 — Definition of “ready to sell”

Living checklist: `docs/READY_TO_SELL.md`. Unchecked boxes (staging restore, E&O, a real clinic shadow file) still block taking a card.

You may take a credit card when all of the following are true:

- [ ] Engine reproduces DOL Fact Sheet overtime examples
- [ ] Engine reproduces at least 10 CA scenarios you have written down in `engine` tests
- [ ] Tenant isolation tests pass
- [ ] A stranger can complete the wizard and import a CSV without you in the call (almost — first 5 customers you will still join)
- [ ] Gusto CSV imports in a Gusto demo account without manual column fixes
- [ ] Pay run snapshot can recompute identical totals
- [ ] Stripe subscription gates the product
- [ ] Backups restored once on staging
- [ ] TOS, privacy policy, and disclaimer are live
- [ ] E&O insurance bound, or you have a written note that you are binding it before CA tenants
- [ ] You have done one paid or unpaid shadow payroll with a real clinic file
- [ ] You can explain every dollar on the register in one sentence each

If any box is unchecked, you can demo, you cannot honestly sell.

---

## 21. What to build after it sells (only when a customer pays for it)

Backlog: `docs/AFTER_IT_SELLS.md`. Do not start employee mobile punch until three live payrolls.

Priority order once you have 5+ paying groups:

1. Employee mobile punch + geofence + attestation (so you can unplug Homebase later)
2. Second and third state packs sold as add-ons ($49/month/state)
3. QuickBooks labor journal export
4. Location labor-cost vs production report (office managers love this; still not an EHR)
5. Credential expiration reminders (license tracking — adjacent, easy upsell)
6. Multi-legal-entity inside one tenant (real DSOs)
7. SOC 2 Type I
8. Official clock APIs (Deputy, Homebase)

Do not start #1 until the engine has survived three live payrolls.

---

## 22. Risks that kill this business if you ignore them

Register: `docs/RISKS.md`. CA math, tax-filing creep, PHI, engine forks, 50-state vanity, and silence in the market.

1. **Wrong CA math** — one public error and dental Facebook groups will bury you. Tests and counsel first.
2. **Becoming a full payroll company** too early — tax filing will consume a year.
3. **PHI creep** — one “can you just pull hours from our EHR” later you are in HIPAA land.
4. **Custom rules per clinic** — forks of the engine. Express customs as data (policies), not as tenant-specific code branches.
5. **Building 50 states** before 10 customers.
6. **No one to talk to** — if you will not do discovery calls, this product will not sell, no matter how clean the Spring architecture is.

---

## 23. Immediate next action when you are ready to write code

When you say to proceed, the first engineering tasks are:

1. Create `backend` (Gradle, `app` + `engine` modules) and `frontend` (Vite React) inside this folder
2. Docker Compose for Postgres 16 + Redis
3. Flyway migrations for the tables in Step 4
4. Tenant-aware Spring Security skeleton
5. Empty `calculate()` with the first FLSA golden test failing, then make it pass

Until then, this document is the entire project. Use it as the build order, the scope lock, and the sales story.
