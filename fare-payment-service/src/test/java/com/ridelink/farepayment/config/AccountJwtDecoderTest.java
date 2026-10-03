package com.ridelink.farepayment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class AccountJwtDecoderTest {
    private final SecurityConfig config = new SecurityConfig(new ObjectMapper());
    @Test void verifiesTokensProducedByAccountLibraryAtAlgorithmBoundaries() {
        for (int length : new int[]{32, 47, 48, 63, 64, 80}) {
            String secret = (UUID.randomUUID().toString() + UUID.randomUUID() + UUID.randomUUID()).substring(0, length);
            var key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder().subject("42").claim("role", "PASSENGER")
                    .expiration(Date.from(Instant.now().plusSeconds(300))).signWith(key).compact();
            var decoded = config.jwtDecoder(secret).decode(token);
            assertThat(decoded.getSubject()).isEqualTo("42");
            assertThat(decoded.getHeaders().get("alg")).isEqualTo(length >= 64 ? "HS512" : length >= 48 ? "HS384" : "HS256");
        }
    }
    @Test void failsFastForBlankOrShortSecret() {
        for (String secret : List.of("", " ".repeat(64), "short"))
            assertThatThrownBy(() -> config.jwtDecoder(secret)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsDifferentAlgorithmEvenWithTheSameLongKey() {
        String secret = UUID.randomUUID().toString() + UUID.randomUUID();
        String token = Jwts.builder().subject("42").expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
        assertThatThrownBy(() -> config.jwtDecoder(secret).decode(token)).isInstanceOf(JwtException.class);
    }
}
