package com.aegispay.app.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {
    Optional<LeaveBalance> findByTenantIdAndPersonIdAndPolicyCode(UUID tenantId, UUID personId, String policyCode);

    List<LeaveBalance> findByTenantId(UUID tenantId);
}
