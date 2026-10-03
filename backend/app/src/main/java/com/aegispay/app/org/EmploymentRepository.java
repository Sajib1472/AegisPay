package com.aegispay.app.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmploymentRepository extends JpaRepository<Employment, UUID> {
    List<Employment> findByTenantIdAndPersonId(UUID tenantId, UUID personId);
}
