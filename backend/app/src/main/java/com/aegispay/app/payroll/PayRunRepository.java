package com.aegispay.app.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayRunRepository extends JpaRepository<PayRun, UUID> {
    List<PayRun> findByTenantIdAndPayPeriodId(UUID tenantId, UUID payPeriodId);
}
