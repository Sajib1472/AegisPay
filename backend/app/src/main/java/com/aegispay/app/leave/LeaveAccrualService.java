package com.aegispay.app.leave;

import com.aegispay.app.billing.EntitlementService;
import com.aegispay.app.org.Location;
import com.aegispay.app.org.Person;
import com.aegispay.app.platform.audit.AuditRecorder;
import com.aegispay.app.platform.tenancy.TenantContext;
import com.aegispay.engine.leave.LeaveAccrualCalculator;
import com.aegispay.engine.leave.LeaveAccrualCalculator.Policy;
import com.aegispay.engine.money.Hours;
import com.aegispay.engine.result.EarningsResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class LeaveAccrualService {

    private final LeaveLedgerRepository ledger;
    private final LeaveBalanceRepository balances;
    private final EntitlementService entitlements;
    private final AuditRecorder audit;

    public LeaveAccrualService(
            LeaveLedgerRepository ledger,
            LeaveBalanceRepository balances,
            EntitlementService entitlements,
            AuditRecorder audit
    ) {
        this.ledger = ledger;
        this.balances = balances;
        this.entitlements = entitlements;
        this.audit = audit;
    }

    @Transactional
    public void accrueFromRun(UUID payRunId, Person person, Location location, EarningsResult result, LocalDate asOf) {
        Hours worked = result.totals().regularHours()
                .plus(result.totals().otHours())
                .plus(result.totals().doubleTimeHours());
        UUID tenantId = TenantContext.requireTenantId();
        if (ledger.existsByTenantIdAndPayRunIdAndPersonIdAndEntryType(tenantId, payRunId, person.getId(), "ACCRUAL")) {
            return;
        }
        Policy policy = LeaveAccrualCalculator.resolve(location.getJurisdictions());
        Hours current = Hours.of(sumLedger(tenantId, person.getId()).toPlainString());
        Hours earned = LeaveAccrualCalculator.accrue(worked, current, policy);
        if (earned.isZero()) {
            rewriteBalance(person.getId(), policy.code(), current, asOf);
            return;
        }
        Hours after = current.plus(earned);
        post(person.getId(), payRunId, policy.code(), "ACCRUAL", earned.toBigDecimal(), after.toBigDecimal(), asOf,
                "1 hour per " + policy.hoursWorkedPerAccruedHour() + " hours worked");
        rewriteBalance(person.getId(), policy.code(), after, asOf);
    }

    @Transactional
    public LeaveLedgerEntry recordUsage(UUID personId, BigDecimal hours, LocalDate workDate, String note) {
        entitlements.assertWritable();
        if (hours == null || hours.signum() <= 0) {
            throw new IllegalArgumentException("Sick hours to pay must be positive");
        }
        UUID tenantId = TenantContext.requireTenantId();
        Hours current = Hours.of(sumLedger(tenantId, personId).toPlainString());
        Hours use = Hours.of(hours);
        if (use.isGreaterThan(current)) {
            throw new IllegalStateException("Not enough sick balance");
        }
        Hours after = current.minus(use);
        LeaveLedgerEntry row = post(personId, null, LeaveAccrualCalculator.CA_SICK, "USAGE",
                use.toBigDecimal().negate(), after.toBigDecimal(), workDate, note);
        rewriteBalance(personId, LeaveAccrualCalculator.CA_SICK, after, workDate);
        audit.record("LEAVE_USAGE", "leave_ledger", row.getId().toString());
        return row;
    }

    public List<LeaveBalance> list() {
        return balances.findByTenantId(TenantContext.requireTenantId());
    }

    public BigDecimal sickHoursToPay(UUID tenantId, UUID personId) {
        return ledger.findByTenantIdAndPersonIdOrderByCreatedAtAsc(tenantId, personId).stream()
                .filter(e -> "USAGE".equals(e.getEntryType()))
                .map(LeaveLedgerEntry::getHoursDelta)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LeaveLedgerEntry post(
            UUID personId,
            UUID payRunId,
            String policy,
            String type,
            BigDecimal delta,
            BigDecimal after,
            LocalDate workDate,
            String note
    ) {
        LeaveLedgerEntry row = new LeaveLedgerEntry();
        row.setPersonId(personId);
        row.setPayRunId(payRunId);
        row.setPolicyCode(policy);
        row.setEntryType(type);
        row.setHoursDelta(delta);
        row.setBalanceAfter(after);
        row.setWorkDate(workDate);
        row.setNote(note);
        return ledger.save(row);
    }

    private BigDecimal sumLedger(UUID tenantId, UUID personId) {
        return ledger.findByTenantIdAndPersonIdOrderByCreatedAtAsc(tenantId, personId).stream()
                .map(LeaveLedgerEntry::getHoursDelta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void rewriteBalance(UUID personId, String policy, Hours hours, LocalDate asOf) {
        UUID tenantId = TenantContext.requireTenantId();
        LeaveBalance row = balances.findByTenantIdAndPersonIdAndPolicyCode(tenantId, personId, policy)
                .orElseGet(LeaveBalance::new);
        row.setPersonId(personId);
        row.setPolicyCode(policy);
        row.setHours(hours.toBigDecimal());
        row.setAsOf(asOf);
        balances.save(row);
    }
}
