import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import { Modal } from '../ui/Modal.jsx'
import { ROLE_LABEL } from '../../i18n/messages.js'

const DISMISS_KEY = 'mfa_prompt_dismissed'

/**
 * UC-09: after the first successful password login, a user whose role requires MFA
 * but who has not enrolled yet is asked to enrol. Each portal role enrols MFA for
 * themselves via the QR code — the administrator never creates it for them
 * (admin only enables MFA in UC-08 and may reset it in UC-16).
 */
export function MfaEnrollmentPrompt() {
  const { session } = useAuth()
  const navigate = useNavigate()
  const [dismissed, setDismissed] = useState(
    () => typeof sessionStorage !== 'undefined' && sessionStorage.getItem(DISMISS_KEY) === '1',
  )

  const required = Boolean(session && session.mfaEnabled && !session.mfaEnrolled)
  if (!required || dismissed) return null

  const dismiss = () => {
    sessionStorage.setItem(DISMISS_KEY, '1')
    setDismissed(true)
  }

  return (
    <Modal
      open
      title="Set up two-factor authentication (UC-09)"
      onClose={dismiss}
      footer={
        <>
          <button type="button" className="btn-ghost" onClick={dismiss}>
            Later
          </button>
          <button
            type="button"
            className="btn-primary"
            onClick={() => {
              dismiss()
              navigate('/profile')
            }}
          >
            🔐 Set up now
          </button>
        </>
      }
    >
      <p className="text-sm text-ink-600">
        The system has <strong>enabled two-factor authentication</strong> for the role{' '}
        <strong>{ROLE_LABEL[session.role] || session.role}</strong>, but your account has not enrolled a second factor yet.
      </p>
      <p className="mt-3 text-sm text-ink-600">
        Per UC-09, after your first successful password sign-in you need to <strong>enrol yourself</strong> an MFA factor: scan the QR code
        with an authenticator app (Google Authenticator, FreeOTP) and enter the 6-digit code to confirm.
      </p>
      <p className="mt-3 rounded-xl bg-surface-soft px-3 py-2 text-xs text-ink-400">
        Each user sets up MFA for themselves. The administrator only configures the authentication mode (UC-08) and can reset
        MFA when needed (UC-16) — they do not create the QR code on the user's behalf.
      </p>
    </Modal>
  )
}
