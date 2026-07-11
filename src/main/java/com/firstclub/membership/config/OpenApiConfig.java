package com.firstclub.membership.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI membershipOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FirstClub Membership API")
                .version("v1")
                .description("Subscription-based memberships with configurable, tiered benefits. "
                        + "Plans define billing cadence + price; tiers define benefit level + progression criteria; "
                        + "a subscription binds a user to a plan and tier with a full lifecycle."));
    }
}
