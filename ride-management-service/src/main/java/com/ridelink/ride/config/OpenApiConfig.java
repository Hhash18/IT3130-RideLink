package com.ridelink.ride.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI openAPI(Environment environment) {
        boolean demo = environment.acceptsProfiles(Profiles.of("demo"));
        String name = demo ? "demoBasic" : "bearerAuth";
        SecurityScheme scheme = new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme(demo ? "basic" : "bearer");
        if (!demo) scheme.bearerFormat("JWT");
        return new OpenAPI().info(new Info().title("RideLink Ride Management API").version("1.0.0")
                .description("Member 3 backend. Ride lifecycle, simulated locations and driver assignment. "
                        + (demo ? "Demo fixtures and HTTP Basic authentication are active." : "Account-issued JWT authentication is active.")))
                .components(new Components().addSecuritySchemes(name, scheme))
                .addSecurityItem(new SecurityRequirement().addList(name));
    }
}
