package com.aegispay.app.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EarningsLineRepository extends JpaRepository<EarningsLineEntity, UUID> {
    List<EarningsLineEntity> findByTenantIdAndPayRunId(UUID tenantId, UUID payRunId);
}
