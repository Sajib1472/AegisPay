package com.aegispay.app.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {
    List<Person> findByTenantId(UUID tenantId);

    Optional<Person> findByTenantIdAndExternalEmployeeCode(UUID tenantId, String code);
}
