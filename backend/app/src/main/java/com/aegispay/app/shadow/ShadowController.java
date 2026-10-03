package com.aegispay.app.shadow;

import com.aegispay.app.payroll.PayrollRunService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shadow")
public class ShadowController {

    private final PayrollRunService payroll;

    public ShadowController(PayrollRunService payroll) {
        this.payroll = payroll;
    }

    @PostMapping("/compare")
    @PreAuthorize("hasAnyAuthority('PAYROLL_APPROVE','VIEW_REGISTER')")
    public List<ShadowCsvDiff.Row> compare(@RequestBody CompareBody body) {
        String ours = body.ourCsv();
        if ((ours == null || ours.isBlank()) && body.runId() != null) {
            ours = payroll.view(body.runId()).gustoCsv();
        }
        return ShadowCsvDiff.compare(body.excelCsv(), ours);
    }

    public record CompareBody(String excelCsv, String ourCsv, UUID runId) {
    }
}
