# Risks that kill this business if you ignore them

1. **Wrong CA math** — one public error and dental Facebook groups will bury you. Tests and counsel first. Golden fixtures in `engine/src/test/resources/fixtures/` are the moat.
2. **Becoming a full payroll company too early** — tax filing will consume a year. AegisPay sits in front of Gusto. It does not remit.
3. **PHI creep** — one “can you just pull hours from our EHR” later you are in HIPAA land. `ForbiddenHrColumnGuard` rejects SSN; refuse patient data.
4. **Custom rules per clinic** — forks of the engine. Express customs as tenant policy data, not tenant-specific code branches.
5. **Building 50 states before 10 customers.** CA + FLSA in v1. Second state is a Network add-on.
6. **No one to talk to** — if you will not do discovery calls, this product will not sell, no matter how clean the Spring architecture is.

See also `docs/READY_TO_SELL.md` and `docs/SECURITY.md`.
