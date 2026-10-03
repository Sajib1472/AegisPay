package com.aegispay.app.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuditArtifactRepository extends JpaRepository<AuditArtifact, UUID> {
    Optional<AuditArtifact> findByTenantIdAndPayRunId(UUID tenantId, UUID payRunId);
}
