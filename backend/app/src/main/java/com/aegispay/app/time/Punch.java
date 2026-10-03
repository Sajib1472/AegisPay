package com.aegispay.app.time;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "punch")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Punch extends TenantEntity {

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "import_batch_id")
    private UUID importBatchId;

    @Column(name = "punch_type", nullable = false)
    private String punchType;

    @Column(nullable = false)
    private String source;

    @Column(name = "original_at", nullable = false)
    private Instant originalAt;

    @Column(name = "adjusted_at", nullable = false)
    private Instant adjustedAt;

    @Column(name = "adjust_reason")
    private String adjustReason;

    @Column(name = "adjust_note")
    private String adjustNote;

    @Column(name = "adjusted_by")
    private UUID adjustedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public UUID getLocationId() {
        return locationId;
    }

    public void setLocationId(UUID locationId) {
        this.locationId = locationId;
    }

    public UUID getImportBatchId() {
        return importBatchId;
    }

    public void setImportBatchId(UUID importBatchId) {
        this.importBatchId = importBatchId;
    }

    public String getPunchType() {
        return punchType;
    }

    public void setPunchType(String punchType) {
        this.punchType = punchType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getOriginalAt() {
        return originalAt;
    }

    public void setOriginalAt(Instant originalAt) {
        this.originalAt = originalAt;
    }

    public Instant getAdjustedAt() {
        return adjustedAt;
    }

    public void setAdjustedAt(Instant adjustedAt) {
        this.adjustedAt = adjustedAt;
    }

    public String getAdjustReason() {
        return adjustReason;
    }

    public void setAdjustReason(String adjustReason) {
        this.adjustReason = adjustReason;
    }

    public String getAdjustNote() {
        return adjustNote;
    }

    public void setAdjustNote(String adjustNote) {
        this.adjustNote = adjustNote;
    }

    public UUID getAdjustedBy() {
        return adjustedBy;
    }

    public void setAdjustedBy(UUID adjustedBy) {
        this.adjustedBy = adjustedBy;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public void setVoidedAt(Instant voidedAt) {
        this.voidedAt = voidedAt;
    }
}
