package com.aegispay.app.web;

import com.aegispay.app.org.Location;
import com.aegispay.app.org.OrgSoftDeleteService;
import com.aegispay.app.org.Person;
import com.aegispay.app.payroll.EarningsLedger;
import com.aegispay.app.payroll.EarningsLineEntity;
import com.aegispay.app.time.Punch;
import com.aegispay.app.time.PunchAdjustmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DomainController {

    private final OrgSoftDeleteService softDelete;
    private final PunchAdjustmentService punchAdjustments;
    private final EarningsLedger earnings;

    public DomainController(
            OrgSoftDeleteService softDelete,
            PunchAdjustmentService punchAdjustments,
            EarningsLedger earnings
    ) {
        this.softDelete = softDelete;
        this.punchAdjustments = punchAdjustments;
        this.earnings = earnings;
    }

    @DeleteMapping("/locations/{id}")
    @PreAuthorize("hasAuthority('MANAGE_ORG')")
    public Location archiveLocation(@PathVariable UUID id) {
        return softDelete.archiveLocation(id);
    }

    @DeleteMapping("/people/{id}")
    @PreAuthorize("hasAuthority('MANAGE_ORG')")
    public Person archivePerson(@PathVariable UUID id) {
        return softDelete.archivePerson(id);
    }

    @PostMapping("/punches/{id}/adjust")
    @PreAuthorize("hasAnyAuthority('IMPORT_TIME','PAYROLL_APPROVE')")
    public Punch adjustPunch(@PathVariable UUID id, @RequestBody PunchAdjustBody body) {
        return punchAdjustments.adjust(id, body.adjustedAt(), body.reasonCode(), body.note());
    }

    @PostMapping("/punches/{id}/void")
    @PreAuthorize("hasAnyAuthority('IMPORT_TIME','PAYROLL_APPROVE')")
    public Punch voidPunch(@PathVariable UUID id, @RequestBody PunchAdjustBody body) {
        return punchAdjustments.voidPunch(id, body.reasonCode() == null ? "VOID" : body.reasonCode(), body.note());
    }

    @PostMapping("/earnings-lines/{id}/reverse")
    @PreAuthorize("hasAuthority('PAYROLL_APPROVE')")
    public EarningsLineEntity reverseLine(@PathVariable UUID id) {
        return earnings.reverse(id);
    }

    public record PunchAdjustBody(Instant adjustedAt, String reasonCode, String note) {
    }
}
