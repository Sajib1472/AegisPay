# Contributing to AegisPay

Thank you for wanting to help. This is a **compliance product**, not a weekend todo app. A bad merge can underpay people or overpay them in a way that becomes evidence. Please read this before you write a line.

## Can others contribute?

Yes — on a **tight leash**.

AegisPay is a good project for contributors who:

- Will add a **failing golden test** before they change wage math
- Cite a statute, wage order, or DOL fact sheet in the PR
- Accept that engine changes may sit unmerged until counsel or the maintainer reviews them
- Are comfortable with Java 21, Spring, or React

It is a **poor** project for:

- “Good first issue” drive-by refactors
- Adding 50 states before 10 customers
- EHR / patient / SSN / bank-account features (rejected by design)
- Anyone who will not run `gradle :engine:test`

Until a `LICENSE` file exists, **all rights reserved**. Forking or using this in a competing paid product is not granted. Choose a license (often source-available or proprietary for this kind of SaaS) before you advertise “open source.”

## Where to help (highest value first)

1. **Forever fixtures** — de-identified punch CSVs named after a ticket, plus the expected dollars in a test. This is the moat.
2. **Clock CSV formats** — Homebase, Deputy, Time Clock Plus edge cases in `ClockCsvFormats`.
3. **UI** — exception queue, register, empty states, keyboard tables. Keep the **light paper theme**; office managers print screenshots.
4. **Docs and runbooks** — staging restore dated, security page, rule-pack changelog.
5. **Shadow CSV diff** — `ShadowCsvDiff` is how pilots close.

## Where not to freelance

- `backend/engine/src/main/java/com/aegispay/engine/calc/` without tests
- Changing `Money` / `Hours` scale or rounding
- Weakening tenant isolation (must stay **404**, never 403-with-data)
- Skipping the dual-control unlock
- Logging punch payloads with names at INFO

## PR checklist

- [ ] `gradle :engine:test` (always, if you touched Java)
- [ ] New rule behavior has a citation on the explanation object
- [ ] No SSN / bank / PHI columns in fixtures
- [ ] No secrets (`.env`, live Stripe keys, real clinic files with names)
- [ ] UI copy uses plain English for destructive actions (“Unlock approved payroll”)

## Engine changes

Open an issue first. Include:

1. Jurisdiction (`US-CA`, `US-FLSA`, city if any)
2. Public citation
3. Inputs (hours, rates, attestations)
4. Expected buckets and dollars
5. Whether this came from a **real de-identified payroll** (preferred)

Maintainer may ask you to wait for an employment-law review before merge. That is the product, not bureaucracy.

## Conduct

Be specific, cite sources, and assume the person reading the register is about to send money to staff. No harassment, no scraped clinic data, no “just this once” tenant-isolation bypass.
