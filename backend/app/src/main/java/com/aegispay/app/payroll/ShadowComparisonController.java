package com.aegispay.app.payroll;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shadow")
public class ShadowComparisonController {

    private final ShadowComparisonService shadow;
    private final PayrollRunService payroll;

    public ShadowComparisonController(ShadowComparisonService shadow, PayrollRunService payroll) {
        this.shadow = shadow;
        this.payroll = payroll;
    }

    @PostMapping("/compare")
    @PreAuthorize("hasAnyAuthority('PAYROLL_APPROVE','VIEW_REGISTER')")
    public List<ShadowComparisonService.DiffRow> compare(@RequestBody CompareBody body) {
        String ours = payroll.view(body.runId()).gustoCsv();
        return shadow.diff(body.theirsCsv(), ours);
    }

    public record CompareBody(UUID runId, String theirsCsv) {
    }
}
