package com.aegispay.app.time;

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
@Table(name = "timesheet_edit")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TimesheetEdit extends TenantEntity {

    @Column(name = "punch_id", nullable = false)
    private UUID punchId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "reason_code", nullable = false)
    private String reasonCode;

    private String note;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_json", nullable = false)
    private Map<String, Object> beforeJson = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_json", nullable = false)
    private Map<String, Object> afterJson = Map.of();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getPunchId() {
        return punchId;
    }

    public void setPunchId(UUID punchId) {
        this.punchId = punchId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Map<String, Object> getBeforeJson() {
        return beforeJson;
    }

    public void setBeforeJson(Map<String, Object> beforeJson) {
        this.beforeJson = beforeJson;
    }

    public Map<String, Object> getAfterJson() {
        return afterJson;
    }

    public void setAfterJson(Map<String, Object> afterJson) {
        this.afterJson = afterJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
