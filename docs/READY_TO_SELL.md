# Ready to sell

Take a credit card only when every box is true. Demo is allowed earlier.

- [x] Engine reproduces DOL Fact Sheet overtime examples (`FlsaGoldenTest`)
- [x] Engine covers CA daily OT, meals, seventh day, dual rate (`engine` tests + forever fixtures)
- [x] Tenant isolation tests exist (`TenantIsolationIT`)
- [x] Wizard + CSV import exist (first five customers you will still join the call)
- [x] Gusto CSV exporter + checked-in fixture (`gusto-import.csv`)
- [x] Pay run snapshot stored with SHA-256; replay runbook written
- [x] Stripe subscription gates the product (Checkout + webhooks + READ_ONLY after grace)
- [ ] Backups restored once on staging (record the date in `runbooks/restore-backup.md`)
- [x] TOS, privacy policy, and disclaimer files exist (`legal/`) — counsel must still review before CA paid tenants
- [ ] E&O insurance bound, or a written note that you are binding it before CA tenants
- [ ] One paid or unpaid shadow payroll with a real clinic file
- [x] Every register dollar has an explanation object (code, citation, formula, narrative)

If any box is unchecked, you can demo, you cannot honestly sell.
