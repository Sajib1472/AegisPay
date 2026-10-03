package com.aegispay.app.billing;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "tenant_entitlement")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TenantEntitlement extends TenantEntity {

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private boolean enabled = true;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
