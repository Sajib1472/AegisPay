package com.aegispay.app.payroll;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pay_run_snapshot")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PayRunSnapshot extends TenantEntity {

    @Column(name = "pay_run_id", nullable = false)
    private UUID payRunId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> payload;

    @Column(nullable = false)
    private String sha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getPayRunId() {
        return payRunId;
    }

    public void setPayRunId(UUID payRunId) {
        this.payRunId = payRunId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }
}
