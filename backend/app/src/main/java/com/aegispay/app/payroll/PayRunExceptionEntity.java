package com.aegispay.app.payroll;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pay_run_exception")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PayRunExceptionEntity extends TenantEntity {

    @Column(name = "pay_run_id", nullable = false)
    private UUID payRunId;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "work_date")
    private LocalDate workDate;

    @Column(name = "exception_type", nullable = false)
    private String exceptionType;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private boolean blocker;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private boolean dismissed;

    @Column(name = "dismiss_reason")
    private String dismissReason;

    @Column(name = "dismissed_by")
    private UUID dismissedBy;

    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    public UUID getPayRunId() {
        return payRunId;
    }

    public void setPayRunId(UUID payRunId) {
        this.payRunId = payRunId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public boolean isBlocker() {
        return blocker;
    }

    public void setBlocker(boolean blocker) {
        this.blocker = blocker;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public boolean isDismissed() {
        return dismissed;
    }

    public void setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
    }

    public String getDismissReason() {
        return dismissReason;
    }

    public void setDismissReason(String dismissReason) {
        this.dismissReason = dismissReason;
    }

    public UUID getDismissedBy() {
        return dismissedBy;
    }

    public void setDismissedBy(UUID dismissedBy) {
        this.dismissedBy = dismissedBy;
    }

    public Instant getDismissedAt() {
        return dismissedAt;
    }

    public void setDismissedAt(Instant dismissedAt) {
        this.dismissedAt = dismissedAt;
    }
}
