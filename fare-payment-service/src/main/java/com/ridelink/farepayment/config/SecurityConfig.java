package com.ridelink.farepayment.config;

import com.ridelink.farepayment.dto.ApiError;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final ObjectMapper mapper;
    public SecurityConfig(ObjectMapper mapper) { this.mapper = mapper; }

    private HttpSecurity common(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint((r, s, ex) -> error(r, s, 401, "UNAUTHORIZED", "Authentication required"))
                        .accessDeniedHandler((r, s, ex) -> error(r, s, 403, "FORBIDDEN", "Access denied")));
    }
    @Bean @Profile("!demo")
    SecurityFilterChain jwtSecurity(HttpSecurity http) throws Exception {
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("role"); roles.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(roles);
        return common(http).oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((r,s,ex) -> error(r,s,401,"UNAUTHORIZED","A valid bearer token is required"))
                .accessDeniedHandler((r,s,ex) -> error(r,s,403,"FORBIDDEN","Access denied"))).build();
    }
    @Bean @Profile("!demo")
    JwtDecoder jwtDecoder(@Value("${ridelink.security.jwt-secret}") String secret) {
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
    @Bean @Profile("demo")
    SecurityFilterChain demoSecurity(HttpSecurity http) throws Exception {
        return common(http).httpBasic(b -> b.authenticationEntryPoint((r,s,ex) -> error(r,s,401,"UNAUTHORIZED","Valid demo credentials required"))).build();
    }
    @Bean @Profile("demo")
    UserDetailsService demoUsers(@Value("${ridelink.demo-password}") String password) {
        if (password.isBlank()) throw new IllegalArgumentException("DEMO_PASSWORD must not be blank");
        String encoded = "{bcrypt}" + new BCryptPasswordEncoder().encode(password);
        return new InMemoryUserDetailsManager(
                User.withUsername("passenger-001").password(encoded).roles("PASSENGER").build(),
                User.withUsername("passenger-002").password(encoded).roles("PASSENGER").build(),
                User.withUsername("admin-demo").password(encoded).roles("ADMIN").build());
    }
    private void error(HttpServletRequest request, HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ApiError(Instant.now(), status, code, message, request.getRequestURI(), List.of()));
    }
}
