package com.ridelink.ride.config;

import com.ridelink.ride.dto.ApiError;

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
    SecurityFilterChain jwtSecurity(HttpSecurity http, @Value("${ridelink.jwt-mode}") String mode) throws Exception {
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(mode.equals("account") ? "role" : "roles"); roles.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(roles);
        return common(http).oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((r,s,ex) -> error(r,s,401,"UNAUTHORIZED","A valid bearer token is required"))
                .accessDeniedHandler((r,s,ex) -> error(r,s,403,"FORBIDDEN","Access denied"))).build();
    }
    @Bean @Profile("!demo")
    JwtDecoder jwtDecoder(@Value("${ridelink.jwt-mode}") String mode,
            @Value("${ridelink.jwt-secret}") String secret,
            @Value("${ridelink.jwt-jwks}") String jwks,
            @Value("${ridelink.jwt-issuer}") String issuer,
            @Value("${ridelink.jwt-audience}") String audience) {
        NimbusJwtDecoder decoder;
        OAuth2TokenValidator<Jwt> baseValidator;
        if(mode.equals("account")) {
            byte[] bytes=secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if(bytes.length<32) throw new IllegalArgumentException("JWT_SECRET must match Account and be at least 32 UTF-8 bytes");
            var algorithm=bytes.length>=64 ? org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS512
                    : bytes.length>=48 ? org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS384
                    : org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256;
            decoder=NimbusJwtDecoder.withSecretKey(new javax.crypto.spec.SecretKeySpec(bytes,algorithm.getName().replace("HS","HmacSHA")))
                    .macAlgorithm(algorithm).build();
            baseValidator=JwtValidators.createDefault();
        } else if(mode.equals("jwks")) {
            decoder=NimbusJwtDecoder.withJwkSetUri(jwks).build();
            baseValidator=JwtValidators.createDefaultWithIssuer(issuer);
        } else throw new IllegalArgumentException("JWT_MODE must be account or jwks");
        OAuth2TokenValidator<Jwt> required=jwt -> jwt.getSubject()!=null && !jwt.getSubject().isBlank()
                && jwt.getSubject().length()<=100 && jwt.getExpiresAt()!=null
                && (!mode.equals("jwks") || jwt.getAudience().contains(audience))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Missing required identity, expiry or audience",null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(baseValidator,required));
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
                User.withUsername("driver@example.test").password(encoded).roles("DRIVER").build(),
                User.withUsername("other-driver@example.test").password(encoded).roles("DRIVER").build(),
                User.withUsername("admin-demo").password(encoded).roles("ADMIN").build());
    }
    private void error(HttpServletRequest request, HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ApiError(Instant.now(), status, code, message, request.getRequestURI(), List.of()));
    }
}
