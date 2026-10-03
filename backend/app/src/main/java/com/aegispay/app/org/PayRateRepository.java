package com.aegispay.app.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayRateRepository extends JpaRepository<PayRate, UUID> {
    List<PayRate> findByTenantIdAndAssignmentId(UUID tenantId, UUID assignmentId);
}
