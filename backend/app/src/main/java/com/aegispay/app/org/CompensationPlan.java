package com.aegispay.app.org;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "compensation_plan")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class CompensationPlan extends TenantEntity {

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "plan_type", nullable = false)
    private String planType;

    private BigDecimal percent;

    @Column(name = "flat_amount")
    private BigDecimal flatAmount;

    @Column(nullable = false)
    private boolean discretionary;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public BigDecimal getPercent() {
        return percent;
    }

    public void setPercent(BigDecimal percent) {
        this.percent = percent;
    }

    public BigDecimal getFlatAmount() {
        return flatAmount;
    }

    public void setFlatAmount(BigDecimal flatAmount) {
        this.flatAmount = flatAmount;
    }

    public boolean isDiscretionary() {
        return discretionary;
    }

    public void setDiscretionary(boolean discretionary) {
        this.discretionary = discretionary;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }
}
