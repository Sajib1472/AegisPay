package com.aegispay.app.web;

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

    public ApiController(
            LocationRepository locations,
            PersonRepository people,
            PunchRepository punches,
            PunchImportService punchImportService,
            PayPeriodRepository periods,
            PayrollRunService payrollRunService
    ) {
        this.locations = locations;
        this.people = people;
        this.punches = punches;
        this.punchImportService = punchImportService;
        this.periods = periods;
        this.payrollRunService = payrollRunService;
    }

    @GetMapping("/locations")
    public List<Location> locations() {
        return locations.findByTenantId(TenantContext.requireTenantId());
    }

    @GetMapping("/people")
    public List<Person> people() {
        return people.findByTenantId(TenantContext.requireTenantId());
    }

    @GetMapping("/punches")
    public List<Punch> punches() {
        return punches.findByTenantIdOrderByAdjustedAtDesc(TenantContext.requireTenantId());
    }

    @PostMapping(value = "/punches/import", consumes = MediaType.TEXT_PLAIN_VALUE)
    public PunchImportService.ImportResult importPunches(
            @RequestParam UUID locationId,
            @RequestParam(defaultValue = "America/Los_Angeles") String timeZone,
            @RequestParam(defaultValue = "upload.csv") String fileName,
            @RequestBody String csv
    ) {
        return punchImportService.importCsv(fileName, csv, locationId, ZoneId.of(timeZone));
    }

    @GetMapping("/pay-periods")
    public List<PayPeriod> periods() {
        return periods.findByTenantIdOrderByStartDateDesc(TenantContext.requireTenantId());
    }

    @PostMapping("/pay-periods")
    public PayPeriod createPeriod(@RequestBody PeriodBody body) {
        PayPeriod period = new PayPeriod();
        period.setPeriodType(body.periodType() == null ? "BIWEEKLY" : body.periodType());
        period.setStartDate(body.startDate());
        period.setEndDate(body.endDate());
        period.setStatus("OPEN");
        return periods.save(period);
    }

    @PostMapping("/pay-periods/{periodId}/runs")
    public PayrollRunService.PayrollView calculate(@PathVariable UUID periodId) {
        PayRun run = payrollRunService.calculate(periodId);
        return payrollRunService.view(run.getId());
    }

    @GetMapping("/pay-runs/{runId}")
    public PayrollRunService.PayrollView view(@PathVariable UUID runId) {
        return payrollRunService.view(runId);
    }

    @PostMapping("/pay-runs/{runId}/approve")
    public PayrollRunService.PayrollView approve(@PathVariable UUID runId) {
        payrollRunService.approve(runId);
        return payrollRunService.view(runId);
    }

    public record PeriodBody(String periodType, LocalDate startDate, LocalDate endDate) {
    }
}
