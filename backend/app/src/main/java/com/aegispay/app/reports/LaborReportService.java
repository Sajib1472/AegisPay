package com.aegispay.app.reports;

import com.aegispay.app.org.Assignment;
import com.aegispay.app.org.AssignmentRepository;
import com.aegispay.app.org.JobCode;
import com.aegispay.app.org.JobCodeRepository;
import com.aegispay.app.org.Location;
import com.aegispay.app.org.LocationRepository;
import com.aegispay.app.payroll.EarningsLineEntity;
import com.aegispay.app.payroll.EarningsLineRepository;
import com.aegispay.app.payroll.PayPeriod;
import com.aegispay.app.payroll.PayPeriodRepository;
import com.aegispay.app.payroll.PayRun;
import com.aegispay.app.payroll.PayRunExceptionEntity;
import com.aegispay.app.payroll.PayRunExceptionRepository;
import com.aegispay.app.payroll.PayRunLifecycle;
import com.aegispay.app.payroll.PayRunRepository;
import com.aegispay.app.platform.tenancy.TenantContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LaborReportService {

    private final PayPeriodRepository periods;
    private final PayRunRepository runs;
    private final EarningsLineRepository lines;
    private final PayRunExceptionRepository exceptions;
    private final AssignmentRepository assignments;
    private final LocationRepository locations;
    private final JobCodeRepository jobs;

    public LaborReportService(
            PayPeriodRepository periods,
            PayRunRepository runs,
            EarningsLineRepository lines,
            PayRunExceptionRepository exceptions,
            AssignmentRepository assignments,
            LocationRepository locations,
            JobCodeRepository jobs
    ) {
        this.periods = periods;
        this.runs = runs;
        this.lines = lines;
        this.exceptions = exceptions;
        this.assignments = assignments;
        this.locations = locations;
        this.jobs = jobs;
    }

    public LaborReport labor() {
        UUID tenantId = TenantContext.requireTenantId();
        PayRun run = latest(tenantId).orElse(null);
        if (run == null) {
            return new LaborReport(null, List.of(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        Map<UUID, Location> locs = new LinkedHashMap<>();
        locations.findByTenantIdAndDeletedAtIsNull(tenantId).forEach(l -> locs.put(l.getId(), l));
        Map<UUID, JobCode> jobById = new LinkedHashMap<>();
        jobs.findByTenantId(tenantId).forEach(j -> jobById.put(j.getId(), j));
        Map<UUID, Assignment> assignmentByPerson = new LinkedHashMap<>();
        assignments.findByTenantId(tenantId).forEach(a -> assignmentByPerson.putIfAbsent(a.getPersonId(), a));

        Map<String, BigDecimal> byLocation = new LinkedHashMap<>();
        Map<String, BigDecimal> byJob = new LinkedHashMap<>();
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal ot = BigDecimal.ZERO;
        BigDecimal premiums = BigDecimal.ZERO;
        for (EarningsLineEntity line : lines.findByTenantIdAndPayRunId(tenantId, run.getId())) {
            BigDecimal amount = line.getAmount() == null ? BigDecimal.ZERO : line.getAmount();
            gross = gross.add(amount);
            if (line.getBucket() != null && line.getBucket().startsWith("OT")) {
                ot = ot.add(amount);
            }
            if (line.getBucket() != null && (line.getBucket().contains("PREMIUM")
                    || "SPLIT_SHIFT".equals(line.getBucket()) || "REPORTING_TIME".equals(line.getBucket()))) {
                premiums = premiums.add(amount);
            }
            Assignment assignment = assignmentByPerson.get(line.getPersonId());
            String locName = "Unassigned";
            String jobName = "Unassigned";
            if (assignment != null) {
                Location location = locs.get(assignment.getLocationId());
                JobCode job = jobById.get(assignment.getJobCodeId());
                if (location != null) {
                    locName = location.getName();
                }
                if (job != null) {
                    jobName = job.getCode();
                }
            }
            byLocation.merge(locName, amount, BigDecimal::add);
            byJob.merge(jobName, amount, BigDecimal::add);
        }
        return new LaborReport(run.getId(), toRows(byLocation), toRows(byJob), gross, ot, premiums);
    }

    public RiskView risk() {
        UUID tenantId = TenantContext.requireTenantId();
        LaborReport labor = labor();
        List<PayPeriod> open = periods.findByTenantIdOrderByStartDateDesc(tenantId).stream()
                .filter(p -> !PayRunLifecycle.isTerminal(p.getStatus()))
                .toList();
        long days = open.stream()
                .mapToLong(p -> Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), p.getEndDate())))
                .min()
                .orElse(-1);
        PayRun run = latest(tenantId).orElse(null);
        long blockers = run == null ? 0 : exceptions.findByTenantIdAndPayRunId(tenantId, run.getId()).stream()
                .filter(PayRunExceptionEntity::isBlocker)
                .count();
        return new RiskView(labor.premiums(), labor.otPay(), labor.gross(), days, blockers, open.size());
    }

    private java.util.Optional<PayRun> latest(UUID tenantId) {
        return periods.findByTenantIdOrderByStartDateDesc(tenantId).stream()
                .flatMap(p -> runs.findByTenantIdAndPayPeriodIdOrderByCreatedAtDesc(tenantId, p.getId()).stream())
                .findFirst();
    }

    private static List<NamedAmount> toRows(Map<String, BigDecimal> map) {
        List<NamedAmount> rows = new ArrayList<>();
        map.forEach((name, amount) -> rows.add(new NamedAmount(name, amount)));
        return rows;
    }

    public record NamedAmount(String name, BigDecimal amount) {
    }

    public record LaborReport(
            UUID runId,
            List<NamedAmount> byLocation,
            List<NamedAmount> byJob,
            BigDecimal gross,
            BigDecimal otPay,
            BigDecimal premiums
    ) {
    }

    public record RiskView(
            BigDecimal premiumsGenerated,
            BigDecimal overtimePay,
            BigDecimal gross,
            long daysUntilPeriodEnd,
            long blockerExceptions,
            int unapprovedPeriods
    ) {
    }
}
