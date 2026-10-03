package com.aegispay.app.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayRunUnlockRequestRepository extends JpaRepository<PayRunUnlockRequest, UUID> {
    Optional<PayRunUnlockRequest> findFirstByTenantIdAndPayRunIdAndStatusOrderByRequestedAtDesc(
            UUID tenantId, UUID payRunId, String status);

    List<PayRunUnlockRequest> findByTenantIdAndPayRunId(UUID tenantId, UUID payRunId);
}
