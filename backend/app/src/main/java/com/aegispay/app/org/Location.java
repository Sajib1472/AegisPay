package com.aegispay.app.org;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "location")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Location extends TenantEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String line1;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String region;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String country = "US";

    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> jurisdictions = List.of();

    @Column(name = "wage_order")
    private String wageOrder;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public List<String> getJurisdictions() {
        return jurisdictions;
    }

    public void setJurisdictions(List<String> jurisdictions) {
        this.jurisdictions = jurisdictions;
    }

    public String getWageOrder() {
        return wageOrder;
    }

    public void setWageOrder(String wageOrder) {
        this.wageOrder = wageOrder;
    }
}
