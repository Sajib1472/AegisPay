package com.aegispay.app.payroll;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pay_run_unlock_request")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PayRunUnlockRequest extends TenantEntity {

    @Column(name = "pay_run_id", nullable = false)
    private UUID payRunId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(nullable = false)
    private String status = "PENDING";

    public UUID getPayRunId() {
        return payRunId;
    }

    public void setPayRunId(UUID payRunId) {
        this.payRunId = payRunId;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(UUID requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public UUID getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(UUID confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
