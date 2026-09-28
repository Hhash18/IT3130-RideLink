package com.ridelink.farepayment.config;

import com.ridelink.farepayment.entity.Fare;
import com.ridelink.farepayment.entity.Payment;

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
        return new OpenAPI().info(new Info().title("RideLink Fare & Payment API").version("1.0.0")
                .description("Member 4 backend. Currency LKR. Payments and trip distances are simulated. "
                        + (demo ? "Demo fixtures and HTTP Basic authentication are active." : "Account-issued JWT authentication is active.")))
                .components(new Components().addSecuritySchemes(name, scheme))
                .addSecurityItem(new SecurityRequirement().addList(name));
    }
}
