package com.aegispay.app.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {
    List<Person> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    Optional<Person> findByTenantIdAndExternalEmployeeCodeAndDeletedAtIsNull(UUID tenantId, String code);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);
}
