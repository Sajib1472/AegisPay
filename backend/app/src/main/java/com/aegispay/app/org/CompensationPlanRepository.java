package com.aegispay.app.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompensationPlanRepository extends JpaRepository<CompensationPlan, UUID> {
    List<CompensationPlan> findByTenantIdAndPersonId(UUID tenantId, UUID personId);
}
