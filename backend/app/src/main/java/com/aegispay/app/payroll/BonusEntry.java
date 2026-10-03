package com.aegispay.app.payroll;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bonus_entry")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class BonusEntry extends TenantEntity {

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "earned_on", nullable = false)
    private LocalDate earnedOn;

    @Column(nullable = false)
    private boolean discretionary;

    @Column(name = "pay_period_id")
    private UUID payPeriodId;

    private String note;

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getEarnedOn() {
        return earnedOn;
    }

    public void setEarnedOn(LocalDate earnedOn) {
        this.earnedOn = earnedOn;
    }

    public boolean isDiscretionary() {
        return discretionary;
    }

    public void setDiscretionary(boolean discretionary) {
        this.discretionary = discretionary;
    }

    public UUID getPayPeriodId() {
        return payPeriodId;
    }

    public void setPayPeriodId(UUID payPeriodId) {
        this.payPeriodId = payPeriodId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
