package com.aegispay.app.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantPolicyRepository extends JpaRepository<TenantPolicyEntity, UUID> {
}
