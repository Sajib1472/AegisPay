package com.aegispay.app.time;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PunchRepository extends JpaRepository<Punch, UUID> {
    List<Punch> findByTenantIdAndPersonIdAndAdjustedAtBetweenOrderByAdjustedAt(
            UUID tenantId, UUID personId, Instant from, Instant to);

    List<Punch> findByTenantIdOrderByAdjustedAtDesc(UUID tenantId);
}
