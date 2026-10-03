package com.aegispay.app.web;

import com.aegispay.app.billing.EntitlementService;
import com.aegispay.app.org.Location;
import com.aegispay.app.org.LocationRepository;
import com.aegispay.app.org.Person;
import com.aegispay.app.org.PersonRepository;
import com.aegispay.app.payroll.PayPeriod;
import com.aegispay.app.payroll.PayPeriodRepository;
import com.aegispay.app.payroll.PayRun;
import com.aegispay.app.payroll.PayrollRunService;
import com.aegispay.app.platform.tenancy.TenantContext;
import com.aegispay.app.time.Punch;
import com.aegispay.app.time.PunchImportService;
import com.aegispay.app.time.PunchRepository;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final LocationRepository locations;
    private final PersonRepository people;
    private final PunchRepository punches;
    private final PunchImportService punchImportService;
    private final PayPeriodRepository periods;
    private final PayrollRunService payrollRunService;
    private final EntitlementService entitlements;

    public ApiController(
            LocationRepository locations,
            PersonRepository people,
            PunchRepository punches,
            PunchImportService punchImportService,
            PayPeriodRepository periods,
            PayrollRunService payrollRunService,
            EntitlementService entitlements
    ) {
        this.locations = locations;
        this.people = people;
        this.punches = punches;
        this.punchImportService = punchImportService;
        this.periods = periods;
        this.payrollRunService = payrollRunService;
        this.entitlements = entitlements;
    }

    @GetMapping("/locations")
    public List<Location> locations() {
        return locations.findByTenantId(TenantContext.requireTenantId());
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
        return locations.save(location);
    }

    @GetMapping("/people")
    public List<Person> people() {
        return people.findByTenantId(TenantContext.requireTenantId());
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
        return people.save(person);
    }

    @GetMapping("/punches")
    public List<Punch> punches() {
        return punches.findByTenantIdOrderByAdjustedAtDesc(TenantContext.requireTenantId());
    }

    @PostMapping(value = "/punches/import", consumes = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAuthority('IMPORT_TIME')")
    public PunchImportService.ImportResult importPunches(
            @RequestParam UUID locationId,
            @RequestParam(defaultValue = "America/Los_Angeles") String timeZone,
            @RequestParam(defaultValue = "upload.csv") String fileName,
            @RequestBody String csv
    ) {
        entitlements.assertWritable();
        return punchImportService.importCsv(fileName, csv, locationId, ZoneId.of(timeZone));
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
        period.setStatus("OPEN");
        return periods.save(period);
    }

    @PostMapping("/pay-periods/{periodId}/runs")
    @PreAuthorize("hasAnyAuthority('PAYROLL_APPROVE','MANAGE_ORG')")
    public PayrollRunService.PayrollView calculate(@PathVariable UUID periodId) {
        PayRun run = payrollRunService.calculate(periodId);
        return payrollRunService.view(run.getId());
    }

    @GetMapping("/pay-runs/{runId}")
    public PayrollRunService.PayrollView view(@PathVariable UUID runId) {
        return payrollRunService.view(runId);
    }

    @PostMapping("/pay-runs/{runId}/approve")
    @PreAuthorize("hasAuthority('PAYROLL_APPROVE')")
    public PayrollRunService.PayrollView approve(@PathVariable UUID runId) {
        payrollRunService.approve(runId);
        return payrollRunService.view(runId);
    }

    public record PeriodBody(String periodType, LocalDate startDate, LocalDate endDate) {
    }

    public record LocationBody(
            String name,
            String line1,
            String city,
            String region,
            String postalCode,
            String timeZone,
            List<String> jurisdictions
    ) {
    }

    public record PersonBody(
            String externalEmployeeCode,
            String legalName,
            String email,
            LocalDate hireDate,
            String exemptionStatus
    ) {
    }
}
