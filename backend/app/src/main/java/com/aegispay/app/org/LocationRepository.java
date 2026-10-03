package com.aegispay.app.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    List<Location> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);
}
