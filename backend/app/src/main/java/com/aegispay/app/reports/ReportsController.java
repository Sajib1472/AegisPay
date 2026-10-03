package com.aegispay.app.reports;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportsController {

    private final LaborReportService reports;

    public ReportsController(LaborReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/labor")
    @PreAuthorize("hasAnyAuthority('VIEW_REGISTER','VIEW_RISK_DASHBOARD')")
    public LaborReportService.LaborReport labor() {
        return reports.labor();
    }

    @GetMapping("/risk")
    @PreAuthorize("hasAuthority('VIEW_RISK_DASHBOARD')")
    public LaborReportService.RiskView risk() {
        return reports.risk();
    }
}
