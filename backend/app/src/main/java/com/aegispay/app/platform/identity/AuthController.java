package com.aegispay.app.platform.identity;

import com.aegispay.app.platform.tenancy.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.AuthResponse signup(@Valid @RequestBody SignupBody body) {
        if (!Boolean.TRUE.equals(body.tosAccepted())) {
            throw new IllegalArgumentException("Accept the Terms of Service to create a clinic group");
        }
        return authService.signup(new AuthService.SignupRequest(
                body.legalName(), body.vertical(), body.displayName(), body.email(), body.password(), body.tosAccepted()
        ));
    }

    @PostMapping("/login")
    public AuthService.AuthResponse login(@Valid @RequestBody LoginBody body) {
        return authService.login(new AuthService.LoginRequest(body.email(), body.password()));
    }

    @PostMapping("/refresh")
    public AuthService.AuthResponse refresh(@RequestBody RefreshBody body) {
        return authService.refresh(body.refreshToken());
    }

    @PostMapping("/verify-email")
    public java.util.Map<String, String> verify(@RequestBody TokenBody body) {
        return java.util.Map.of("status", authService.verifyEmail(body.token()));
    }

    @PostMapping("/invites")
    @PreAuthorize("hasAnyAuthority('MANAGE_BILLING','MANAGE_ORG')")
    public AuthService.InviteCreated invite(@RequestBody InviteBody body) {
        return authService.invite(new AuthService.InviteRequest(body.email(), body.role(), body.locationId()));
    }

    @PostMapping("/accept-invite")
    public AuthService.AuthResponse acceptInvite(@RequestBody AcceptBody body) {
        return authService.acceptInvite(new AuthService.AcceptInviteRequest(body.token(), body.displayName(), body.password()));
    }

    @GetMapping("/me")
    public AuthService.AuthResponse me() {
        return authService.me(TenantContext.userId());
    }

    public record SignupBody(
            @NotBlank String legalName,
            String vertical,
            @NotBlank String displayName,
            @Email String email,
            @NotBlank String password,
            Boolean tosAccepted
    ) {
    }

    public record LoginBody(@Email String email, @NotBlank String password) {
    }

    public record RefreshBody(@NotBlank String refreshToken) {
    }

    public record TokenBody(@NotBlank String token) {
    }

    public record InviteBody(@Email String email, @NotBlank String role, UUID locationId) {
    }

    public record AcceptBody(@NotBlank String token, @NotBlank String displayName, @NotBlank String password) {
    }
}
