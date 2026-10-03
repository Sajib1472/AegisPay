# Restore backup

1. Stop the app.
2. Restore the Postgres dump onto staging first: `pg_restore --clean --if-exists -d aegispay backup.dump`
3. Point `AEGISPAY_OBJECT_DIR` at the matching object-storage snapshot (audit PDFs).
4. Start app with `SPRING_PROFILES_ACTIVE=staging`.
5. Hit `/actuator/health`. Run Harbor Dental login. Recalculate a locked run’s checksum — it must match `pay_run.audit_pdf_sha256`.
6. Only then restore prod.

Test restore once a month and write the date here: _not yet_.
