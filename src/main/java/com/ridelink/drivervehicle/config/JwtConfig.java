package com.ridelink.drivervehicle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;

@Configuration
public class JwtConfig {
    @Bean
    JwtDecoder jwtDecoder(@Value("${JWT_SECRET}") String secret) {
        // Match Account JwtService: raw UTF-8 secret, JJWT's key-length-based HMAC algorithm.
        byte[] keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (secret.isBlank() || keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must match Account and contain at least 32 UTF-8 bytes");
        }
        var algorithm = keyBytes.length >= 64
                ? org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS512
                : keyBytes.length >= 48
                    ? org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS384
                    : org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256;
        var key = new javax.crypto.spec.SecretKeySpec(keyBytes, algorithm.getName().replace("HS", "HmacSHA"));
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(algorithm).build();
        OAuth2TokenValidator<Jwt> claims = jwt -> jwt.getSubject() != null
                && !jwt.getSubject().isBlank() && jwt.getSubject().length() <= 100
                && jwt.getExpiresAt() != null
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required identity or expiry missing", null));
        // Account currently does not issue iss/aud. Signature, expiry/nbf and identity are checked.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), claims));
        return decoder;
    }
}
