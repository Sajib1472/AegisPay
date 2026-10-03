package com.aegispay.app.audit;

import com.aegispay.app.billing.EntitlementService;
import com.aegispay.app.org.Person;
import com.aegispay.app.org.PersonRepository;
import com.aegispay.app.payroll.PayPeriod;
import com.aegispay.app.payroll.PayPeriodRepository;
import com.aegispay.app.payroll.PayRun;
import com.aegispay.app.payroll.PayRunLifecycle;
import com.aegispay.app.payroll.PayRunRepository;
import com.aegispay.app.payroll.PayrollRunService;
import com.aegispay.app.payroll.PayrollRunService.ExceptionView;
import com.aegispay.app.payroll.PayrollRunService.LineView;
import com.aegispay.app.payroll.PayrollRunService.PayrollView;
import com.aegispay.app.payroll.PayrollRunService.RegisterRow;
import com.aegispay.app.platform.audit.AuditRecorder;
import com.aegispay.app.platform.identity.Tenant;
import com.aegispay.app.platform.identity.TenantRepository;
import com.aegispay.app.platform.tenancy.TenantContext;
import com.aegispay.app.time.Punch;
import com.aegispay.app.time.PunchRepository;
import com.aegispay.engine.EngineVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditPackService {

    public static final String DISCLAIMER =
            "Not legal advice. Calculations are based on the tenant-configured wage order and punches supplied by the customer. AegisPay is not the employer of record and does not remit taxes.";

    private final EntitlementService entitlements;
    private final PayrollRunService payroll;
    private final PayRunRepository runs;
    private final PayPeriodRepository periods;
    private final TenantRepository tenants;
    private final PersonRepository people;
    private final PunchRepository punches;
    private final AuditArtifactRepository artifacts;
    private final LocalObjectStore store;
    private final AuditRecorder audit;

    public AuditPackService(
            EntitlementService entitlements,
            PayrollRunService payroll,
            PayRunRepository runs,
            PayPeriodRepository periods,
            TenantRepository tenants,
            PersonRepository people,
            PunchRepository punches,
            AuditArtifactRepository artifacts,
            LocalObjectStore store,
            AuditRecorder audit
    ) {
        this.entitlements = entitlements;
        this.payroll = payroll;
        this.runs = runs;
        this.periods = periods;
        this.tenants = tenants;
        this.people = people;
        this.punches = punches;
        this.artifacts = artifacts;
        this.store = store;
        this.audit = audit;
    }

    @Transactional
    public AuditPackFile generate(UUID runId) {
        entitlements.assertAuditPack();
        UUID tenantId = TenantContext.requireTenantId();
        PayRun run = runs.findById(runId).orElseThrow();
        AuditArtifact existing = artifacts.findByTenantIdAndPayRunId(tenantId, runId).orElse(null);
        byte[] pdf = AuditPdfRenderer.render(linesFor(run));
        String digest = sha(pdf);
        if (existing != null) {
            if (PayRunLifecycle.isTerminal(run.getStatus()) && !digest.equals(existing.getSha256())) {
                throw new IllegalStateException("Locked audit pack checksum changed. Engine or PDF template bug.");
            }
            if (digest.equals(existing.getSha256())) {
                return new AuditPackFile(existing.getSha256(), store.get(existing.getStorageKey()), existing.getByteLength());
            }
        }
        String key = store.put(tenantId, digest, pdf);
        AuditArtifact row = existing == null ? new AuditArtifact() : existing;
        row.setPayRunId(runId);
        row.setStorageKey(key);
        row.setSha256(digest);
        row.setByteLength(pdf.length);
        artifacts.save(row);
        run.setAuditPdfSha256(digest);
        audit.record("AUDIT_PACK", "pay_run", runId.toString());
        return new AuditPackFile(digest, pdf, pdf.length);
    }

    List<String> linesFor(PayRun run) {
        Tenant tenant = tenants.findById(TenantContext.requireTenantId()).orElseThrow();
        PayPeriod period = periods.findById(run.getPayPeriodId()).orElseThrow();
        PayrollView view = payroll.view(run.getId());
        List<String> lines = new ArrayList<>();
        lines.add("AEGISPAY AUDIT PACK");
        lines.add("Tenant: " + tenant.getLegalName());
        lines.add("Period: " + period.getStartDate() + " to " + period.getEndDate());
        lines.add("Engine: " + EngineVersion.VALUE);
        lines.add("Rule packs: " + String.valueOf(run.getRulePackVersions()));
        lines.add("Approver: " + (run.getApprovedBy() == null ? "none" : run.getApprovedBy()));
        lines.add("Approved at: " + (run.getApprovedAt() == null ? "n/a" : run.getApprovedAt()));
        lines.add("Snapshot: " + (run.getSnapshotSha256() == null ? "n/a" : run.getSnapshotSha256()));
        lines.add("");
        lines.add("SUMMARY BY EARNING TYPE");
        Map<String, String> totals = new LinkedHashMap<>();
        for (LineView line : view.lines()) {
            totals.merge(line.bucket(), line.amount() == null ? "0" : line.amount().toPlainString(),
                    (a, b) -> new java.math.BigDecimal(a).add(new java.math.BigDecimal(b)).toPlainString());
        }
        totals.forEach((bucket, amount) -> lines.add(bucket + "  " + amount));
        lines.add("");
        lines.add("REGISTER");
        for (RegisterRow row : view.register()) {
            lines.add(row.employeeCode() + "  " + row.legalName() + "  gross=" + row.grossEarningsSubmitted());
        }
        lines.add("");
        lines.add("EXCEPTIONS");
        List<ExceptionView> exceptions = new ArrayList<>(view.exceptions());
        exceptions.sort(Comparator.comparing(e -> String.valueOf(e.type()) + e.workDate()));
        for (ExceptionView ex : exceptions) {
            lines.add(ex.type() + "  blocker=" + ex.blocker() + "  dismissed=" + ex.dismissed() + "  " + ex.message());
        }
        lines.add("");
        lines.add("PUNCH APPENDIX (premium or OT)");
        Set<UUID> flagged = view.lines().stream()
                .filter(l -> l.bucket() != null && (l.bucket().startsWith("OT") || l.bucket().contains("PREMIUM")
                        || "SPLIT_SHIFT".equals(l.bucket()) || "REPORTING_TIME".equals(l.bucket())))
                .map(LineView::personId)
                .collect(Collectors.toSet());
        Instant from = period.getStartDate().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = period.getEndDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<UUID, Person> byId = people.findByTenantIdAndDeletedAtIsNull(tenant.getId()).stream()
                .collect(Collectors.toMap(Person::getId, p -> p));
        flagged.stream().sorted().forEach(personId -> {
            Person person = byId.get(personId);
            lines.add("-- " + (person == null ? personId : person.getLegalName()));
            for (Punch punch : punches.findByTenantIdAndPersonIdAndAdjustedAtBetweenOrderByAdjustedAt(
                    tenant.getId(), personId, from, to)) {
                if (punch.getVoidedAt() != null) {
                    continue;
                }
                lines.add(punch.getPunchType() + "  " + punch.getAdjustedAt());
            }
        });
        lines.add("");
        lines.add(DISCLAIMER);
        return lines;
    }

    private static String sha(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record AuditPackFile(String sha256, byte[] pdf, int byteLength) {
        public String fileName() {
            return "aegispay-audit-" + sha256.substring(0, 12) + ".pdf";
        }
    }
}
