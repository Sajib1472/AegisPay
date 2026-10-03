package com.aegispay.app.payroll;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "earnings_line")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class EarningsLineEntity extends TenantEntity {

    @Column(name = "pay_run_id", nullable = false)
    private UUID payRunId;

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "work_date")
    private LocalDate workDate;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private BigDecimal hours;

    @Column(nullable = false)
    private BigDecimal rate;

    @Column(nullable = false)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> explanation = Map.of();

    public void setPayRunId(UUID payRunId) {
        this.payRunId = payRunId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Map<String, Object> getExplanation() {
        return explanation;
    }

    public void setExplanation(Map<String, Object> explanation) {
        this.explanation = explanation;
    }
}
