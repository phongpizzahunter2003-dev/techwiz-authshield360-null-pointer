/**
 * Minimal RFC 6238 TOTP (SHA-1, 6 digits, 30s) so the suite can log in with a real
 * authenticator code for the seeded MFA fixture `student_mfa01`
 * (secret JBSWY3DPEHPK3PXP — the RFC 6238 sample secret, lab only).
 */
const crypto = require('node:crypto')

const ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'

function base32Decode(input) {
  const clean = String(input).toUpperCase().replace(/=+$/, '').replace(/\s+/g, '')
  let bits = 0
  let value = 0
  const bytes = []
  for (const char of clean) {
    const idx = ALPHABET.indexOf(char)
    if (idx === -1) throw new Error(`Invalid base32 character: ${char}`)
    value = (value << 5) | idx
    bits += 5
    if (bits >= 8) {
      bits -= 8
      bytes.push((value >>> bits) & 0xff)
    }
  }
  return Buffer.from(bytes)
}

function totp(secret, { period = 30, digits = 6, atMs = Date.now() } = {}) {
  const counter = Math.floor(atMs / 1000 / period)
  const buf = Buffer.alloc(8)
  buf.writeUInt32BE(Math.floor(counter / 2 ** 32), 0)
  buf.writeUInt32BE(counter >>> 0, 4)
  const hmac = crypto.createHmac('sha1', base32Decode(secret)).update(buf).digest()
  const offset = hmac[hmac.length - 1] & 0x0f
  const bin =
    ((hmac[offset] & 0x7f) << 24) |
    ((hmac[offset + 1] & 0xff) << 16) |
    ((hmac[offset + 2] & 0xff) << 8) |
    (hmac[offset + 3] & 0xff)
  return String(bin % 10 ** digits).padStart(digits, '0')
}

module.exports = { totp, base32Decode }
