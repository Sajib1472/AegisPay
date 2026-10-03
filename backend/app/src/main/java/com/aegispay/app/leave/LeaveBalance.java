package com.aegispay.app.leave;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_balance")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class LeaveBalance extends TenantEntity {

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "policy_code", nullable = false)
    private String policyCode;

    @Column(nullable = false)
    private BigDecimal hours;

    @Column(name = "as_of", nullable = false)
    private LocalDate asOf;

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public void setPolicyCode(String policyCode) {
        this.policyCode = policyCode;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public LocalDate getAsOf() {
        return asOf;
    }

    public void setAsOf(LocalDate asOf) {
        this.asOf = asOf;
    }
}
