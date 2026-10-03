package com.aegispay.app.platform.identity;

import com.aegispay.app.org.TenantPolicyEntity;
import com.aegispay.app.org.TenantPolicyRepository;
import com.aegispay.app.platform.tenancy.TenantContext;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final TenantRepository tenants;
    private final AppUserRepository users;
    private final TenantPolicyRepository policies;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            TenantRepository tenants,
            AppUserRepository users,
            TenantPolicyRepository policies,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.tenants = tenants;
        this.users = users;
        this.policies = policies;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
        tenant.setStatus("ACTIVE");
        tenant.setPlan("GROUP");
        tenant.setVertical(request.vertical() == null ? "DENTAL" : request.vertical());
        tenants.save(tenant);

        TenantContext.set(tenant.getId(), null, UserRole.OWNER.name(), UUID.randomUUID().toString());

        AppUser owner = new AppUser();
        owner.setTenantId(tenant.getId());
        owner.setEmail(request.email().toLowerCase(Locale.ROOT));
        owner.setPasswordHash(passwordEncoder.encode(request.password()));
        owner.setDisplayName(request.displayName());
        owner.setRole(UserRole.OWNER);
        owner.setEmailVerified(true);
        users.save(owner);

        TenantPolicyEntity policy = new TenantPolicyEntity();
        policy.setTenantId(tenant.getId());
        policies.save(policy);

        return toResponse(owner, tenant);
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        Tenant tenant = tenants.findById(user.getTenantId()).orElseThrow();
        return toResponse(user, tenant);
    }

    public AuthResponse me(UUID userId) {
        AppUser user = users.findById(userId).orElseThrow();
        Tenant tenant = tenants.findById(user.getTenantId()).orElseThrow();
        return toResponse(user, tenant);
    }

    private AuthResponse toResponse(AppUser user, Tenant tenant) {
        return new AuthResponse(
                jwtService.issueAccessToken(user),
                user.getId(),
                tenant.getId(),
                tenant.getLegalName(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole().name(),
                tenant.getPlan(),
                tenant.getVertical()
        );
    }

    private static String slugify(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    public record SignupRequest(String legalName, String vertical, String displayName, String email, String password) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record AuthResponse(
            String accessToken,
            UUID userId,
            UUID tenantId,
            String tenantName,
            String displayName,
            String email,
            String role,
            String plan,
            String vertical
    ) {
    }
}
