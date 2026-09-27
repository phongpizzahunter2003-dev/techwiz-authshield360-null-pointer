// Minimal RFC 6238 TOTP generator for the lab UI.
// Lets a tester complete MFA enrollment / login where no phone is available (VD-06).
// Clearly labelled as a test-only convenience in the UI.

const BASE32 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'

function base32Decode(input) {
  const clean = (input || '').replace(/=+$/, '').toUpperCase()
  let bits = 0
  let value = 0
  const bytes = []
  for (const char of clean) {
    const idx = BASE32.indexOf(char)
    if (idx === -1) continue
    value = (value << 5) | idx
    bits += 5
    if (bits >= 8) {
      bytes.push((value >>> (bits - 8)) & 0xff)
      bits -= 8
    }
  }
  return new Uint8Array(bytes)
}

export async function generateTotp(secret, digits = 6, period = 30) {
  if (!secret) return ''
  const keyBytes = base32Decode(secret)
  const counter = Math.floor(Date.now() / 1000 / period)
  const message = new Uint8Array(8)
  let temp = counter
  for (let i = 7; i >= 0; i -= 1) {
    message[i] = temp & 0xff
    temp = Math.floor(temp / 256)
  }
  const key = await crypto.subtle.importKey('raw', keyBytes, { name: 'HMAC', hash: 'SHA-1' }, false, ['sign'])
  const signature = new Uint8Array(await crypto.subtle.sign('HMAC', key, message))
  const offset = signature[signature.length - 1] & 0x0f
  const binary =
    ((signature[offset] & 0x7f) << 24) |
    ((signature[offset + 1] & 0xff) << 16) |
    ((signature[offset + 2] & 0xff) << 8) |
    (signature[offset + 3] & 0xff)
  const otp = binary % 10 ** digits
  return String(otp).padStart(digits, '0')
}

export function secondsRemaining(period = 30) {
  return period - (Math.floor(Date.now() / 1000) % period)
}
