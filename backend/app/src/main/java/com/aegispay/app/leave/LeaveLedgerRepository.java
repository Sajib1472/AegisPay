package com.aegispay.app.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeaveLedgerRepository extends JpaRepository<LeaveLedgerEntry, UUID> {
    List<LeaveLedgerEntry> findByTenantIdAndPersonIdOrderByCreatedAtAsc(UUID tenantId, UUID personId);

    boolean existsByTenantIdAndPayRunIdAndPersonIdAndEntryType(UUID tenantId, UUID payRunId, UUID personId, String entryType);
}
