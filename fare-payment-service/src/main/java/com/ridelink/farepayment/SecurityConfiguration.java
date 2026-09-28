package com.ridelink.farepayment;


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
import static com.ridelink.farepayment.ApiModels.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    private final ObjectMapper mapper;
    public SecurityConfiguration(ObjectMapper mapper) { this.mapper = mapper; }

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
        roles.setAuthoritiesClaimName("roles"); roles.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(roles);
        return common(http).oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((r,s,ex) -> error(r,s,401,"UNAUTHORIZED","A valid bearer token is required"))
                .accessDeniedHandler((r,s,ex) -> error(r,s,403,"FORBIDDEN","Access denied"))).build();
    }
    @Bean @Profile("!demo")
    JwtDecoder jwtDecoder(@Value("${ridelink.security.jwk-set-uri}") String jwks,
            @Value("${ridelink.security.issuer}") String issuer,
            @Value("${ridelink.security.audience}") String audience) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
        OAuth2TokenValidator<Jwt> claims = jwt -> jwt.getAudience().contains(audience)
                && jwt.getSubject() != null && !jwt.getSubject().isBlank() && jwt.getExpiresAt() != null
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required token claims missing or invalid", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), claims));
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
