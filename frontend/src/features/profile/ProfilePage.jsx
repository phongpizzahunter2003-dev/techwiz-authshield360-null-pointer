import { useEffect, useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { Badge } from '../../components/ui/Badge.jsx'
import { ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { useAuth } from '../../context/AuthContext.jsx'
import { authApi } from '../../api/endpoints.js'
import { useAsync } from '../../hooks/useAsync.js'
import { ROLE_LABEL } from '../../i18n/messages.js'
import { formatDateTime } from '../../utils/format.js'
import { generateTotp, secondsRemaining } from '../../utils/totp.js'

export function ProfilePage() {
  const { session, logout } = useAuth()
  const toast = useToast()
  const current = useAsync(() => authApi.session(), [])

  const [enroll, setEnroll] = useState(null) // { secret, otpauthUri }
  const [code, setCode] = useState('')
  const [busy, setBusy] = useState(false)
  const [totpPreview, setTotpPreview] = useState('')
  const [seconds, setSeconds] = useState(secondsRemaining())

  // Live TOTP preview for the enrollment step (test-only convenience).
  useEffect(() => {
    if (!enroll?.secret) return undefined
    let alive = true
    const tick = async () => {
      const value = await generateTotp(enroll.secret)
      if (alive) {
        setTotpPreview(value)
        setSeconds(secondsRemaining())
      }
    }
    tick()
    const id = setInterval(tick, 1000)
    return () => {
      alive = false
      clearInterval(id)
    }
  }, [enroll?.secret])

  const copy = async (text, label) => {
    try {
      await navigator.clipboard.writeText(text)
      toast.success(`Copied ${label}.`)
    } catch {
      toast.error('Could not copy automatically. Please select and copy manually.')
    }
  }

  const startEnroll = async () => {
    setBusy(true)
    try {
      const res = await authApi.enrollMfa()
      setEnroll(res.data)
      toast.info('Scan the QR code with your authenticator app, then enter the 6-digit code to confirm.')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setBusy(false)
    }
  }

  const confirmEnroll = async (event) => {
    event.preventDefault()
    setBusy(true)
    try {
      await authApi.confirmMfa({ code: code.trim() })
      toast.success('Two-factor authentication set up successfully.')
      setEnroll(null)
      setCode('')
      await current.reload()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setBusy(false)
    }
  }

  const s = current.data?.data || session

  return (
    <AppShell title="Profile & security" subtitle="Session information and two-factor authentication (UC-09)">
      {current.loading ? <Spinner /> : null}
      {current.error ? <ErrorState message={current.error.message} onRetry={current.reload} /> : null}

      {s ? (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Current session</h2>
            <dl className="mt-4 space-y-3 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Username</dt>
                <dd className="font-semibold text-ink-900">{s.username}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Full name</dt>
                <dd className="font-semibold text-ink-900">{s.fullName || '—'}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Email</dt>
                <dd className="font-semibold text-ink-900">{s.email || '—'}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Role</dt>
                <dd>
                  <Badge tone="brand">{ROLE_LABEL[s.role] || s.role}</Badge>
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Authentication method</dt>
                <dd className="font-semibold text-ink-900">{s.authMethod || '—'}</dd>
              </div>
              <div className="flex items-start justify-between gap-3">
                <dt className="text-ink-400">Effective authentication mode</dt>
                <dd className="text-right">
                  <Badge tone="brand">{s.effectiveAuthMode || 'S1'}</Badge>
                  <p className="mt-1 text-xs text-ink-400">
                    {s.authModeOverride
                      ? 'Assigned by an administrator for this account'
                      : 'Uses the global system configuration'}
                  </p>
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Session ID</dt>
                <dd className="max-w-[220px] truncate font-mono text-xs text-ink-600">{s.sessionId}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Started</dt>
                <dd className="font-semibold text-ink-900">{formatDateTime(s.issuedAt)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Expires</dt>
                <dd className="font-semibold text-ink-900">{formatDateTime(s.expiresAt)}</dd>
              </div>
            </dl>
            <button type="button" className="btn-danger mt-5" onClick={logout}>
              ↩ Sign out
            </button>
            <p className="mt-2 text-xs text-ink-400">
              Signing out destroys the server-side session and clears browser history (UC-06).
            </p>
          </section>

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Two-factor authentication (MFA)</h2>
            {!enroll ? (
              <>
                <p className="mt-2 text-sm text-ink-600">
                  Set up an authenticator app (Google Authenticator, FreeOTP) to protect your account with TOTP.
                </p>
                <button type="button" className="btn-primary mt-4" onClick={startEnroll} disabled={busy}>
                  {busy ? <InlineSpinner /> : '🔐'} Start setup
                </button>
              </>
            ) : (
              <form onSubmit={confirmEnroll} className="mt-3 space-y-4">
                <ol className="list-decimal space-y-1 pl-5 text-sm text-ink-600">
                  <li>Open your authenticator app (Google Authenticator, Microsoft Authenticator, FreeOTP…).</li>
                  <li>
                    Choose <strong>“Scan QR code”</strong> <em>in the authenticator app</em> and then scan the code below.
                    Don't scan it with your phone's default camera app — the camera will say “open with an app only”.
                  </li>
                  <li>Enter the 6-digit code shown by the app to finish.</li>
                </ol>

                <div className="flex flex-col items-center gap-3 rounded-2xl bg-surface-soft p-4">
                  <div className="rounded-2xl bg-white p-3 shadow-soft">
                    <QRCodeSVG value={enroll.otpauthUri} size={168} />
                  </div>
                  <div className="w-full">
                    <p className="text-center text-xs font-bold uppercase text-ink-400">
                      Or enter the secret code manually
                    </p>
                    <div className="mt-1 flex items-center gap-2">
                      <code className="min-w-0 flex-1 break-all rounded-xl bg-white px-3 py-2 font-mono text-xs text-ink-900">
                        {enroll.secret}
                      </code>
                      <button
                        type="button"
                        className="btn-ghost !px-3 !py-2"
                        onClick={() => copy(enroll.secret, 'secret code')}
                        title="Copy secret code"
                      >
                        📋
                      </button>
                    </div>
                    <button
                      type="button"
                      className="btn-ghost mt-2 w-full !py-2 text-xs"
                      onClick={() => copy(enroll.otpauthUri, 'otpauth link')}
                    >
                      🔗 Copy the otpauth:// link (paste into your authenticator app)
                    </button>
                  </div>
                </div>

                <details className="rounded-xl border border-sky-300 bg-sky-100/50 px-3 py-2 text-xs text-ink-600">
                  <summary className="cursor-pointer font-semibold text-sky-600">
                    ❓ Can't scan the QR code? Open troubleshooting
                  </summary>
                  <div className="mt-2 space-y-1.5">
                    <p>
                      • The phone says <strong>“open with an app only”</strong>: the phone is opening the{' '}
                      <code className="font-mono">otpauth://</code> link with a browser/camera, so no app can handle
                      it. Scan the QR code <strong>directly in the authenticator app</strong>.
                    </p>
                    <p>
                      • Can't scan? Choose <strong>“Enter setup key”</strong> in the authenticator
                      app and then paste the <strong>secret code</strong> above.
                    </p>
                    <p>
                      • The code is always wrong even when entered correctly? Check whether your phone's{' '}
                      <strong>time</strong> is set to automatic — TOTP needs synchronized clocks.
                    </p>
                  </div>
                </details>

                <div className="rounded-2xl border border-sun-400 bg-sun-100/60 p-3 text-sm">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="font-semibold text-ink-900">
                      Current TOTP code (test environment only): <strong>{totpPreview}</strong>
                    </p>
                    <button
                      type="button"
                      className="btn-ghost !min-h-0 !px-2.5 !py-1.5 text-xs"
                      onClick={() => setCode(totpPreview)}
                      disabled={!totpPreview}
                    >
                      ⌨️ Fill in this code
                    </button>
                  </div>
                  <p className="text-xs text-ink-600">
                    The code changes in {seconds}s. In a real environment, the code is only shown on the device.
                  </p>
                </div>

                <div className="rounded-2xl border-2 border-brand-300 bg-brand-50/60 p-4">
                  <p className="flex items-center gap-2 text-sm font-bold text-ink-900">
                    <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-brand-500 text-xs text-white">
                      4
                    </span>
                    Enter the 6-digit code from your authenticator app here
                  </p>
                  <p className="mt-1 text-xs text-ink-600">
                    Open <strong>Google Authenticator</strong> → select <strong>AuthShield 360</strong> → type the
                    6-digit code shown into the field below. The code refreshes every 30 seconds.
                  </p>
                  <label className="label mt-3" htmlFor="txt-mfa-code">
                    Confirmation code (6 digits)
                  </label>
                  <input
                    id="txt-mfa-code"
                    className="input text-center text-2xl font-bold tracking-[0.5em]"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    maxLength={6}
                    autoFocus
                    value={code}
                    onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    placeholder="123456"
                  />
                  <p className="mt-2 text-xs text-ink-400">
                    Entered: <strong>{code ? `${code.length}/6` : '0/6'}</strong> digits
                    {code.length === 6 ? ' — press “Confirm” below.' : ''}
                  </p>
                </div>

                <div className="flex justify-end gap-2">
                  <button type="button" className="btn-ghost" onClick={() => setEnroll(null)}>
                    Cancel
                  </button>
                  <button type="submit" className="btn-primary" disabled={busy || code.length !== 6}>
                    {busy ? <InlineSpinner /> : '✓'} Confirm
                  </button>
                </div>
              </form>
            )}
          </section>
        </div>
      ) : null}
    </AppShell>
  )
}
