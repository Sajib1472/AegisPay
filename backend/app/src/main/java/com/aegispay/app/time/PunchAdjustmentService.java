package com.aegispay.app.time;

import com.aegispay.app.platform.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Punches are never deleted. Adjustments write a timesheet_edit row; voids set voided_at.
 */
@Service
public class PunchAdjustmentService {

    private final PunchRepository punches;
    private final TimesheetEditRepository edits;

    public PunchAdjustmentService(PunchRepository punches, TimesheetEditRepository edits) {
        this.punches = punches;
        this.edits = edits;
    }

    @Transactional
    public Punch adjust(UUID punchId, Instant adjustedAt, String reasonCode, String note) {
        Punch punch = punches.findById(punchId).orElseThrow();
        if (punch.getVoidedAt() != null) {
            throw new IllegalStateException("Voided punches cannot be adjusted");
        }
        Map<String, Object> before = snapshot(punch);
        punch.setAdjustedAt(adjustedAt);
        punch.setAdjustReason(reasonCode);
        punch.setAdjustNote(note);
        punch.setAdjustedBy(TenantContext.userId());
        punches.save(punch);
        record(punch, before, snapshot(punch), reasonCode, note);
        return punch;
    }

    @Transactional
    public Punch voidPunch(UUID punchId, String reasonCode, String note) {
        Punch punch = punches.findById(punchId).orElseThrow();
        if (punch.getVoidedAt() != null) {
            return punch;
        }
        Map<String, Object> before = snapshot(punch);
        punch.setVoidedAt(Instant.now());
        punch.setAdjustReason(reasonCode);
        punch.setAdjustNote(note);
        punch.setAdjustedBy(TenantContext.userId());
        punches.save(punch);
        record(punch, before, snapshot(punch), reasonCode == null ? "VOID" : reasonCode, note);
        return punch;
    }

    private void record(Punch punch, Map<String, Object> before, Map<String, Object> after, String reason, String note) {
        UUID actor = TenantContext.userId();
        if (actor == null) {
            throw new IllegalStateException("Timesheet edits require an authenticated actor");
        }
        TimesheetEdit edit = new TimesheetEdit();
        edit.setPunchId(punch.getId());
        edit.setActorId(actor);
        edit.setReasonCode(reason);
        edit.setNote(note);
        edit.setBeforeJson(before);
        edit.setAfterJson(after);
        edits.save(edit);
    }

    static Map<String, Object> snapshot(Punch punch) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", punch.getId() == null ? null : punch.getId().toString());
        row.put("personId", punch.getPersonId() == null ? null : punch.getPersonId().toString());
        row.put("locationId", punch.getLocationId() == null ? null : punch.getLocationId().toString());
        row.put("punchType", punch.getPunchType());
        row.put("source", punch.getSource());
        row.put("originalAt", punch.getOriginalAt() == null ? null : punch.getOriginalAt().toString());
        row.put("adjustedAt", punch.getAdjustedAt() == null ? null : punch.getAdjustedAt().toString());
        row.put("adjustReason", punch.getAdjustReason());
        row.put("voidedAt", punch.getVoidedAt() == null ? null : punch.getVoidedAt().toString());
        return row;
    }
}
