package com.authshield360;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * AuthShield 360 backend entry point.
 *
 * <p>The default in-memory {@code UserDetailsService} is excluded: authentication is handled by the
 * custom {@code JwtAuthFilter} + server-side session registry, so the auto-configured demo user is
 * unnecessary (and undesirable in a security-focused application).
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
