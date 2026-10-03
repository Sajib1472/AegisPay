package com.aegispay.app.web;

import com.aegispay.app.time.MealAttestation;
import com.aegispay.app.time.MealAttestationRepository;
import com.aegispay.app.time.PunchImportPreviewService;
import com.aegispay.app.time.PunchReasonCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TimeIngestionController {

    private final PunchImportPreviewService preview;
    private final MealAttestationRepository attestations;

    public TimeIngestionController(PunchImportPreviewService preview, MealAttestationRepository attestations) {
        this.preview = preview;
        this.attestations = attestations;
    }

    @PostMapping("/punches/preview")
    @PreAuthorize("hasAuthority('IMPORT_TIME')")
    public PunchImportPreviewService.Preview preview(
            @RequestParam(defaultValue = "GENERIC") String format,
            @RequestParam(defaultValue = "America/Los_Angeles") String timeZone,
            @RequestBody PreviewBody body
    ) {
        return preview.preview(body.csv(), format, body.mapping(), ZoneId.of(timeZone));
    }

    @GetMapping("/punches/reason-codes")
    public List<String> reasonCodes() {
        return Arrays.stream(PunchReasonCode.values()).map(Enum::name).toList();
    }

    @PostMapping("/attestations")
    @PreAuthorize("hasAnyAuthority('IMPORT_TIME','PAYROLL_APPROVE')")
    public MealAttestation attest(@RequestBody AttestationBody body) {
        MealAttestation row = new MealAttestation();
        row.setPersonId(body.personId());
        row.setWorkDate(body.workDate());
        row.setMealReceived(body.meal());
        row.setRestReceived(body.rest());
        row.setSource("MANAGER");
        return attestations.save(row);
    }

    public record PreviewBody(String csv, Map<String, Integer> mapping) {
    }

    public record AttestationBody(UUID personId, LocalDate workDate, String meal, String rest) {
    }
}
