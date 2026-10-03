package com.aegispay.app.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, UUID> {
    List<LeavePolicy> findByTenantId(UUID tenantId);
}
