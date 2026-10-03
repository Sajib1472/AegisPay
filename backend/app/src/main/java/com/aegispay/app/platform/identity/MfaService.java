package com.aegispay.app.platform.identity;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * TOTP for PAYROLL_APPROVE. Off until card payments for more than three tenants (Step 6 calendar).
 */
@Service
public class MfaService {

    public String enroll(AppUser user) {
        String secret = TokenHasher.randomToken().substring(0, 32);
        user.setTotpSecret(secret);
        user.setTotpConfirmed(false);
        return secret;
    }

    public void confirm(AppUser user, String code) {
        if (user.getTotpSecret() == null || code == null || code.isBlank()) {
            throw new IllegalArgumentException("Invalid TOTP code");
        }
        user.setTotpConfirmed(true);
    }

    public boolean requiredFor(UserRole role, boolean flagOn) {
        return flagOn && Permission.forRole(role).contains(Permission.PAYROLL_APPROVE);
    }

    public UUID userId(AppUser user) {
        return user.getId();
    }
}
