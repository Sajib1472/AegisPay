package com.aegispay.app.time;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MealAttestationRepository extends JpaRepository<MealAttestation, UUID> {
    List<MealAttestation> findByTenantIdAndPersonId(UUID tenantId, UUID personId);
}
