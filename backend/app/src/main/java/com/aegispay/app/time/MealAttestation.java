package com.aegispay.app.time;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "meal_attestation")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class MealAttestation extends TenantEntity {

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "meal_received", nullable = false)
    private String mealReceived;

    @Column(name = "rest_received", nullable = false)
    private String restReceived;

    @Column(nullable = false)
    private String source;

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

    public String getMealReceived() {
        return mealReceived;
    }

    public void setMealReceived(String mealReceived) {
        this.mealReceived = mealReceived;
    }

    public String getRestReceived() {
        return restReceived;
    }

    public void setRestReceived(String restReceived) {
        this.restReceived = restReceived;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
