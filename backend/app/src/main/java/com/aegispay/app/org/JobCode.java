package com.aegispay.app.org;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "job_code")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class JobCode extends TenantEntity {

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
