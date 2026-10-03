package com.aegispay.app.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayPeriodRepository extends JpaRepository<PayPeriod, UUID> {
    List<PayPeriod> findByTenantIdOrderByStartDateDesc(UUID tenantId);

    long countByTenantId(UUID tenantId);
}
