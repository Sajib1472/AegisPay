package com.aegispay.app.web;

import com.aegispay.app.billing.EntitlementService;
import com.aegispay.app.org.Employment;
import com.aegispay.app.org.EmploymentRepository;
import com.aegispay.app.org.Location;
import com.aegispay.app.org.LocationRepository;
import com.aegispay.app.org.Person;
import com.aegispay.app.org.PersonRepository;
import com.aegispay.app.payroll.PayPeriod;
import com.aegispay.app.payroll.PayPeriodRepository;
import com.aegispay.app.payroll.PayRun;
import com.aegispay.app.payroll.PayRunUnlockRequest;
import com.aegispay.app.payroll.PayrollRunService;
import com.aegispay.app.platform.ops.IdempotencyService;
import com.aegispay.app.platform.tenancy.TenantContext;
import com.aegispay.app.rules.RulePackResolver;
import com.aegispay.app.time.Punch;
import com.aegispay.app.time.PunchImportService;
import com.aegispay.app.time.PunchQueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final LocationRepository locations;
    private final PersonRepository people;
    private final EmploymentRepository employments;
    private final PunchQueryService punchQuery;
    private final PunchImportService punchImportService;
    private final PayPeriodRepository periods;
    private final PayrollRunService payrollRunService;
    private final EntitlementService entitlements;
    private final IdempotencyService idempotency;

    public ApiController(
            LocationRepository locations,
            PersonRepository people,
            EmploymentRepository employments,
            PunchQueryService punchQuery,
            PunchImportService punchImportService,
            PayPeriodRepository periods,
            PayrollRunService payrollRunService,
            EntitlementService entitlements,
            IdempotencyService idempotency
    ) {
        this.locations = locations;
        this.people = people;
        this.employments = employments;
        this.punchQuery = punchQuery;
        this.punchImportService = punchImportService;
        this.periods = periods;
        this.payrollRunService = payrollRunService;
        this.entitlements = entitlements;
        this.idempotency = idempotency;
    }

    @GetMapping("/locations")
    public List<Location> locations() {
        return locations.findByTenantIdAndDeletedAtIsNull(TenantContext.requireTenantId());
    }

    @PostMapping("/locations")
    @PreAuthorize("hasAnyAuthority('MANAGE_ORG','PAYROLL_APPROVE')")
    public Location createLocation(@RequestBody LocationBody body) {
        entitlements.assertCanAddLocation();
        Location location = new Location();
        location.setName(body.name());
        location.setLine1(body.line1() == null ? "" : body.line1());
        location.setCity(body.city());
        location.setRegion(body.region());
        location.setPostalCode(body.postalCode() == null ? "" : body.postalCode());
        location.setTimeZone(body.timeZone() == null ? "America/Los_Angeles" : body.timeZone());
        location.setJurisdictions(body.jurisdictions() == null ? List.of("US-FLSA") : body.jurisdictions());
        if (body.openingHours() != null) {
            location.setOpeningHours(body.openingHours());
        }
        return locations.save(location);
    }

    @GetMapping("/people")
    public List<Person> people() {
        return people.findByTenantIdAndDeletedAtIsNull(TenantContext.requireTenantId());
    }

    @PostMapping("/people")
    @PreAuthorize("hasAuthority('MANAGE_ORG')")
    public Person createPerson(@RequestBody PersonBody body) {
        entitlements.assertCanAddPerson();
        Person person = new Person();
        person.setExternalEmployeeCode(body.externalEmployeeCode());
        person.setLegalName(body.legalName());
        person.setEmail(body.email());
        person.setHireDate(body.hireDate() == null ? LocalDate.now() : body.hireDate());
        person.setExemptionStatus(body.exemptionStatus() == null ? "NON_EXEMPT" : body.exemptionStatus());
        person.setWorkerType(body.workerType() == null ? "EMPLOYEE" : body.workerType());
        people.save(person);
        Employment stint = new Employment();
        stint.setPersonId(person.getId());
        stint.setWorkerType(person.getWorkerType());
        stint.setExemptionStatus(person.getExemptionStatus());
        stint.setHireDate(person.getHireDate());
        stint.setEffectiveFrom(person.getHireDate());
        employments.save(stint);
        return person;
    }

    @GetMapping("/punches")
    public PageResponse<Punch> punches(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit
    ) {
        return punchQuery.page(cursor, limit);
    }

    @PostMapping(value = "/punches/import", consumes = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAuthority('IMPORT_TIME')")
    public PunchImportService.ImportResult importPunches(
            @RequestParam UUID locationId,
            @RequestParam(defaultValue = "America/Los_Angeles") String timeZone,
            @RequestParam(defaultValue = "upload.csv") String fileName,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody String csv
    ) {
        entitlements.assertWritable();
        return idempotency.run(
                idempotencyKey,
                "POST",
                "/api/v1/punches/import",
                csv,
                PunchImportService.ImportResult.class,
                () -> punchImportService.importCsv(fileName, csv, locationId, ZoneId.of(timeZone))
        );
    }

    @GetMapping("/pay-periods")
    public List<PayPeriod> periods() {
        return periods.findByTenantIdOrderByStartDateDesc(TenantContext.requireTenantId());
    }

    @PostMapping("/pay-periods")
    @PreAuthorize("hasAnyAuthority('MANAGE_ORG','PAYROLL_APPROVE')")
    public PayPeriod createPeriod(@RequestBody PeriodBody body) {
        entitlements.assertCanOpenPayPeriod();
        PayPeriod period = new PayPeriod();
        period.setPeriodType(body.periodType() == null ? "BIWEEKLY" : body.periodType());
        period.setStartDate(body.startDate());
        period.setEndDate(body.endDate());
        period.setStatus("DRAFT");
        return periods.save(period);
    }

    @PostMapping("/pay-periods/{periodId}/runs")
    @PreAuthorize("hasAnyAuthority('PAYROLL_APPROVE','MANAGE_ORG')")
    public PayrollRunService.PayrollView calculate(
            @PathVariable UUID periodId,
            @RequestParam(defaultValue = "HISTORICAL") String law,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        RulePackResolver.LawMode mode = "CURRENT_LAW".equalsIgnoreCase(law)
                ? RulePackResolver.LawMode.CURRENT_LAW
                : RulePackResolver.LawMode.HISTORICAL;
        return idempotency.run(
                idempotencyKey,
                "POST",
                "/api/v1/pay-periods/" + periodId + "/runs",
                periodId + ":" + mode,
                PayrollRunService.PayrollView.class,
                () -> {
                    PayRun run = payrollRunService.calculate(periodId, mode);
                    return payrollRunService.view(run.getId());
                }
        );
    }

    @GetMapping("/pay-runs/{runId}")
    public PayrollRunService.PayrollView view(@PathVariable UUID runId) {
        return payrollRunService.view(runId);
    }

    @GetMapping("/pay-periods/{periodId}/runs/latest")
    public ResponseEntity<PayrollRunService.PayrollView> latestRun(@PathVariable UUID periodId) {
        return payrollRunService.latestRun(periodId)
                .map(run -> ResponseEntity.ok(payrollRunService.view(run.getId())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/pay-runs/{runId}/approve")
    @PreAuthorize("hasAuthority('PAYROLL_APPROVE')")
    public PayrollRunService.PayrollView approve(
            @PathVariable UUID runId,
            @RequestBody(required = false) ApproveBody body,
            HttpServletRequest request
    ) {
        boolean confirmed = body != null && body.confirmed();
        payrollRunService.approve(runId, confirmed, clientIp(request));
        return payrollRunService.view(runId);
    }

    @PostMapping("/pay-runs/{runId}/export")
    @PreAuthorize("hasAnyAuthority('PAYROLL_APPROVE','MANAGE_ORG')")
    public PayrollRunService.PayrollView exportRun(@PathVariable UUID runId) {
        payrollRunService.exportRun(runId);
        return payrollRunService.view(runId);
    }

    @PostMapping("/pay-runs/{runId}/unlock-request")
    @PreAuthorize("hasAuthority('PAYROLL_APPROVE')")
    public PayRunUnlockRequest requestUnlock(@PathVariable UUID runId) {
        return payrollRunService.requestUnlock(runId);
    }

    @PostMapping("/pay-runs/{runId}/unlock")
    @PreAuthorize("hasAuthority('PAYROLL_APPROVE')")
    public PayrollRunService.PayrollView confirmUnlock(@PathVariable UUID runId) {
        payrollRunService.confirmUnlock(runId);
        return payrollRunService.view(runId);
    }

    @PostMapping("/pay-runs/{runId}/exceptions/{exceptionId}/dismiss")
    @PreAuthorize("hasAnyAuthority('PAYROLL_APPROVE','MANAGE_ORG')")
    public PayrollRunService.PayrollView dismissException(
            @PathVariable UUID runId,
            @PathVariable UUID exceptionId,
            @RequestBody DismissBody body
    ) {
        payrollRunService.dismissException(exceptionId, body == null ? null : body.reason());
        return payrollRunService.view(runId);
    }

    @GetMapping("/pay-runs/{runId}/register")
    public PayrollRunService.PayrollView register(@PathVariable UUID runId) {
        return payrollRunService.view(runId);
    }

    @GetMapping("/bonuses")
    public List<com.aegispay.app.payroll.BonusEntry> bonuses() {
        return payrollRunService.listBonuses();
    }

    @PostMapping("/bonuses")
    @PreAuthorize("hasAnyAuthority('PAYROLL_APPROVE','MANAGE_ORG')")
    public com.aegispay.app.payroll.BonusEntry createBonus(@RequestBody BonusBody body) {
        return payrollRunService.createBonus(
                body.personId(),
                body.amount(),
                body.earnedOn(),
                body.discretionary(),
                body.note(),
                body.payPeriodId()
        );
    }

    public record PeriodBody(String periodType, LocalDate startDate, LocalDate endDate) {
    }

    public record ApproveBody(boolean confirmed) {
    }

    public record DismissBody(String reason) {
    }

    public record BonusBody(
            UUID personId,
            BigDecimal amount,
            LocalDate earnedOn,
            boolean discretionary,
            String note,
            UUID payPeriodId
    ) {
    }

    public record LocationBody(
            String name,
            String line1,
            String city,
            String region,
            String postalCode,
            String timeZone,
            List<String> jurisdictions,
            Map<String, Object> openingHours
    ) {
    }

    public record PersonBody(
            String externalEmployeeCode,
            String legalName,
            String email,
            LocalDate hireDate,
            String exemptionStatus,
            String workerType
    ) {
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
