# Freeze a tenant (legal request)

1. Set `tenant.status = 'FROZEN'` (or `READ_ONLY`) for that id.
2. Record an audit event `TENANT_FREEZE` with the ticket number.
3. They can still export the last approved run; they cannot calculate or import.
4. Do not delete punches while counsel is involved.
