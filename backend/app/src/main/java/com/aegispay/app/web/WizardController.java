package com.aegispay.app.web;

import com.aegispay.app.org.JobCode;
import com.aegispay.app.org.Location;
import com.aegispay.app.org.WizardService;
import com.aegispay.app.payroll.PayPeriod;
import com.aegispay.app.platform.identity.Tenant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wizard")
@PreAuthorize("hasAnyAuthority('MANAGE_ORG','MANAGE_BILLING')")
public class WizardController {

    private final WizardService wizard;

    public WizardController(WizardService wizard) {
        this.wizard = wizard;
    }

    @GetMapping
    public WizardService.WizardView current() {
        return wizard.current();
    }

    @GetMapping("/templates")
    public Object template(@RequestParam(defaultValue = "DENTAL") String vertical) {
        return wizard.template(vertical);
    }

    @GetMapping("/jurisdictions")
    public Map<String, Object> jurisdictions(@RequestParam String city, @RequestParam String region) {
        return wizard.previewLocation(city, region);
    }

    @PostMapping("/profile")
    public WizardService.WizardView profile(@RequestBody ProfileBody body) {
        return wizard.profile(body.legalName(), body.vertical());
    }

    @PostMapping("/locations")
    public Location location(@RequestBody WizardService.LocationDraft draft) {
        return wizard.addLocation(draft);
    }

    @PostMapping("/pay-period")
    public PayPeriod payPeriod(@RequestBody PeriodBody body) {
        return wizard.payPeriod(body.periodType(), body.startDate(), body.endDate());
    }

    @PostMapping("/job-codes")
    public List<JobCode> jobCodes(@RequestBody VerticalBody body) {
        return wizard.applyJobTemplate(body.vertical());
    }

    @PostMapping("/clock")
    public Tenant clock(@RequestBody Map<String, Object> mapping) {
        return wizard.clockMapping(mapping);
    }

    @PostMapping("/destination")
    public Tenant destination(@RequestBody DestinationBody body) {
        return wizard.destination(body.destination());
    }

    @PostMapping("/policies")
    public Tenant policies(@RequestBody PolicyBody body) {
        return wizard.policies(body.roundMinutes(), body.mealWaiverUnderSixHours(), body.autoRestPremium());
    }

    public record ProfileBody(String legalName, String vertical) {
    }

    public record PeriodBody(String periodType, LocalDate startDate, LocalDate endDate) {
    }

    public record VerticalBody(String vertical) {
    }

    public record DestinationBody(String destination) {
    }

    public record PolicyBody(int roundMinutes, boolean mealWaiverUnderSixHours, boolean autoRestPremium) {
    }
}
