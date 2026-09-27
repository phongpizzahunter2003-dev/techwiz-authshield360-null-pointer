import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { DetailShell } from '../../components/layout/DetailShell.jsx'
import { Badge } from '../../components/ui/Badge.jsx'
import { ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { adminApi, dashboardApi } from '../../api/endpoints.js'

export const MODE_META = {
  S1: {
    icon: '🔑',
    name: 'S1 — Password only',
    tone: 'coral',
    summary: 'Baseline: users only need a username and password.',
    factors: ['PASSWORD'],
    flow: ['Enter username + password', 'The system verifies', 'Grant a session and apply role-based permissions'],
    otp: false,
    email: false,
    applies: [
      'Password hashed with BCrypt (work factor 12)',
      'Brute-force protection: temporary lockout after 5 consecutive failures',
      'Logs sign-ins, sign-outs, and access violations',
    ],
    notApplies: ['No second authentication factor', 'A leaked password alone is enough to gain access (baseline weakness)'],
  },
  S2: {
    icon: '📱',
    name: 'S2 — Password + Mobile OTP',
    tone: 'sun',
    summary: 'Adds a second verification layer with Mobile OTP (TOTP or simulated SMS).',
    factors: ['PASSWORD', 'MOBILE_OTP'],
    flow: ['Enter username + password', 'The system sends/generates a Mobile OTP', 'Enter the OTP', 'Grant a session and apply permissions'],
    otp: true,
    email: false,
    applies: [
      'All S1 policies',
      'OTPs are time-limited and single-use',
      'Wrong OTPs also count toward the lockout threshold (VD-04)',
      'Resend limits: 60 seconds each time, up to 3 times',
    ],
    notApplies: ['No email verification layer yet'],
  },
  S3: {
    icon: '📧',
    name: 'S3 — Password + Mobile OTP + Email OTP',
    tone: 'accent',
    summary: 'Sequential three-layer authentication: password → Mobile OTP → Email OTP.',
    factors: ['PASSWORD', 'MOBILE_OTP', 'EMAIL_OTP'],
    flow: [
      'Enter username + password',
      'Verify the Mobile OTP',
      'The system sends an Email OTP to the test mailbox',
      'Verify the Email OTP',
      'Grant a session and apply role-based permissions',
    ],
    otp: true,
    email: true,
    applies: [
      'All S2 policies',
      'Email OTPs are time-limited; resend up to 3 times, then restart the flow (VD-07)',
      'Requires SMTP configuration for the test mailbox',
    ],
    notApplies: [],
  },
}

/** Distinct detail page per authentication mode (S1 / S2 / S3), reachable from the dashboards. */
export function AdminAuthMode() {
  const { mode: rawMode } = useParams()
  const mode = (rawMode || 'S1').toUpperCase()
  const meta = MODE_META[mode] || MODE_META.S1

  const toast = useToast()
  const comparison = useAsync(() => dashboardApi.comparison(), [mode])
  const config = useAsync(() => adminApi.config(), [])
  const [applying, setApplying] = useState(false)

  const metrics = (comparison.data?.data?.modes || []).find((m) => m.mode === mode)
  const configData = config.data?.data
  const isActive = configData?.mode === mode

  const applyMode = async () => {
    setApplying(true)
    try {
      await adminApi.updateConfig({ mode })
      toast.success(`Mode ${mode} applied. The system will use it on the next sign-in.`)
      await config.reload()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setApplying(false)
    }
  }

  const loading = comparison.loading || config.loading
  const error = comparison.error || config.error

  return (
    <DetailShell
      title={meta.name}
      subtitle={meta.summary}
      fallback="/admin/comparison"
      actions={
        <div className="flex flex-wrap gap-2">
          <Link className="btn-ghost" to={`/admin/audit-logs?mode=${mode}`}>
            📜 Logs for this mode
          </Link>
          <Link className="btn-ghost" to={`/admin/config?mode=${mode}`}>
            ⚙️ Detailed configuration
          </Link>
          <button type="button" className="btn-primary" onClick={applyMode} disabled={applying || isActive}>
            {applying ? <InlineSpinner /> : '✓'} {isActive ? 'In use' : 'Apply this mode'}
          </button>
        </div>
      }
    >
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={() => { comparison.reload(); config.reload() }} /> : null}

      {!loading && !error ? (
        <div className="space-y-6">
          <section className="card">
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-3xl" aria-hidden="true">
                {meta.icon}
              </span>
              <Badge tone={meta.tone}>{mode}</Badge>
              {isActive ? <Badge tone="brand">In use</Badge> : <Badge tone="neutral">Not activated</Badge>}
              {meta.factors.map((f) => (
                <Badge key={f} tone="sky">
                  {f}
                </Badge>
              ))}
            </div>
            <p className="mt-3 text-sm text-ink-600">{meta.summary}</p>

            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div className="rounded-xl bg-surface-soft p-3">
                <p className="text-xs font-bold uppercase text-ink-400">Security level</p>
                <p className="mt-1 text-sm font-semibold text-ink-900">{metrics?.security || '—'}</p>
              </div>
              <div className="rounded-xl bg-surface-soft p-3">
                <p className="text-xs font-bold uppercase text-ink-400">Usability</p>
                <p className="mt-1 text-sm font-semibold text-ink-900">{metrics?.usability || '—'}</p>
              </div>
              <div className="rounded-xl bg-surface-soft p-3">
                <p className="text-xs font-bold uppercase text-ink-400">Risk reduced</p>
                <p className="mt-1 text-sm font-semibold text-ink-900">{metrics?.riskReduced || '—'}</p>
              </div>
            </div>
          </section>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <section className="card">
              <h2 className="text-base font-bold text-ink-900">Sign-in flow</h2>
              <ol className="mt-3 space-y-2">
                {meta.flow.map((step, index) => (
                  <li key={step} className="flex items-start gap-3 text-sm text-ink-600">
                    <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-brand-100 text-xs font-bold text-brand-700">
                      {index + 1}
                    </span>
                    {step}
                  </li>
                ))}
              </ol>
            </section>

            <section className="card">
              <h2 className="text-base font-bold text-ink-900">Measured metrics</h2>
              <p className="mt-1 text-xs text-ink-400">
                Aggregated from <code className="font-mono">audit_logs</code> (BR-09: at least 3 runs per mode).
              </p>
              <div className="mt-3 grid grid-cols-3 gap-2 text-center">
                <div className="rounded-xl bg-accent-100 p-3">
                  <p className="text-xl font-extrabold text-accent-600">{metrics?.loginSuccesses ?? 0}</p>
                  <p className="text-[11px] text-ink-600">Successful sign-ins</p>
                </div>
                <div className="rounded-xl bg-coral-100 p-3">
                  <p className="text-xl font-extrabold text-coral-600">{metrics?.loginFailures ?? 0}</p>
                  <p className="text-[11px] text-ink-600">Failed sign-ins</p>
                </div>
                <div className="rounded-xl bg-sun-100 p-3">
                  <p className="text-xl font-extrabold text-sun-600">{metrics?.otpFailures ?? 0}</p>
                  <p className="text-[11px] text-ink-600">OTP errors</p>
                </div>
              </div>
              <Link className="btn-ghost mt-3 w-full" to={`/admin/audit-logs?mode=${mode}`}>
                View logs for mode {mode} →
              </Link>
            </section>
          </div>

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Configuration applied to {mode}</h2>
            {configData ? (
              <dl className="mt-3 grid grid-cols-1 gap-x-8 gap-y-2 text-sm sm:grid-cols-2">
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">OTP type</dt>
                  <dd className="font-semibold text-ink-900">{meta.otp ? configData.otpType : '— (not used)'}</dd>
                </div>
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">Code length</dt>
                  <dd className="font-semibold text-ink-900">{meta.otp ? configData.otpLength : '—'}</dd>
                </div>
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">Validity</dt>
                  <dd className="font-semibold text-ink-900">
                    {meta.otp ? `${configData.otpValiditySeconds} seconds` : '—'}
                  </dd>
                </div>
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">Resend cooldown / max resends</dt>
                  <dd className="font-semibold text-ink-900">
                    {meta.otp ? `${configData.resendCooldownSeconds}s / ${configData.maxResend}` : '—'}
                  </dd>
                </div>
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">Email OTP</dt>
                  <dd className="font-semibold text-ink-900">
                    {meta.email ? (configData.emailOtpEnabled ? 'Enabled' : 'Disabled') : '— (not used)'}
                  </dd>
                </div>
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">SMTP</dt>
                  <dd className="max-w-[60%] truncate font-semibold text-ink-900">
                    {meta.email ? `${configData.smtpHost || '—'}:${configData.smtpPort || '—'}` : '— (not used)'}
                  </dd>
                </div>
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">Account lockout threshold</dt>
                  <dd className="font-semibold text-ink-900">
                    {configData.maxFailedAttempts} attempts → {configData.lockoutDurationsSeconds}
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-ink-400">Captcha after N OTP requests</dt>
                  <dd className="font-semibold text-ink-900">
                    {meta.otp ? configData.requireCaptchaAfter : '—'}
                  </dd>
                </div>
              </dl>
            ) : null}
          </section>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <section className="card">
              <h2 className="text-base font-bold text-ink-900">Applies in this mode</h2>
              <ul className="mt-3 space-y-2 text-sm text-ink-600">
                {meta.applies.map((item) => (
                  <li key={item} className="flex gap-2">
                    <span aria-hidden="true">✅</span>
                    {item}
                  </li>
                ))}
              </ul>
            </section>
            <section className="card">
              <h2 className="text-base font-bold text-ink-900">Not applicable</h2>
              {meta.notApplies.length === 0 ? (
                <p className="mt-3 text-sm text-accent-600">
                  This mode applies every implemented protection layer.
                </p>
              ) : (
                <ul className="mt-3 space-y-2 text-sm text-ink-600">
                  {meta.notApplies.map((item) => (
                    <li key={item} className="flex gap-2">
                      <span aria-hidden="true">🚫</span>
                      {item}
                    </li>
                  ))}
                </ul>
              )}
              <p className="mt-3 rounded-xl bg-surface-soft px-3 py-2 text-xs text-ink-400">
                <strong>MFA enrollment</strong> is still done by each user individually (UC-09). Administrators can
                assign a mode to individual accounts on the Users &amp; roles page.
              </p>
            </section>
          </div>
        </div>
      ) : null}
    </DetailShell>
  )
}
