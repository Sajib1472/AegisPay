package com.aegispay.app.org;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;

@Entity
@Table(name = "person")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Person extends TenantEntity {

    @Column(name = "external_employee_code", nullable = false)
    private String externalEmployeeCode;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    private String email;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "exemption_status", nullable = false)
    private String exemptionStatus;

    public String getExternalEmployeeCode() {
        return externalEmployeeCode;
    }

    public void setExternalEmployeeCode(String externalEmployeeCode) {
        this.externalEmployeeCode = externalEmployeeCode;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public String getExemptionStatus() {
        return exemptionStatus;
    }

    public void setExemptionStatus(String exemptionStatus) {
        this.exemptionStatus = exemptionStatus;
    }
}
