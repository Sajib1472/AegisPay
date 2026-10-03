package com.aegispay.app.time;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PunchImportBatchRepository extends JpaRepository<PunchImportBatch, UUID> {
    Optional<PunchImportBatch> findByTenantIdAndFileSha256(UUID tenantId, String sha);
}
