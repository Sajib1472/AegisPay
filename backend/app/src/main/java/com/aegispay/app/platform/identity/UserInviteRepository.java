package com.aegispay.app.platform.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserInviteRepository extends JpaRepository<UserInvite, UUID> {
    Optional<UserInvite> findByTokenHash(String tokenHash);
}
