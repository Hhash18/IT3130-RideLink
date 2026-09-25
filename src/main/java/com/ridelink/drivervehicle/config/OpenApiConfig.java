package com.ridelink.drivervehicle.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "RideLink Driver & Vehicle Service API",
                version = "1.0",
                description = "REST API for managing drivers and vehicles in the RideLink system.",
                contact = @Contact(
                        name = "RideLink Development Team"
                )
        )
)
public class OpenApiConfig {
}