package com.aegispay.app.platform.identity;

import com.aegispay.app.platform.tenancy.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
        return authService.signup(new AuthService.SignupRequest(
                body.legalName(), body.vertical(), body.displayName(), body.email(), body.password()
        ));
    }

    @PostMapping("/login")
    public AuthService.AuthResponse login(@Valid @RequestBody LoginBody body) {
        return authService.login(new AuthService.LoginRequest(body.email(), body.password()));
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
            @NotBlank String password
    ) {
    }

    public record LoginBody(@Email String email, @NotBlank String password) {
    }
}
