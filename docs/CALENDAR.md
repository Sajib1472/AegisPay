# Implementation sequence (solo Java developer)

Do not skip ahead to a pretty dashboard.

## Weeks 1–2 — Foundation

Done in this repo: Gradle `app` + `engine`, Docker Compose, Flyway, tenant + auth + RBAC + TenantFilter tests, Harbor Dental seed.

## Weeks 3–5 — Engine

FLSA + CA packs, dual rate, bonus true-up, explanation objects (`engine` 0.2.0).

## Weeks 6–7 — Time + payroll API

CSV import + pairing, pay period lifecycle, exceptions, approve + lock + Gusto CSV.

## Weeks 8–10 — UI

Wizard, import + queue + register, people/rates, owner home, audit PDF.

## Week 11 — Money and mail

Stripe Checkout/webhooks. Invites. Staging profile.

## Week 12 — First shadow payroll

`POST /api/v1/shadow/compare`. Add fixtures from production surprises. Convert to paid.

If you are slower, cut the PDF and the owner dashboard, not the engine tests.
