package com.aegispay.app.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobCodeRepository extends JpaRepository<JobCode, UUID> {
    List<JobCode> findByTenantId(UUID tenantId);
}
