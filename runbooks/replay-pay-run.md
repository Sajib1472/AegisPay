# Replay a pay run from snapshot

If you cannot replay a pay run from snapshot, you are not ready for a paying CA clinic.

1. Locate `pay_run_snapshot.payload` and `sha256`.
2. Dual-control unlock if the run is APPROVED/LOCKED.
3. `POST /api/v1/pay-periods/{id}/runs` with the same `HISTORICAL` packs listed in the snapshot.
4. Compare new `grossByPerson` and `regularRates` to the snapshot JSON.
5. If they diverge, stop. That is an engine bug — add a de-identified fixture named after the ticket.

Locked audit PDFs must hash to `pay_run.audit_pdf_sha256`. If not, fail the release.
