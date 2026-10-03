package com.aegispay.app.time;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TimesheetEditRepository extends JpaRepository<TimesheetEdit, UUID> {
    List<TimesheetEdit> findByTenantIdAndPunchIdOrderByCreatedAtAsc(UUID tenantId, UUID punchId);
}
