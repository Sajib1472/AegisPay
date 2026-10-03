package com.aegispay.app.payroll;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "export_file")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ExportFile extends TenantEntity {

    @Column(name = "pay_run_id", nullable = false)
    private UUID payRunId;

    @Column(nullable = false)
    private String destination;

    @Column(name = "checksum_sha256", nullable = false)
    private String checksumSha256;

    @Column(nullable = false)
    private String content;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    public UUID getPayRunId() {
        return payRunId;
    }

    public void setPayRunId(UUID payRunId) {
        this.payRunId = payRunId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
