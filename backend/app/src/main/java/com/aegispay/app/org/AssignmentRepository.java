package com.aegispay.app.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByTenantIdAndPersonId(UUID tenantId, UUID personId);

    List<Assignment> findByTenantIdAndLocationId(UUID tenantId, UUID locationId);

    List<Assignment> findByTenantId(UUID tenantId);
}
