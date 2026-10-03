package com.aegispay.app.org;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "leave_policy")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class LeavePolicy extends TenantEntity {

    @Column(name = "leave_type", nullable = false)
    private String leaveType;

    @Column(name = "accrual_method", nullable = false)
    private String accrualMethod;

    @Column(name = "accrual_rate", nullable = false)
    private BigDecimal accrualRate = BigDecimal.ZERO;

    @Column(name = "cap_hours")
    private BigDecimal capHours;

    @Column(name = "carryover_hours")
    private BigDecimal carryoverHours;

    @Column(name = "state_overlay")
    private String stateOverlay;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public String getAccrualMethod() {
        return accrualMethod;
    }

    public void setAccrualMethod(String accrualMethod) {
        this.accrualMethod = accrualMethod;
    }

    public BigDecimal getAccrualRate() {
        return accrualRate;
    }

    public void setAccrualRate(BigDecimal accrualRate) {
        this.accrualRate = accrualRate;
    }

    public BigDecimal getCapHours() {
        return capHours;
    }

    public void setCapHours(BigDecimal capHours) {
        this.capHours = capHours;
    }

    public BigDecimal getCarryoverHours() {
        return carryoverHours;
    }

    public void setCarryoverHours(BigDecimal carryoverHours) {
        this.carryoverHours = carryoverHours;
    }

    public String getStateOverlay() {
        return stateOverlay;
    }

    public void setStateOverlay(String stateOverlay) {
        this.stateOverlay = stateOverlay;
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
