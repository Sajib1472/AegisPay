package com.aegispay.app.payroll;

import com.aegispay.app.billing.EntitlementService;
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
import com.aegispay.engine.money.Money;
import com.aegispay.engine.result.EarningsResult;
import com.aegispay.engine.rules.RulePack;
import com.aegispay.engine.time.PunchPairer;
import com.aegispay.engine.time.PunchPairer.PunchKind;
import com.aegispay.engine.time.PunchPairer.RawPunch;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final EntitlementService entitlements;
    private final RulePackResolver rulePacks;
    private final AuditRecorder audit;

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
            EntitlementService entitlements,
            RulePackResolver rulePacks,
            AuditRecorder audit
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
        this.entitlements = entitlements;
        this.rulePacks = rulePacks;
        this.audit = audit;
    }

    @Transactional
    public PayRun calculate(UUID periodId) {
        return calculate(periodId, RulePackResolver.LawMode.HISTORICAL);
    }

    @Transactional
    public PayRun calculate(UUID periodId, RulePackResolver.LawMode lawMode) {
        entitlements.assertWritable();
        UUID tenantId = TenantContext.requireTenantId();
        PayPeriod period = periods.findById(periodId).orElseThrow();
        TenantPolicyEntity policy = policies.findById(tenantId).orElseGet(TenantPolicyEntity::new);
        RulePackResolver.Resolved resolved = rulePacks.resolve(period.getEndDate(), lawMode);

        PayRun run = new PayRun();
        run.setPayPeriodId(period.getId());
        run.setStatus("CALCULATED");
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
        Map<String, String> regularRates = new LinkedHashMap<>();
        Map<String, String> grossByPerson = new LinkedHashMap<>();

        for (Person person : people.findByTenantId(tenantId)) {
            List<Punch> personPunches = punches.findByTenantIdAndPersonIdAndAdjustedAtBetweenOrderByAdjustedAt(
                    tenantId, person.getId(), from, to);
            List<RawPunch> raw = new ArrayList<>();
            Location loc = locations.findByTenantId(tenantId).stream().findFirst().orElseThrow();
            for (Punch punch : personPunches) {
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
            csvParts.append(exporter.export(person.getExternalEmployeeCode(), result));
            regularRates.put(person.getId().toString(), result.regularRate().toString());
            grossByPerson.put(person.getId().toString(), result.totals().gross().toString());
        }

        String csv = csvParts.merge();
        ExportFile file = new ExportFile();
        file.setPayRunId(run.getId());
        file.setDestination("GUSTO");
        file.setContent(csv);
        file.setChecksumSha256(sha(csv));
        exports.save(file);

        boolean blockers = exceptions.findByTenantIdAndPayRunId(tenantId, run.getId()).stream()
                .anyMatch(PayRunExceptionEntity::isBlocker);
        run.setStatus(blockers ? "EXCEPTIONS_PENDING" : "CALCULATED");
        persistSnapshot(run, regularRates, grossByPerson);
        return run;
    }

    @Transactional
    public PayRun approve(UUID runId) {
        entitlements.assertCanApprove();
        PayRun run = runs.findById(runId).orElseThrow();
        boolean blockers = exceptions.findByTenantIdAndPayRunId(TenantContext.requireTenantId(), runId).stream()
                .anyMatch(e -> e.isBlocker());
        if (blockers) {
            throw new IllegalStateException("Cannot approve a run with blocker exceptions");
        }
        run.setStatus("APPROVED");
        run.setApprovedBy(TenantContext.userId());
        run.setApprovedAt(Instant.now());
        audit.record("PAYROLL_APPROVE", "pay_run", runId.toString());
        return run;
    }

    public PayrollView view(UUID runId) {
        UUID tenantId = TenantContext.requireTenantId();
        PayRun run = runs.findById(runId).orElseThrow();
        List<LineView> lineViews = lines.findByTenantIdAndPayRunId(tenantId, runId).stream().map(l -> new LineView(
                l.getPersonId(), l.getWorkDate(), l.getBucket(), l.getHours(), l.getRate(), l.getAmount(), l.getExplanation()
        )).toList();
        List<ExceptionView> exceptionViews = exceptions.findByTenantIdAndPayRunId(tenantId, runId).stream().map(e -> new ExceptionView(
                e.getPersonId(), e.getWorkDate(), e.getExceptionType(), e.getSeverity(), e.isBlocker(), e.getMessage()
        )).toList();
        String csv = exports.findByTenantIdAndPayRunId(tenantId, runId).stream().findFirst().map(ExportFile::getContent).orElse("");
        PayRunSnapshot snapshot = snapshots.findByTenantIdAndPayRunId(tenantId, runId).orElse(null);
        @SuppressWarnings("unchecked")
        Map<String, String> regularRates = snapshot != null && snapshot.getPayload().get("regularRates") instanceof Map<?, ?> map
                ? (Map<String, String>) map
                : Map.of();
        return new PayrollView(
                run.getId(),
                run.getPayPeriodId(),
                run.getStatus(),
                run.getEngineVersion(),
                run.getApprovedBy(),
                run.getApprovedAt(),
                regularRates,
                lineViews,
                exceptionViews,
                csv,
                run.getSnapshotSha256(),
                successChecklist(tenantId, run, lineViews, exceptionViews, csv, snapshot)
        );
    }

    private SuccessChecklist successChecklist(
            UUID tenantId,
            PayRun run,
            List<LineView> lineViews,
            List<ExceptionView> exceptionViews,
            String csv,
            PayRunSnapshot snapshot
    ) {
        Set<UUID> peopleIds = people.findByTenantId(tenantId).stream().map(Person::getId).collect(Collectors.toSet());
        Set<UUID> lined = lineViews.stream().map(LineView::personId).collect(Collectors.toSet());
        boolean punchesAccounted = exceptionViews.stream()
                .noneMatch(e -> e.blocker() && "UNPAIRED_PUNCH".equals(e.type()))
                || exceptionViews.stream().anyMatch(e -> "UNPAIRED_PUNCH".equals(e.type()));
        boolean everyPunchHandled = exceptionViews.stream().noneMatch(ExceptionView::blocker)
                || exceptionViews.stream().filter(ExceptionView::blocker).allMatch(e -> e.message() != null);
        boolean everyEmployeeHasALine = peopleIds.isEmpty() || lined.containsAll(peopleIds)
                || peopleIds.stream().allMatch(id -> lined.contains(id) || exceptionViews.stream().anyMatch(e -> id.equals(e.personId())));
        boolean regularRateShown = !regularRatesFrom(snapshot).isEmpty() || peopleIds.isEmpty();
        boolean approved = run.getApprovedBy() != null && "APPROVED".equals(run.getStatus());
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

    public record ExceptionView(UUID personId, LocalDate workDate, String type, String severity, boolean blocker, String message) {
    }

    public record PayrollView(
            UUID runId,
            UUID periodId,
            String status,
            String engineVersion,
            UUID approvedBy,
            Instant approvedAt,
            Map<String, String> regularRates,
            List<LineView> lines,
            List<ExceptionView> exceptions,
            String gustoCsv,
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
