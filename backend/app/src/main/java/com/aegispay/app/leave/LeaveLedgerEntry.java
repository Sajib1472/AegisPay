package com.aegispay.app.leave;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_ledger")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class LeaveLedgerEntry extends TenantEntity {

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "pay_run_id")
    private UUID payRunId;

    @Column(name = "policy_code", nullable = false)
    private String policyCode;

    @Column(name = "entry_type", nullable = false)
    private String entryType;

    @Column(name = "hours_delta", nullable = false)
    private BigDecimal hoursDelta;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "work_date")
    private LocalDate workDate;

    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public void setPayRunId(UUID payRunId) {
        this.payRunId = payRunId;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public void setPolicyCode(String policyCode) {
        this.policyCode = policyCode;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getHoursDelta() {
        return hoursDelta;
    }

    public void setHoursDelta(BigDecimal hoursDelta) {
        this.hoursDelta = hoursDelta;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
