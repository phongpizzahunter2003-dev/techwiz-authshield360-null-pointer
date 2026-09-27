package com.authshield360.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simulated delivery channel for OTP codes (VĐ-06: no paid SMS).
 * In this lab the code is written to the application log and returned to the dev UI when
 * {@code authshield.expose-otp=true}. Production would delegate to an SMS/SMTP provider.
 */
@Component
public class MailGateway {

    private static final Logger log = LoggerFactory.getLogger(MailGateway.class);

    public void sendOtp(String destination, OtpFactor factor, String code, String subject) {
        String channel = factor == OtpFactor.EMAIL_OTP ? "EMAIL" : "SMS";
        log.info("[SIMULATED-{}] to={} subject='{}' code={}", channel, destination, subject, code);
    }

    public void sendPlain(String destination, String subject, String body) {
        log.info("[SIMULATED-EMAIL] to={} subject='{}' body={}", destination, subject, body);
    }
}
