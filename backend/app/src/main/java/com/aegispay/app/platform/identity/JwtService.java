package com.aegispay.app.platform.identity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final String issuer;
    private final int accessMinutes;
    private final byte[] secret;

    public JwtService(
            @Value("${aegispay.jwt.secret}") String secret,
            @Value("${aegispay.jwt.issuer}") String issuer,
            @Value("${aegispay.jwt.access-minutes}") int accessMinutes
    ) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            bytes = (secret + "::::::::::::::::::::::::::::::::").getBytes(StandardCharsets.UTF_8);
        }
        this.secret = bytes.length >= 32 ? java.util.Arrays.copyOf(bytes, 32) : bytes;
        this.issuer = issuer;
        this.accessMinutes = accessMinutes;
    }

    public String issueAccessToken(AppUser user) {
        try {
            Instant now = Instant.now();
            List<String> perms = Permission.forRole(user.getRole()).stream().map(Enum::name).toList();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(user.getId().toString())
                    .claim("tid", user.getTenantId().toString())
                    .claim("role", user.getRole().name())
                    .claim("email", user.getEmail())
                    .claim("perms", perms)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(accessMinutes * 60L)))
                    .jwtID(UUID.randomUUID().toString())
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign JWT", e);
        }
    }

    public Claims parse(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secret))) {
                throw new IllegalArgumentException("Invalid token signature");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                throw new IllegalArgumentException("Token expired");
            }
            List<String> perms = claims.getStringListClaim("perms");
            return new Claims(
                    UUID.fromString(claims.getSubject()),
                    UUID.fromString(claims.getStringClaim("tid")),
                    claims.getStringClaim("role"),
                    claims.getStringClaim("email"),
                    perms == null ? List.of() : perms
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    public record Claims(UUID userId, UUID tenantId, String role, String email, List<String> permissions) {
    }
}
