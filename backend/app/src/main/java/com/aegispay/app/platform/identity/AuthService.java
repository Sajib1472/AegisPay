package com.aegispay.app.platform.identity;

import com.aegispay.app.billing.Subscription;
import com.aegispay.app.billing.SubscriptionRepository;
import com.aegispay.app.org.TenantPolicyEntity;
import com.aegispay.app.org.TenantPolicyRepository;
import com.aegispay.app.platform.tenancy.TenantContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final TenantRepository tenants;
    private final AppUserRepository users;
    private final TenantPolicyRepository policies;
    private final SubscriptionRepository subscriptions;
    private final RefreshTokenRepository refreshTokens;
    private final EmailVerificationRepository verifications;
    private final UserInviteRepository invites;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final boolean mfaRequiredForApprove;

    public AuthService(
            TenantRepository tenants,
            AppUserRepository users,
            TenantPolicyRepository policies,
            SubscriptionRepository subscriptions,
            RefreshTokenRepository refreshTokens,
            EmailVerificationRepository verifications,
            UserInviteRepository invites,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${aegispay.mfa.payroll-approve:false}") boolean mfaRequiredForApprove
    ) {
        this.tenants = tenants;
        this.users = users;
        this.policies = policies;
        this.subscriptions = subscriptions;
        this.refreshTokens = refreshTokens;
        this.verifications = verifications;
        this.invites = invites;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mfaRequiredForApprove = mfaRequiredForApprove;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String slug = slugify(request.legalName());
        if (tenants.findBySlug(slug).isPresent()) {
            throw new IllegalArgumentException("A clinic group with that name already exists");
        }
        Tenant tenant = new Tenant();
        tenant.setLegalName(request.legalName());
        tenant.setSlug(slug);
        tenant.setStatus("TRIAL");
        tenant.setPlan("PILOT");
        tenant.setVertical(request.vertical() == null ? "DENTAL" : request.vertical());
        tenant.setTrialEndsAt(Instant.now().plus(14, ChronoUnit.DAYS));
        tenants.save(tenant);

        TenantContext.set(tenant.getId(), null, UserRole.OWNER.name(), UUID.randomUUID().toString());

        AppUser owner = new AppUser();
        owner.setTenantId(tenant.getId());
        owner.setEmail(request.email().toLowerCase(Locale.ROOT));
        owner.setPasswordHash(passwordEncoder.encode(request.password()));
        owner.setDisplayName(request.displayName());
        owner.setRole(UserRole.OWNER);
        owner.setEmailVerified(false);
        users.save(owner);

        TenantPolicyEntity policy = new TenantPolicyEntity();
        policy.setTenantId(tenant.getId());
        policies.save(policy);

        Subscription subscription = new Subscription();
        subscription.setTenantId(tenant.getId());
        subscription.setPlan("PILOT");
        subscription.setStatus("TRIALING");
        subscription.setCurrentPeriodEnd(tenant.getTrialEndsAt());
        subscriptions.save(subscription);

        String verify = issueEmailVerification(owner);
        return toResponse(owner, tenant, issueRefresh(owner), verify);
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        Tenant tenant = tenants.findById(user.getTenantId()).orElseThrow();
        TenantContext.set(tenant.getId(), user.getId(), user.getRole().name(), UUID.randomUUID().toString());
        return toResponse(user, tenant, issueRefresh(user), null);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(TokenHasher.sha256(refreshToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }
        stored.setRevokedAt(Instant.now());
        AppUser user = users.findById(stored.getUserId()).orElseThrow();
        Tenant tenant = tenants.findById(user.getTenantId()).orElseThrow();
        TenantContext.set(tenant.getId(), user.getId(), user.getRole().name(), UUID.randomUUID().toString());
        return toResponse(user, tenant, issueRefresh(user), null);
    }

    @Transactional
    public String verifyEmail(String token) {
        EmailVerification row = verifications.findByTokenHash(TokenHasher.sha256(token))
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));
        if (row.getConsumedAt() != null || row.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Verification token expired");
        }
        AppUser user = users.findById(row.getUserId()).orElseThrow();
        user.setEmailVerified(true);
        row.setConsumedAt(Instant.now());
        return "verified";
    }

    @Transactional
    public InviteCreated invite(InviteRequest request) {
        if (TenantContext.role() == null || !(UserRole.OWNER.name().equals(TenantContext.role())
                || UserRole.PLATFORM_ADMIN.name().equals(TenantContext.role()))) {
            throw new IllegalStateException("Only the owner can invite users");
        }
        String raw = TokenHasher.randomToken();
        UserInvite invite = new UserInvite();
        invite.setEmail(request.email().toLowerCase(Locale.ROOT));
        invite.setRole(UserRole.valueOf(request.role()));
        invite.setLocationId(request.locationId());
        invite.setTokenHash(TokenHasher.sha256(raw));
        invite.setInvitedBy(TenantContext.userId());
        invite.setExpiresAt(Instant.now().plus(48, ChronoUnit.HOURS));
        invites.save(invite);
        return new InviteCreated(invite.getId(), raw, invite.getExpiresAt());
    }

    @Transactional
    public AuthResponse acceptInvite(AcceptInviteRequest request) {
        UserInvite invite = invites.findByTokenHash(TokenHasher.sha256(request.token()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite"));
        if (invite.getAcceptedAt() != null || invite.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invite expired");
        }
        TenantContext.set(invite.getTenantId(), null, invite.getRole().name(), UUID.randomUUID().toString());
        AppUser user = new AppUser();
        user.setTenantId(invite.getTenantId());
        user.setEmail(invite.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRole(invite.getRole());
        user.setLocationId(invite.getLocationId());
        user.setEmailVerified(true);
        user.setInvitedBy(invite.getInvitedBy() != null ? invite.getInvitedBy() : null);
        users.save(user);
        invite.setAcceptedAt(Instant.now());
        Tenant tenant = tenants.findById(user.getTenantId()).orElseThrow();
        return toResponse(user, tenant, issueRefresh(user), null);
    }

    public AuthResponse me(UUID userId) {
        AppUser user = users.findById(userId).orElseThrow();
        Tenant tenant = tenants.findById(user.getTenantId()).orElseThrow();
        return toResponse(user, tenant, null, null);
    }

    public void assertCanRunPayroll(AppUser user) {
        if (!user.isEmailVerified()) {
            throw new IllegalStateException("Verify the owner email before running payroll");
        }
        if (mfaRequiredForApprove
                && Permission.forRole(user.getRole()).contains(Permission.PAYROLL_APPROVE)
                && !user.isTotpConfirmed()) {
            throw new IllegalStateException("TOTP MFA is required for PAYROLL_APPROVE before card payments");
        }
    }

    public AppUser requireUser() {
        return users.findById(TenantContext.userId()).orElseThrow();
    }

    private String issueEmailVerification(AppUser user) {
        String raw = TokenHasher.randomToken();
        EmailVerification row = new EmailVerification();
        row.setUserId(user.getId());
        row.setTokenHash(TokenHasher.sha256(raw));
        row.setExpiresAt(Instant.now().plus(48, ChronoUnit.HOURS));
        verifications.save(row);
        return raw;
    }

    private String issueRefresh(AppUser user) {
        String raw = TokenHasher.randomToken();
        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(TokenHasher.sha256(raw));
        token.setExpiresAt(Instant.now().plus(jwtService.refreshDays(), ChronoUnit.DAYS));
        refreshTokens.save(token);
        return raw;
    }

    private AuthResponse toResponse(AppUser user, Tenant tenant, String refresh, String emailToken) {
        return new AuthResponse(
                jwtService.issueAccessToken(user),
                refresh,
                emailToken,
                user.getId(),
                tenant.getId(),
                tenant.getLegalName(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole().name(),
                tenant.getPlan(),
                tenant.getVertical(),
                tenant.getTrialEndsAt(),
                user.isEmailVerified(),
                Permission.forRole(user.getRole()).stream().map(Enum::name).toList()
        );
    }

    private static String slugify(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    public record SignupRequest(String legalName, String vertical, String displayName, String email, String password) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record InviteRequest(String email, String role, UUID locationId) {
    }

    public record AcceptInviteRequest(String token, String displayName, String password) {
    }

    public record InviteCreated(UUID inviteId, String token, Instant expiresAt) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String emailVerificationToken,
            UUID userId,
            UUID tenantId,
            String tenantName,
            String displayName,
            String email,
            String role,
            String plan,
            String vertical,
            Instant trialEndsAt,
            boolean emailVerified,
            List<String> permissions
    ) {
    }
}
