package com.aegispay.app.audit;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pay-runs/{runId}/audit-pack")
public class AuditPackController {

    private final AuditPackService packs;

    public AuditPackController(AuditPackService packs) {
        this.packs = packs;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('PAYROLL_APPROVE','VIEW_REGISTER')")
    public Map<String, Object> generate(@PathVariable UUID runId) {
        AuditPackService.AuditPackFile file = packs.generate(runId);
        return Map.of("sha256", file.sha256(), "bytes", file.byteLength(), "fileName", file.fileName());
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PAYROLL_APPROVE','VIEW_REGISTER')")
    public ResponseEntity<byte[]> download(@PathVariable UUID runId) {
        AuditPackService.AuditPackFile file = packs.generate(runId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName())
                        .build()
                        .toString())
                .body(file.pdf());
    }
}
