package com.aegispay.app.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayRunExceptionRepository extends JpaRepository<PayRunExceptionEntity, UUID> {
    List<PayRunExceptionEntity> findByTenantIdAndPayRunId(UUID tenantId, UUID payRunId);
}
