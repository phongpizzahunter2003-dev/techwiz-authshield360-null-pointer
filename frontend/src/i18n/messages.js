// Canonical user-facing messages (the API returns the same wording).
export const M = {
  LOGIN_INVALID: 'Incorrect username or password. Please try again.',
  LOCKOUT_ACTIVE: 'Your account has been temporarily locked after too many failed attempts.',
  OTP_INVALID: 'The verification code is incorrect. Please check and try again.',
  OTP_EXPIRED: 'The verification code has expired. Please request a new one.',
  OTP_RESENT: 'A new verification code has been sent.',
  RESEND_TOO_SOON: 'Too many requests. Please wait before requesting another code.',
  RESEND_LIMIT: 'You have requested too many codes. Please start the sign-in process again.',
  FORBIDDEN: 'You do not have permission to access this resource. This attempt has been logged.',
  SESSION_EXPIRED: 'Your session has expired. Please sign in again.',
  LOGOUT_OK: 'You have signed out successfully.',
  CAPTCHA_REQUIRED: 'Please complete the challenge to confirm you are not a robot.',
  EXPORT_LIMIT:
    'The result set exceeds 10,000 records. The newest 10,000 records will be exported. Narrow the date range or filters to export everything.',
  GENERIC: 'Something went wrong. Please try again.',
}

export const ROLE_LABEL = {
  STUDENT: 'Student',
  TEACHER: 'Teacher',
  ADMIN: 'Administrator',
}

export const ROLE_HOME = {
  STUDENT: '/student',
  TEACHER: '/teacher',
  ADMIN: '/admin',
}

export const AUTH_MODE_LABEL = {
  S1: 'S1 · Password only',
  S2: 'S2 · Password + Mobile OTP',
  S3: 'S3 · Password + Mobile + Email OTP',
  INHERIT: 'Use global configuration',
}
