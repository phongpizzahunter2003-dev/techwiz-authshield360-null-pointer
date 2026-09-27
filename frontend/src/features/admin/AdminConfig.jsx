import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { Badge } from '../../components/ui/Badge.jsx'
import { ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { adminApi } from '../../api/endpoints.js'

const MODE_CARDS = [
  {
    id: 'S1',
    title: 'S1 — Password only',
    desc: 'Baseline: users only need a username and password.',
    icon: '🔑',
  },
  {
    id: 'S2',
    title: 'S2 — Password + OTP',
    desc: 'Requires an additional Mobile OTP (TOTP or simulated SMS).',
    icon: '📱',
  },
  {
    id: 'S3',
    title: 'S3 — Password + Mobile OTP + Email OTP',
    desc: 'Adds an email verification layer before granting access.',
    icon: '📧',
  },
]

export function AdminConfig() {
  const toast = useToast()
  const [searchParams] = useSearchParams()
  const [config, setConfig] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [smtpPassword, setSmtpPassword] = useState('')

  useEffect(() => {
    let mounted = true
    adminApi
      .config()
      .then((res) => {
        if (!mounted) return
        const loaded = res.data
        const preselect = (searchParams.get('mode') || '').toUpperCase()
        if (['S1', 'S2', 'S3'].includes(preselect)) loaded.mode = preselect
        setConfig(loaded)
      })
      .catch((err) => mounted && setError(err))
      .finally(() => mounted && setLoading(false))
    return () => {
      mounted = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const save = async (event) => {
    event.preventDefault()
    setSaving(true)
    try {
      const payload = {
        mode: config.mode,
        otpType: config.otpType,
        otpLength: Number(config.otpLength),
        otpValiditySeconds: Number(config.otpValiditySeconds),
        resendCooldownSeconds: Number(config.resendCooldownSeconds),
        maxResend: Number(config.maxResend),
        maxFailedAttempts: Number(config.maxFailedAttempts),
        lockoutDurationsSeconds: config.lockoutDurationsSeconds,
        requireCaptchaAfter: Number(config.requireCaptchaAfter),
        smtpHost: config.smtpHost,
        smtpPort: config.smtpPort ? Number(config.smtpPort) : null,
        smtpUsername: config.smtpUsername,
        smtpFrom: config.smtpFrom,
        emailOtpEnabled: config.emailOtpEnabled,
      }
      if (smtpPassword) payload.smtpPassword = smtpPassword
      const res = await adminApi.updateConfig(payload)
      setConfig(res.data)
      setSmtpPassword('')
      toast.success('Configuration saved. The system will apply it on the next sign-in.')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  const set = (key, value) => setConfig((c) => ({ ...c, [key]: value }))

  const mode = config?.mode
  const otpApplies = mode !== 'S1'
  const emailApplies = mode === 'S3'

  return (
    <AppShell title="Auth configuration & MFA" subtitle="Switch between S1/S2/S3, OTP parameters, lockout and SMTP (UC-08)">
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} /> : null}

      {config ? (
        <form onSubmit={save} className="space-y-6">
          <section>
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-base font-bold text-ink-900">Authentication mode</h2>
              {mode ? (
                <span className="text-xs text-ink-400">
                  Selected: <strong className="text-ink-900">{mode}</strong> ·{' '}
                  <Link className="font-semibold text-brand-600 hover:underline" to={`/admin/modes/${mode}`}>
                    View mode {mode} details →
                  </Link>
                </span>
              ) : null}
            </div>
            <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
              {MODE_CARDS.map((m) => {
                const active = config.mode === m.id
                return (
                  <div
                    key={m.id}
                    className={`card transition ${
                      active ? 'ring-2 ring-brand-500 shadow-glow' : 'hover:shadow-soft'
                    }`}
                  >
                    <button type="button" onClick={() => set('mode', m.id)} className="w-full text-left">
                      <div className="flex items-center justify-between">
                        <span className="text-2xl" aria-hidden="true">
                          {m.icon}
                        </span>
                        {active ? <span className="badge bg-brand-100 text-brand-700">In use</span> : null}
                      </div>
                      <p className="mt-2 text-sm font-bold text-ink-900">{m.title}</p>
                      <p className="mt-1 text-xs text-ink-400">{m.desc}</p>
                    </button>
                    <Link
                      to={`/admin/modes/${m.id}`}
                      className="mt-3 inline-block text-xs font-semibold text-brand-600 hover:underline"
                    >
                      Mode {m.id} details →
                    </Link>
                  </div>
                )
              })}
            </div>
            <p className="mt-3 rounded-xl bg-surface-soft px-3 py-2 text-xs text-ink-400">
              The mode here is <strong>system-wide default</strong>. To assign it to individual users or
              in bulk, go to <Link className="font-semibold text-brand-600 hover:underline" to="/admin/users">Users &amp; roles</Link>.
            </p>
          </section>

          <section className={`card transition ${otpApplies ? '' : 'opacity-60'}`}>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-base font-bold text-ink-900">OTP parameters</h2>
              <Badge tone={otpApplies ? 'accent' : 'neutral'}>
                {otpApplies ? 'Applies to ' + mode : 'Not applicable to S1'}
              </Badge>
            </div>
            {!otpApplies ? (
              <p className="mt-2 rounded-xl bg-sun-100/60 px-3 py-2 text-xs text-ink-600">
                S1 uses passwords only, so the OTP parameters below have no effect. Choose S2 or S3 to use them.
              </p>
            ) : null}
            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <div>
                <label className="label" htmlFor="otp-type">
                  OTP type
                </label>
                <select id="otp-type" className="input" value={config.otpType} onChange={(e) => set('otpType', e.target.value)}>
                  <option value="TOTP">TOTP (authenticator app)</option>
                  <option value="SMS_SIMULATED">Simulated SMS</option>
                  <option value="EMAIL">Email</option>
                </select>
              </div>
              <div>
                <label className="label" htmlFor="otp-len">
                  Code length
                </label>
                <input
                  id="otp-len"
                  type="number"
                  min="4"
                  max="8"
                  className="input"
                  value={config.otpLength}
                  onChange={(e) => set('otpLength', e.target.value)}
                />
              </div>
              <div>
                <label className="label" htmlFor="otp-valid">
                  Validity (seconds, 30–300)
                </label>
                <input
                  id="otp-valid"
                  type="number"
                  min="30"
                  max="300"
                  className="input"
                  value={config.otpValiditySeconds}
                  onChange={(e) => set('otpValiditySeconds', e.target.value)}
                />
              </div>
              <div>
                <label className="label" htmlFor="otp-cooldown">
                  Resend cooldown (seconds)
                </label>
                <input
                  id="otp-cooldown"
                  type="number"
                  min="10"
                  max="600"
                  className="input"
                  value={config.resendCooldownSeconds}
                  onChange={(e) => set('resendCooldownSeconds', e.target.value)}
                />
              </div>
              <div>
                <label className="label" htmlFor="otp-maxresend">
                  Max resends
                </label>
                <input
                  id="otp-maxresend"
                  type="number"
                  min="0"
                  max="10"
                  className="input"
                  value={config.maxResend}
                  onChange={(e) => set('maxResend', e.target.value)}
                />
              </div>
              <label className="flex items-center gap-2 self-end text-sm">
                <input
                  type="checkbox"
                  checked={config.emailOtpEnabled}
                  onChange={(e) => set('emailOtpEnabled', e.target.checked)}
                />
                Enable Email OTP for S3
              </label>
            </div>
          </section>

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Brute-force protection (lockout)</h2>
            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div>
                <label className="label" htmlFor="max-fail">
                  Failed-attempt threshold
                </label>
                <input
                  id="max-fail"
                  type="number"
                  min="1"
                  max="50"
                  className="input"
                  value={config.maxFailedAttempts}
                  onChange={(e) => set('maxFailedAttempts', e.target.value)}
                />
              </div>
              <div>
                <label className="label" htmlFor="lock-ladder">
                  Lockout durations (seconds, comma separated)
                </label>
                <input
                  id="lock-ladder"
                  className="input"
                  value={config.lockoutDurationsSeconds}
                  onChange={(e) => set('lockoutDurationsSeconds', e.target.value)}
                />
              </div>
              <div>
                <label className="label" htmlFor="captcha-after">
                  Require captcha after N OTP requests
                </label>
                <input
                  id="captcha-after"
                  type="number"
                  min="0"
                  max="20"
                  className="input"
                  value={config.requireCaptchaAfter}
                  onChange={(e) => set('requireCaptchaAfter', e.target.value)}
                />
              </div>
            </div>
          </section>

          <section className={`card transition ${emailApplies ? '' : 'opacity-60'}`}>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-base font-bold text-ink-900">SMTP (Email OTP)</h2>
              <Badge tone={emailApplies ? 'accent' : 'neutral'}>
                {emailApplies ? 'Applies to S3' : 'Only used by S3'}
              </Badge>
            </div>
            <p className="mt-1 text-xs text-ink-400">
              This is the <strong>mail-sending service</strong> account used to send Email OTP codes at step 3 of mode S3
              — not a portal sign-in account. The password is encrypted with AES-GCM when stored and is never returned
              through the API (BR-10).
            </p>
            {!emailApplies ? (
              <p className="mt-2 rounded-xl bg-sun-100/60 px-3 py-2 text-xs text-ink-600">
                Only mode <strong>S3</strong> uses Email OTP. In {mode}, this configuration is kept but
                has no effect.
              </p>
            ) : null}
            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <div>
                <label className="label" htmlFor="smtp-host">
                  Host
                </label>
                <input
                  id="smtp-host"
                  className="input"
                  value={config.smtpHost || ''}
                  onChange={(e) => set('smtpHost', e.target.value)}
                />
              </div>
              <div>
                <label className="label" htmlFor="smtp-port">
                  Port
                </label>
                <input
                  id="smtp-port"
                  type="number"
                  className="input"
                  value={config.smtpPort || ''}
                  onChange={(e) => set('smtpPort', e.target.value)}
                />
              </div>
              <div>
                <label className="label" htmlFor="smtp-user">
                  Account (SMTP username)
                </label>
                <input
                  id="smtp-user"
                  className="input"
                  value={config.smtpUsername || ''}
                  onChange={(e) => set('smtpUsername', e.target.value)}
                  placeholder="e.g. 1a2b3c4d5e6f7g (Mailtrap)"
                />
                <p className="mt-1 text-[11px] text-ink-400">
                  The account issued by your SMTP provider, used to authenticate when sending mail. It must be from
                  the same provider as the “Host” above.
                </p>
              </div>
              <div>
                <label className="label" htmlFor="smtp-pass">
                  Password / API key
                </label>
                <input
                  id="smtp-pass"
                  type="password"
                  className="input"
                  placeholder={config.smtpPasswordSet ? '•••••••• (already set — enter to replace)' : '••••••••'}
                  value={smtpPassword}
                  onChange={(e) => setSmtpPassword(e.target.value)}
                />
                <p className="mt-1 text-[11px] text-ink-400">
                  {config.smtpPasswordSet
                    ? 'A password is already saved (encrypted). Leave blank to keep it unchanged.'
                    : 'Not set. It cannot be viewed again after saving.'}
                </p>
              </div>
              <div className="sm:col-span-2">
                <label className="label" htmlFor="smtp-from">
                  From address
                </label>
                <input
                  id="smtp-from"
                  className="input"
                  value={config.smtpFrom || ''}
                  onChange={(e) => set('smtpFrom', e.target.value)}
                />
              </div>
            </div>
          </section>

          <div className="flex items-center justify-between gap-3">
            <p className="text-xs text-ink-400">
              Last updated by <strong>{config.updatedBy || '—'}</strong>
            </p>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? <InlineSpinner /> : '💾'} Save configuration
            </button>
          </div>
        </form>
      ) : null}
    </AppShell>
  )
}
