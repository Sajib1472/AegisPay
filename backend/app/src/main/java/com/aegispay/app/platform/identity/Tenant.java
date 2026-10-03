package com.aegispay.app.platform.identity;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tenant")
public class Tenant {

    @jakarta.persistence.Id
    private java.util.UUID id;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String plan;

    @Column(nullable = false)
    private String vertical;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "audit_pack_enabled", nullable = false)
    private boolean auditPackEnabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID();
        }
    }

    public java.util.UUID getId() {
        return id;
    }

    public void setId(java.util.UUID id) {
        this.id = id;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getVertical() {
        return vertical;
    }

    public void setVertical(String vertical) {
        this.vertical = vertical;
    }

    public Instant getTrialEndsAt() {
        return trialEndsAt;
    }

    public void setTrialEndsAt(Instant trialEndsAt) {
        this.trialEndsAt = trialEndsAt;
    }

    public boolean isAuditPackEnabled() {
        return auditPackEnabled;
    }

    public void setAuditPackEnabled(boolean auditPackEnabled) {
        this.auditPackEnabled = auditPackEnabled;
    }
}
