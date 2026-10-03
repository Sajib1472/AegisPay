package com.aegispay.app.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantEntitlementRepository extends JpaRepository<TenantEntitlement, UUID> {
    List<TenantEntitlement> findByTenantId(UUID tenantId);
}
