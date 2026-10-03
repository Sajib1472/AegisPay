package com.aegispay.app.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayRunSnapshotRepository extends JpaRepository<PayRunSnapshot, UUID> {
    Optional<PayRunSnapshot> findByTenantIdAndPayRunId(UUID tenantId, UUID payRunId);
}
