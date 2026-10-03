package com.aegispay.app.payroll;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pay_run")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PayRun extends TenantEntity {

    @Column(name = "pay_period_id", nullable = false)
    private UUID payPeriodId;

    @Column(nullable = false)
    private String status;

    @Column(name = "engine_version", nullable = false)
    private String engineVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_pack_versions", nullable = false)
    private Object rulePackVersions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_pack_ids", nullable = false)
    private Object rulePackIds;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "snapshot_sha256")
    private String snapshotSha256;

    @Column(name = "approval_ip")
    private String approvalIp;

    @Column(name = "unlocked_by")
    private UUID unlockedBy;

    @Column(name = "unlocked_at")
    private Instant unlockedAt;

    @Column(name = "exported_at")
    private Instant exportedAt;

    @Column(name = "lock_at")
    private Instant lockAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getPayPeriodId() {
        return payPeriodId;
    }

    public void setPayPeriodId(UUID payPeriodId) {
        this.payPeriodId = payPeriodId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    public Object getRulePackVersions() {
        return rulePackVersions;
    }

    public void setRulePackVersions(Object rulePackVersions) {
        this.rulePackVersions = rulePackVersions;
    }

    public Object getRulePackIds() {
        return rulePackIds;
    }

    public void setRulePackIds(Object rulePackIds) {
        this.rulePackIds = rulePackIds;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getSnapshotSha256() {
        return snapshotSha256;
    }

    public void setSnapshotSha256(String snapshotSha256) {
        this.snapshotSha256 = snapshotSha256;
    }

    public String getApprovalIp() {
        return approvalIp;
    }

    public void setApprovalIp(String approvalIp) {
        this.approvalIp = approvalIp;
    }

    public UUID getUnlockedBy() {
        return unlockedBy;
    }

    public void setUnlockedBy(UUID unlockedBy) {
        this.unlockedBy = unlockedBy;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(Instant unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public Instant getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(Instant exportedAt) {
        this.exportedAt = exportedAt;
    }

    public Instant getLockAt() {
        return lockAt;
    }

    public void setLockAt(Instant lockAt) {
        this.lockAt = lockAt;
    }
}
