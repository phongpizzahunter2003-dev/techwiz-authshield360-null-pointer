package com.authshield360.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findByUserIdentifierAndFactor(String userIdentifier, OtpFactor factor);
}
