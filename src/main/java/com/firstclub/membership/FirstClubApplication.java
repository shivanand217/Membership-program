package com.firstclub.membership;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the FirstClub Membership Program backend.
 *
 * <p>Enables:
 * <ul>
 *   <li>{@code @EnableScheduling} — the subscription expiry/renewal sweep.</li>
 *   <li>{@code @EnableRetry} — bounded retry around optimistic-lock conflicts on lifecycle use cases.</li>
 *   <li>{@code @ConfigurationPropertiesScan} — binds {@code membership.*} properties.</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
@EnableRetry
@ConfigurationPropertiesScan
public class FirstClubApplication {

    public static void main(String[] args) {
        SpringApplication.run(FirstClubApplication.class, args);
    }
}
