package com.aegispay.app.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BonusEntryRepository extends JpaRepository<BonusEntry, UUID> {
    List<BonusEntry> findByTenantIdAndPersonId(UUID tenantId, UUID personId);
}
