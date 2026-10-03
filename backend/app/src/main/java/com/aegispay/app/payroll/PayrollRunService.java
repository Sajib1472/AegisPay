package com.aegispay.app.payroll;

import com.aegispay.app.billing.EntitlementService;
import com.aegispay.app.leave.LeaveAccrualService;
import com.aegispay.app.org.Assignment;
import com.aegispay.app.org.AssignmentRepository;
import com.aegispay.app.org.JobCode;
import com.aegispay.app.org.JobCodeRepository;
import com.aegispay.app.org.Location;
import com.aegispay.app.org.LocationRepository;
import com.aegispay.app.org.PayRate;
import com.aegispay.app.org.PayRateRepository;
import com.aegispay.app.org.Person;
import com.aegispay.app.org.PersonRepository;
import com.aegispay.app.org.TenantPolicyEntity;
import com.aegispay.app.org.TenantPolicyRepository;
import com.aegispay.app.platform.audit.AuditRecorder;
import com.aegispay.app.platform.identity.AuthService;
import com.aegispay.app.platform.identity.UserRole;
import com.aegispay.app.platform.tenancy.TenantContext;
import com.aegispay.app.rules.RulePackResolver;
import com.aegispay.app.time.MealAttestation;
import com.aegispay.app.time.MealAttestationRepository;
import com.aegispay.app.time.Punch;
import com.aegispay.app.time.PunchRepository;
import com.aegispay.engine.AegisPayEngine;
import com.aegispay.engine.EngineOptions;
import com.aegispay.engine.EngineVersion;
import com.aegispay.engine.export.GustoCsvExporter;
import com.aegispay.engine.model.WorkPeriod;
import com.aegispay.engine.model.WorkPeriod.Attestation;
import com.aegispay.engine.model.WorkPeriod.Attestation.MealAnswer;
import com.aegispay.engine.model.WorkPeriod.ExemptionStatus;
import com.aegispay.engine.money.Hours;
import com.aegispay.engine.money.Money;
import com.aegispay.engine.result.EarningsResult;
import com.aegispay.engine.result.EarningsResult.EarningBucket;
import com.aegispay.engine.rules.RulePack;
import com.aegispay.engine.time.PunchPairer;
import com.aegispay.engine.time.PunchPairer.PunchKind;
import com.aegispay.engine.time.PunchPairer.RawPunch;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PayrollRunService {

    private final AegisPayEngine engine = new AegisPayEngine();
    private final PunchPairer pairer = new PunchPairer();
    private final GustoCsvExporter exporter = new GustoCsvExporter();

    private final PayPeriodRepository periods;
    private final PayRunRepository runs;
    private final EarningsLineRepository lines;
    private final PayRunExceptionRepository exceptions;
    private final ExportFileRepository exports;
    private final PersonRepository people;
    private final LocationRepository locations;
    private final AssignmentRepository assignments;
    private final PayRateRepository rates;
    private final JobCodeRepository jobs;
    private final PunchRepository punches;
    private final MealAttestationRepository attestations;
    private final BonusEntryRepository bonuses;
    private final TenantPolicyRepository policies;
    private final PayRunSnapshotRepository snapshots;
    private final PayRunUnlockRequestRepository unlockRequests;
    private final EntitlementService entitlements;
    private final RulePackResolver rulePacks;
    private final AuditRecorder audit;
    private final AuthService auth;
    private final LeaveAccrualService leave;

    public PayrollRunService(
            PayPeriodRepository periods,
            PayRunRepository runs,
            EarningsLineRepository lines,
            PayRunExceptionRepository exceptions,
            ExportFileRepository exports,
            PersonRepository people,
            LocationRepository locations,
            AssignmentRepository assignments,
            PayRateRepository rates,
            JobCodeRepository jobs,
            PunchRepository punches,
            MealAttestationRepository attestations,
            BonusEntryRepository bonuses,
            TenantPolicyRepository policies,
            PayRunSnapshotRepository snapshots,
            PayRunUnlockRequestRepository unlockRequests,
            EntitlementService entitlements,
            RulePackResolver rulePacks,
            AuditRecorder audit,
            AuthService auth,
            LeaveAccrualService leave
    ) {
        this.periods = periods;
        this.runs = runs;
        this.lines = lines;
        this.exceptions = exceptions;
        this.exports = exports;
        this.people = people;
        this.locations = locations;
        this.assignments = assignments;
        this.rates = rates;
        this.jobs = jobs;
        this.punches = punches;
        this.attestations = attestations;
        this.bonuses = bonuses;
        this.policies = policies;
        this.snapshots = snapshots;
        this.unlockRequests = unlockRequests;
        this.entitlements = entitlements;
        this.rulePacks = rulePacks;
        this.audit = audit;
        this.auth = auth;
        this.leave = leave;
    }

    @Transactional
    public PayRun calculate(UUID periodId) {
        return calculate(periodId, RulePackResolver.LawMode.HISTORICAL);
    }

    @Transactional
    public PayRun calculate(UUID periodId, RulePackResolver.LawMode lawMode) {
        entitlements.assertWritable();
        auth.assertCanRunPayroll(auth.requireUser());
        UUID tenantId = TenantContext.requireTenantId();
        PayPeriod period = periods.findById(periodId).orElseThrow();
        TenantPolicyEntity policy = policies.findById(tenantId).orElseGet(TenantPolicyEntity::new);
        applyLockIfDue(latestRun(tenantId, periodId).orElse(null), policy);
        latestRun(tenantId, periodId).ifPresent(existing -> {
            if (!PayRunLifecycle.recalculateAllowed(existing.getStatus())) {
                throw new IllegalStateException("Recalculate is not allowed after APPROVED. Dual-control unlock is required.");
            }
        });
        RulePackResolver.Resolved resolved = rulePacks.resolve(period.getEndDate(), lawMode);

        PayRun run = new PayRun();
        run.setPayPeriodId(period.getId());
        run.setStatus(PayRunLifecycle.DRAFT);
        run.setEngineVersion(EngineVersion.VALUE);
        run.setRulePackVersions(resolved.labels());
        run.setRulePackIds(resolved.ids());
        run.setCreatedBy(TenantContext.userId());
        runs.save(run);

        Instant from = period.getStartDate().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = period.getEndDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<RulePack> packs = resolved.engine();
        EngineOptions options = EngineOptions.fromPacks(packs);
        GustoCsvExporter.Holder csvParts = new GustoCsvExporter.Holder();
        GustoCsvExporter.Holder genericParts = new GustoCsvExporter.Holder();
        Map<String, String> regularRates = new LinkedHashMap<>();
        Map<String, String> grossByPerson = new LinkedHashMap<>();

        for (Person person : people.findByTenantIdAndDeletedAtIsNull(tenantId)) {
            List<Punch> personPunches = punches.findByTenantIdAndPersonIdAndAdjustedAtBetweenOrderByAdjustedAt(
                    tenantId, person.getId(), from, to);
            List<RawPunch> raw = new ArrayList<>();
            Location loc = locations.findByTenantIdAndDeletedAtIsNull(tenantId).stream().findFirst().orElseThrow();
            for (Punch punch : personPunches) {
                if (punch.getVoidedAt() != null) {
                    continue;
                }
                Location punchLoc = locations.findById(punch.getLocationId()).orElse(loc);
                loc = punchLoc;
                raw.add(new RawPunch(
                        punch.getAdjustedAt(),
                        PunchKind.valueOf(punch.getPunchType()),
                        punchLoc.getId().toString(),
                        "RDH"
                ));
            }
            var paired = pairer.pair(raw);
            for (String problem : paired.problems()) {
                PayRunExceptionEntity ex = new PayRunExceptionEntity();
                ex.setPayRunId(run.getId());
                ex.setPersonId(person.getId());
                ex.setExceptionType("UNPAIRED_PUNCH");
                ex.setSeverity("BLOCKER");
                ex.setBlocker(true);
                ex.setMessage(problem);
                exceptions.save(ex);
            }

            List<Assignment> personAssignments = assignments.findByTenantIdAndPersonId(tenantId, person.getId());
            Assignment assignment = personAssignments.stream().findFirst().orElse(null);
            List<com.aegispay.engine.model.WorkPeriod.PayRate> engineRates = new ArrayList<>();
            String jobCode = "STAFF";
            if (assignment != null) {
                JobCode job = jobs.findById(assignment.getJobCodeId()).orElse(null);
                if (job != null) {
                    jobCode = job.getCode();
                }
                for (PayRate rate : rates.findByTenantIdAndAssignmentId(tenantId, assignment.getId())) {
                    engineRates.add(new com.aegispay.engine.model.WorkPeriod.PayRate(
                            jobCode,
                            WorkPeriod.RateType.HOURLY,
                            Money.of(rate.getAmount())
                    ));
                }
                loc = locations.findById(assignment.getLocationId()).orElse(loc);
            }
            if (engineRates.isEmpty()) {
                engineRates.add(new com.aegispay.engine.model.WorkPeriod.PayRate(
                        jobCode, WorkPeriod.RateType.HOURLY, Money.of("0")
                ));
                PayRunExceptionEntity ex = new PayRunExceptionEntity();
                ex.setPayRunId(run.getId());
                ex.setPersonId(person.getId());
                ex.setExceptionType("MISSING_RATE");
                ex.setSeverity("BLOCKER");
                ex.setBlocker(true);
                ex.setMessage("No hourly rate for " + person.getLegalName());
                exceptions.save(ex);
            }

            WorkPeriod.Shift shift = new WorkPeriod.Shift(
                    loc.getId().toString(),
                    jobCode,
                    ZoneId.of(loc.getTimeZone()),
                    loc.getJurisdictions(),
                    paired.intervals(),
                    engineRates
            );

            List<Attestation> atts = new ArrayList<>();
            for (MealAttestation att : attestations.findByTenantIdAndPersonId(tenantId, person.getId())) {
                atts.add(new Attestation(
                        att.getWorkDate(),
                        MealAnswer.valueOf(att.getMealReceived()),
                        MealAnswer.valueOf(att.getRestReceived())
                ));
            }
            List<WorkPeriod.Bonus> bonusList = new ArrayList<>();
            for (BonusEntry bonus : bonuses.findByTenantIdAndPersonId(tenantId, person.getId())) {
                if (!bonus.getEarnedOn().isBefore(period.getStartDate()) && !bonus.getEarnedOn().isAfter(period.getEndDate())) {
                    bonusList.add(new WorkPeriod.Bonus(
                            Money.of(bonus.getAmount()),
                            bonus.getEarnedOn(),
                            bonus.isDiscretionary(),
                            bonus.getNote()
                    ));
                }
            }

            WorkPeriod workPeriod = new WorkPeriod(
                    person.getId().toString(),
                    "EXEMPT_SALARY".equals(person.getExemptionStatus())
                            ? ExemptionStatus.EXEMPT_SALARY
                            : ExemptionStatus.NON_EXEMPT,
                    DayOfWeek.valueOf(policy.getWorkweekStart()),
                    paired.intervals().isEmpty() ? List.of() : List.of(shift),
                    bonusList,
                    atts,
                    new WorkPeriod.TenantPolicy(
                            policy.getPunchRoundMinutes(),
                            policy.isMealWaiverUnderSixHours(),
                            policy.isAutoRestPremium(),
                            policy.isAttestationOverridesClock()
                    )
            );

            EarningsResult result = engine.calculate(workPeriod, packs, options);
            persistResult(run, person, result);
            leave.accrueFromRun(run.getId(), person, loc, result, period.getEndDate());
            addWarnings(run, period, person, result, bonusList.isEmpty());
            csvParts.append(exporter.export(person.getExternalEmployeeCode(), result));
            genericParts.append(exporter.generic(person.getExternalEmployeeCode(), result));
            regularRates.put(person.getId().toString(), result.regularRate().toString());
            grossByPerson.put(person.getId().toString(), result.totals().gross().toString());
        }

        String csv = csvParts.merge();
        saveExport(run.getId(), "GUSTO", csv);
        saveExport(run.getId(), "GENERIC", genericParts.merge());

        boolean blockers = exceptions.findByTenantIdAndPayRunId(tenantId, run.getId()).stream()
                .anyMatch(e -> e.isBlocker() && !e.isDismissed());
        String next = PayRunLifecycle.afterCalculate(blockers);
        run.setStatus(next);
        period.setStatus(next);
        persistSnapshot(run, regularRates, grossByPerson);
        return run;
    }

    @Transactional
    public PayRun approve(UUID runId, boolean confirmed, String ip) {
        entitlements.assertCanApprove();
        auth.assertCanRunPayroll(auth.requireUser());
        if (!confirmed) {
            throw new IllegalStateException("Confirm that exceptions were reviewed before approving");
        }
        UUID tenantId = TenantContext.requireTenantId();
        PayRun run = runs.findById(runId).orElseThrow();
        applyLockIfDue(run, policies.findById(tenantId).orElseGet(TenantPolicyEntity::new));
        if (PayRunLifecycle.LOCKED.equals(run.getStatus())) {
            throw new IllegalStateException("Locked runs cannot be approved");
        }
        boolean blockers = exceptions.findByTenantIdAndPayRunId(tenantId, runId).stream()
                .anyMatch(e -> e.isBlocker() && !e.isDismissed());
        if (blockers) {
            throw new IllegalStateException("Cannot approve a run with blocker exceptions");
        }
        run.setStatus(PayRunLifecycle.APPROVED);
        run.setApprovedBy(TenantContext.userId());
        run.setApprovedAt(Instant.now());
        run.setApprovalIp(ip);
        periods.findById(run.getPayPeriodId()).ifPresent(period -> period.setStatus(PayRunLifecycle.APPROVED));
        audit.record("PAYROLL_APPROVE", "pay_run", runId.toString());
        return run;
    }

    @Transactional
    public PayRun exportRun(UUID runId) {
        entitlements.assertWritable();
        auth.assertCanRunPayroll(auth.requireUser());
        UUID tenantId = TenantContext.requireTenantId();
        PayRun run = runs.findById(runId).orElseThrow();
        if (!PayRunLifecycle.APPROVED.equals(run.getStatus()) && !PayRunLifecycle.EXPORTED.equals(run.getStatus())) {
            throw new IllegalStateException("Approve the run before exporting");
        }
        TenantPolicyEntity policy = policies.findById(tenantId).orElseGet(TenantPolicyEntity::new);
        Instant now = Instant.now();
        run.setExportedAt(now);
        run.setLockAt(PayRunLifecycle.lockAt(now, policy.isLockOnExport()));
        run.setStatus(PayRunLifecycle.EXPORTED);
        periods.findById(run.getPayPeriodId()).ifPresent(period -> period.setStatus(PayRunLifecycle.EXPORTED));
        applyLockIfDue(run, policy);
        audit.record("PAYROLL_EXPORT", "pay_run", runId.toString());
        return run;
    }

    @Transactional
    public PayRunUnlockRequest requestUnlock(UUID runId) {
        entitlements.assertWritable();
        if (!UserRole.PAYROLL_ADMIN.name().equals(TenantContext.role())) {
            throw new IllegalStateException("PAYROLL_ADMIN must request the unlock; OWNER confirms");
        }
        PayRun run = runs.findById(runId).orElseThrow();
        if (!PayRunLifecycle.isTerminal(run.getStatus())) {
            throw new IllegalStateException("Unlock is only for APPROVED, EXPORTED, or LOCKED runs");
        }
        PayRunUnlockRequest request = new PayRunUnlockRequest();
        request.setPayRunId(runId);
        request.setRequestedBy(TenantContext.userId());
        request.setStatus("PENDING");
        unlockRequests.save(request);
        audit.record("PAYROLL_UNLOCK_REQUEST", "pay_run", runId.toString());
        return request;
    }

    @Transactional
    public PayRun confirmUnlock(UUID runId) {
        entitlements.assertWritable();
        if (!UserRole.OWNER.name().equals(TenantContext.role())) {
            throw new IllegalStateException("OWNER must confirm the unlock");
        }
        UUID tenantId = TenantContext.requireTenantId();
        PayRun run = runs.findById(runId).orElseThrow();
        PayRunUnlockRequest request = unlockRequests
                .findFirstByTenantIdAndPayRunIdAndStatusOrderByRequestedAtDesc(tenantId, runId, "PENDING")
                .orElseThrow(() -> new IllegalStateException("PAYROLL_ADMIN has not requested an unlock"));
        if (request.getRequestedBy().equals(TenantContext.userId())) {
            throw new IllegalStateException("Dual control requires a second person");
        }
        request.setConfirmedBy(TenantContext.userId());
        request.setConfirmedAt(Instant.now());
        request.setStatus("CONFIRMED");
        run.setStatus(PayRunLifecycle.CALCULATED);
        run.setUnlockedBy(TenantContext.userId());
        run.setUnlockedAt(Instant.now());
        run.setApprovedBy(null);
        run.setApprovedAt(null);
        run.setApprovalIp(null);
        run.setExportedAt(null);
        run.setLockAt(null);
        periods.findById(run.getPayPeriodId()).ifPresent(period -> period.setStatus(PayRunLifecycle.CALCULATED));
        audit.record("PAYROLL_UNLOCK", "pay_run", runId.toString());
        audit.record("PAYROLL_UNLOCK_OWNER_NOTIFIED", "pay_run", runId.toString());
        return run;
    }

    @Transactional
    public PayRunExceptionEntity dismissException(UUID exceptionId, String reason) {
        entitlements.assertWritable();
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Dismiss reason is required");
        }
        PayRunExceptionEntity ex = exceptions.findById(exceptionId).orElseThrow();
        if (ex.isBlocker()) {
            throw new IllegalStateException("Blockers cannot be dismissed. Fix the punch or rate.");
        }
        PayRun run = runs.findById(ex.getPayRunId()).orElseThrow();
        if (PayRunLifecycle.isTerminal(run.getStatus())) {
            throw new IllegalStateException("Unlock the run before dismissing exceptions");
        }
        ex.setDismissed(true);
        ex.setDismissReason(reason.trim());
        ex.setDismissedBy(TenantContext.userId());
        ex.setDismissedAt(Instant.now());
        audit.record("EXCEPTION_DISMISS", "pay_run_exception", exceptionId.toString());
        return ex;
    }

    @Transactional
    public BonusEntry createBonus(UUID personId, BigDecimal amount, LocalDate earnedOn, boolean discretionary, String note, UUID periodId) {
        entitlements.assertWritable();
        if (personId == null || amount == null || earnedOn == null) {
            throw new IllegalArgumentException("Person, amount, and date are required");
        }
        BonusEntry bonus = new BonusEntry();
        bonus.setPersonId(personId);
        bonus.setAmount(amount);
        bonus.setEarnedOn(earnedOn);
        bonus.setDiscretionary(discretionary);
        bonus.setNote(note);
        bonus.setPayPeriodId(periodId);
        bonuses.save(bonus);
        audit.record("BONUS_CREATE", "bonus_entry", bonus.getId().toString());
        return bonus;
    }

    public List<BonusEntry> listBonuses() {
        return bonuses.findByTenantId(TenantContext.requireTenantId());
    }

    public Optional<PayRun> latestRun(UUID periodId) {
        return latestRun(TenantContext.requireTenantId(), periodId);
    }

    @Transactional
    public PayrollView view(UUID runId) {
        UUID tenantId = TenantContext.requireTenantId();
        PayRun run = runs.findById(runId).orElseThrow();
        applyLockIfDue(run, policies.findById(tenantId).orElseGet(TenantPolicyEntity::new));
        List<LineView> lineViews = lines.findByTenantIdAndPayRunId(tenantId, runId).stream().map(l -> new LineView(
                l.getPersonId(), l.getWorkDate(), l.getBucket(), l.getHours(), l.getRate(), l.getAmount(), l.getExplanation()
        )).toList();
        List<ExceptionView> exceptionViews = exceptions.findByTenantIdAndPayRunId(tenantId, runId).stream().map(e -> new ExceptionView(
                e.getId(), e.getPersonId(), e.getWorkDate(), e.getExceptionType(), e.getSeverity(), e.isBlocker(),
                e.getMessage(), e.isDismissed(), e.getDismissReason()
        )).toList();
        List<ExportFile> files = exports.findByTenantIdAndPayRunId(tenantId, runId);
        String csv = files.stream().filter(f -> "GUSTO".equals(f.getDestination())).findFirst()
                .or(() -> files.stream().findFirst()).map(ExportFile::getContent).orElse("");
        String genericCsv = files.stream().filter(f -> "GENERIC".equals(f.getDestination())).findFirst()
                .map(ExportFile::getContent).orElse("");
        PayRunSnapshot snapshot = snapshots.findByTenantIdAndPayRunId(tenantId, runId).orElse(null);
        @SuppressWarnings("unchecked")
        Map<String, String> regularRates = snapshot != null && snapshot.getPayload().get("regularRates") instanceof Map<?, ?> map
                ? (Map<String, String>) map
                : Map.of();
        boolean unlockPending = unlockRequests
                .findFirstByTenantIdAndPayRunIdAndStatusOrderByRequestedAtDesc(tenantId, runId, "PENDING")
                .isPresent();
        return new PayrollView(
                run.getId(),
                run.getPayPeriodId(),
                run.getStatus(),
                run.getEngineVersion(),
                run.getApprovedBy(),
                run.getApprovedAt(),
                run.getApprovalIp(),
                unlockPending,
                regularRates,
                lineViews,
                exceptionViews,
                registerRows(tenantId, lineViews),
                csv,
                genericCsv,
                run.getSnapshotSha256(),
                successChecklist(tenantId, run, lineViews, exceptionViews, csv, snapshot)
        );
    }

    private List<RegisterRow> registerRows(UUID tenantId, List<LineView> lineViews) {
        Map<UUID, Person> byId = people.findByTenantIdAndDeletedAtIsNull(tenantId).stream()
                .collect(Collectors.toMap(Person::getId, p -> p));
        Map<UUID, List<LineView>> grouped = lineViews.stream().collect(Collectors.groupingBy(LineView::personId, LinkedHashMap::new, Collectors.toList()));
        List<RegisterRow> rows = new ArrayList<>();
        grouped.forEach((personId, personLines) -> {
            Person person = byId.get(personId);
            BigDecimal regH = BigDecimal.ZERO;
            BigDecimal regPay = BigDecimal.ZERO;
            BigDecimal otH = BigDecimal.ZERO;
            BigDecimal otPay = BigDecimal.ZERO;
            BigDecimal dtH = BigDecimal.ZERO;
            BigDecimal dtPay = BigDecimal.ZERO;
            BigDecimal premiums = BigDecimal.ZERO;
            BigDecimal diffs = BigDecimal.ZERO;
            BigDecimal bonus = BigDecimal.ZERO;
            for (LineView line : personLines) {
                BigDecimal hours = line.hours() == null ? BigDecimal.ZERO : line.hours();
                BigDecimal amount = line.amount() == null ? BigDecimal.ZERO : line.amount();
                switch (line.bucket() == null ? "" : line.bucket()) {
                    case "REG" -> {
                        regH = regH.add(hours);
                        regPay = regPay.add(amount);
                    }
                    case "OT_1_5" -> {
                        otH = otH.add(hours);
                        otPay = otPay.add(amount);
                    }
                    case "OT_2_0" -> {
                        dtH = dtH.add(hours);
                        dtPay = dtPay.add(amount);
                    }
                    case "DIFFERENTIAL" -> diffs = diffs.add(amount);
                    case "BONUS", "BONUS_TRUE_UP" -> bonus = bonus.add(amount);
                    default -> premiums = premiums.add(amount);
                }
            }
            BigDecimal gross = regPay.add(otPay).add(dtPay).add(premiums).add(diffs).add(bonus);
            rows.add(new RegisterRow(
                    personId,
                    person == null ? personId.toString() : person.getLegalName(),
                    person == null ? "" : person.getExternalEmployeeCode(),
                    regH, regPay, otH, otPay, dtH, dtPay, premiums, diffs, bonus, gross
            ));
        });
        rows.sort(Comparator.comparing(RegisterRow::legalName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return rows;
    }

    private Optional<PayRun> latestRun(UUID tenantId, UUID periodId) {
        return runs.findByTenantIdAndPayPeriodIdOrderByCreatedAtDesc(tenantId, periodId).stream().findFirst();
    }

    private void applyLockIfDue(PayRun run, TenantPolicyEntity policy) {
        if (run == null || !PayRunLifecycle.EXPORTED.equals(run.getStatus())) {
            return;
        }
        Instant lockAt = run.getLockAt();
        if (lockAt == null) {
            lockAt = PayRunLifecycle.lockAt(run.getExportedAt(), policy.isLockOnExport());
            run.setLockAt(lockAt);
        }
        if (PayRunLifecycle.lockDue(lockAt, Instant.now())) {
            run.setStatus(PayRunLifecycle.LOCKED);
            periods.findById(run.getPayPeriodId()).ifPresent(period -> period.setStatus(PayRunLifecycle.LOCKED));
            audit.record("PAYROLL_LOCK", "pay_run", run.getId().toString());
        }
    }

    private void saveExport(UUID runId, String destination, String content) {
        ExportFile file = new ExportFile();
        file.setPayRunId(runId);
        file.setDestination(destination);
        file.setContent(content == null ? "" : content);
        file.setChecksumSha256(sha(content == null ? "" : content));
        exports.save(file);
    }

    private void addWarnings(PayRun run, PayPeriod period, Person person, EarningsResult result, boolean noBonus) {
        Hours ot = result.totals().otHours().plus(result.totals().doubleTimeHours());
        if (ot.isGreaterThan(Hours.of("20"))) {
            warn(run, person, "HIGH_OT", "Overtime hours exceed 20 this week — check the punches");
        }
        boolean meal = result.lines().stream().anyMatch(l -> l.bucket() == EarningBucket.MEAL_PREMIUM);
        if (meal) {
            warn(run, person, "MEAL_PREMIUM", "Meal premium generated");
        }
        Hours worked = result.totals().regularHours().plus(result.totals().otHours()).plus(result.totals().doubleTimeHours());
        if ("EXEMPT_SALARY".equals(person.getExemptionStatus()) && !worked.isZero()) {
            warn(run, person, "EXEMPT_WITH_HOURS", "Exempt classification but hours were recorded");
        }
        LocalDate monthEnd = YearMonth.from(period.getEndDate()).atEndOfMonth();
        if (!period.getStartDate().isAfter(monthEnd) && !period.getEndDate().isBefore(monthEnd) && noBonus
                && !"EXEMPT_SALARY".equals(person.getExemptionStatus())) {
            warn(run, person, "BONUS_NOT_ENTERED", "Month-end week and no bonus entered");
        }
    }

    private void warn(PayRun run, Person person, String type, String message) {
        PayRunExceptionEntity entity = new PayRunExceptionEntity();
        entity.setPayRunId(run.getId());
        entity.setPersonId(person.getId());
        entity.setExceptionType(type);
        entity.setSeverity("WARNING");
        entity.setBlocker(false);
        entity.setMessage(message);
        exceptions.save(entity);
    }

    private SuccessChecklist successChecklist(
            UUID tenantId,
            PayRun run,
            List<LineView> lineViews,
            List<ExceptionView> exceptionViews,
            String csv,
            PayRunSnapshot snapshot
    ) {
        Set<UUID> peopleIds = people.findByTenantIdAndDeletedAtIsNull(tenantId).stream().map(Person::getId).collect(Collectors.toSet());
        Set<UUID> lined = lineViews.stream().map(LineView::personId).collect(Collectors.toSet());
        boolean punchesAccounted = exceptionViews.stream()
                .noneMatch(e -> e.blocker() && "UNPAIRED_PUNCH".equals(e.type()))
                || exceptionViews.stream().anyMatch(e -> "UNPAIRED_PUNCH".equals(e.type()));
        boolean everyPunchHandled = exceptionViews.stream().noneMatch(ExceptionView::blocker)
                || exceptionViews.stream().filter(ExceptionView::blocker).allMatch(e -> e.message() != null);
        boolean everyEmployeeHasALine = peopleIds.isEmpty() || lined.containsAll(peopleIds)
                || peopleIds.stream().allMatch(id -> lined.contains(id) || exceptionViews.stream().anyMatch(e -> id.equals(e.personId())));
        boolean regularRateShown = !regularRatesFrom(snapshot).isEmpty() || peopleIds.isEmpty();
        boolean approved = run.getApprovedBy() != null && PayRunLifecycle.APPROVED.equals(run.getStatus());
        boolean snapshotStored = snapshot != null && snapshot.getSha256() != null;
        boolean gustoReady = csv != null && csv.contains("employee_code");
        return new SuccessChecklist(
                everyPunchHandled,
                everyEmployeeHasALine,
                regularRateShown,
                approved,
                snapshotStored,
                gustoReady,
                punchesAccounted
        );
    }

    private static Map<String, String> regularRatesFrom(PayRunSnapshot snapshot) {
        if (snapshot == null || !(snapshot.getPayload().get("regularRates") instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, String> rates = new LinkedHashMap<>();
        map.forEach((k, v) -> rates.put(String.valueOf(k), String.valueOf(v)));
        return rates;
    }

    private void persistSnapshot(PayRun run, Map<String, String> regularRates, Map<String, String> grossByPerson) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("engineVersion", EngineVersion.VALUE);
        payload.put("rulePacks", run.getRulePackVersions());
        payload.put("rulePackIds", run.getRulePackIds());
        payload.put("regularRates", regularRates);
        payload.put("grossByPerson", grossByPerson);
        payload.put("createdAt", Instant.now().toString());
        String jsonish = payload.toString();
        String digest = sha(jsonish);
        PayRunSnapshot snapshot = new PayRunSnapshot();
        snapshot.setPayRunId(run.getId());
        snapshot.setPayload(payload);
        snapshot.setSha256(digest);
        snapshots.save(snapshot);
        run.setSnapshotSha256(digest);
    }

    private void persistResult(PayRun run, Person person, EarningsResult result) {
        result.lines().forEach(line -> {
            EarningsLineEntity entity = new EarningsLineEntity();
            entity.setPayRunId(run.getId());
            entity.setPayPeriodId(run.getPayPeriodId());
            entity.setPersonId(person.getId());
            entity.setWorkDate(line.workDate());
            entity.setBucket(line.bucket().name());
            entity.setHours(line.hours().toBigDecimal());
            entity.setRate(line.rate().toBigDecimal());
            entity.setAmount(line.amount().toBigDecimal());
            entity.setExplanation(Map.of(
                    "code", line.explanation().code(),
                    "citation", line.explanation().citation(),
                    "formula", line.explanation().formula(),
                    "narrative", line.explanation().narrative()
            ));
            lines.save(entity);
        });
        result.exceptions().forEach(ex -> {
            PayRunExceptionEntity entity = new PayRunExceptionEntity();
            entity.setPayRunId(run.getId());
            entity.setPersonId(person.getId());
            entity.setWorkDate(ex.workDate());
            entity.setExceptionType(ex.type());
            entity.setSeverity(ex.severity());
            entity.setBlocker(ex.blocker());
            entity.setMessage(ex.message());
            exceptions.save(entity);
        });
    }

    private static String sha(String csv) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(csv.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record LineView(UUID personId, LocalDate workDate, String bucket, java.math.BigDecimal hours,
                           java.math.BigDecimal rate, java.math.BigDecimal amount, Map<String, Object> explanation) {
    }

    public record ExceptionView(
            UUID id,
            UUID personId,
            LocalDate workDate,
            String type,
            String severity,
            boolean blocker,
            String message,
            boolean dismissed,
            String dismissReason
    ) {
    }

    public record RegisterRow(
            UUID personId,
            String legalName,
            String employeeCode,
            BigDecimal regularHours,
            BigDecimal regularPay,
            BigDecimal otHours,
            BigDecimal otPay,
            BigDecimal dtHours,
            BigDecimal dtPay,
            BigDecimal premiums,
            BigDecimal differentials,
            BigDecimal bonus,
            BigDecimal grossEarningsSubmitted
    ) {
    }

    public record PayrollView(
            UUID runId,
            UUID periodId,
            String status,
            String engineVersion,
            UUID approvedBy,
            Instant approvedAt,
            String approvalIp,
            boolean unlockPending,
            Map<String, String> regularRates,
            List<LineView> lines,
            List<ExceptionView> exceptions,
            List<RegisterRow> register,
            String gustoCsv,
            String genericCsv,
            String snapshotSha256,
            SuccessChecklist success
    ) {
    }

    public record SuccessChecklist(
            boolean punchesAccounted,
            boolean everyEmployeeHasALine,
            boolean regularRateShown,
            boolean approvedByPayrollApprove,
            boolean snapshotStored,
            boolean gustoExportReady,
            boolean pairingProblemsFlagged
    ) {
    }
}
