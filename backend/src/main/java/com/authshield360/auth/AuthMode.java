package com.authshield360.auth;

/** The three authentication modes compared by the project. */
public enum AuthMode {
    /** S1 — password only. */
    S1,
    /** S2 — password + mobile OTP. */
    S2,
    /** S3 — password + mobile OTP + email OTP. */
    S3
}
