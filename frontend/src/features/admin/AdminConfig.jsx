import { useEffect, useState } from 'react'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { adminApi } from '../../api/endpoints.js'

const MODE_CARDS = [
  {
    id: 'S1',
    title: 'S1 — Chỉ mật khẩu',
    desc: 'Baseline: chỉ cần username + mật khẩu.',
    icon: '🔑',
  },
  {
    id: 'S2',
    title: 'S2 — Mật khẩu + OTP',
    desc: 'Yêu cầu thêm Mobile OTP (TOTP hoặc mô phỏng SMS).',
    icon: '📱',
  },
  {
    id: 'S3',
    title: 'S3 — Mật khẩu + Mobile OTP + Email OTP',
    desc: 'Thêm lớp xác minh qua email trước khi cấp quyền.',
    icon: '📧',
  },
]

export function AdminConfig() {
  const toast = useToast()
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
        if (mounted) setConfig(res.data)
      })
      .catch((err) => mounted && setError(err))
      .finally(() => mounted && setLoading(false))
    return () => {
      mounted = false
    }
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
      toast.success('Cấu hình đã được lưu. Hệ thống sẽ áp dụng ở lần đăng nhập tiếp theo.')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  const set = (key, value) => setConfig((c) => ({ ...c, [key]: value }))

  return (
    <AppShell title="Cấu hình xác thực & MFA" subtitle="Chuyển đổi S1/S2/S3, tham số OTP, lockout và SMTP (UC-08)">
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} /> : null}

      {config ? (
        <form onSubmit={save} className="space-y-6">
          <section>
            <h2 className="mb-3 text-base font-bold text-ink-900">Chế độ xác thực</h2>
            <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
              {MODE_CARDS.map((m) => {
                const active = config.mode === m.id
                return (
                  <button
                    key={m.id}
                    type="button"
                    onClick={() => set('mode', m.id)}
                    className={`card text-left transition ${
                      active ? 'ring-2 ring-brand-500 shadow-glow' : 'hover:shadow-soft'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-2xl" aria-hidden="true">
                        {m.icon}
                      </span>
                      {active ? <span className="badge bg-brand-100 text-brand-700">Đang dùng</span> : null}
                    </div>
                    <p className="mt-2 text-sm font-bold text-ink-900">{m.title}</p>
                    <p className="mt-1 text-xs text-ink-400">{m.desc}</p>
                  </button>
                )
              })}
            </div>
          </section>

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Tham số OTP</h2>
            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <div>
                <label className="label" htmlFor="otp-type">
                  Loại OTP
                </label>
                <select id="otp-type" className="input" value={config.otpType} onChange={(e) => set('otpType', e.target.value)}>
                  <option value="TOTP">TOTP (ứng dụng xác thực)</option>
                  <option value="SMS_SIMULATED">SMS mô phỏng</option>
                  <option value="EMAIL">Email</option>
                </select>
              </div>
              <div>
                <label className="label" htmlFor="otp-len">
                  Độ dài mã
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
                  Thời gian hiệu lực (giây, 30–300)
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
                  Thời gian chờ gửi lại (giây)
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
                  Số lần gửi lại tối đa
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
                Bật Email OTP cho chế độ S3
              </label>
            </div>
          </section>

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Chống dò mật khẩu (lockout)</h2>
            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div>
                <label className="label" htmlFor="max-fail">
                  Ngưỡng số lần sai
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
                  Thời gian khóa (giây, phân tách bởi dấu phẩy)
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
                  Yêu cầu captcha sau N lần gửi OTP
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

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">SMTP (Email OTP)</h2>
            <p className="mt-1 text-xs text-ink-400">
              Mật khẩu SMTP được mã hóa AES-GCM khi lưu và không bao giờ trả về qua API (BR-10).
            </p>
            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <div>
                <label className="label" htmlFor="smtp-host">
                  Máy chủ
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
                  Cổng
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
                  Tài khoản
                </label>
                <input
                  id="smtp-user"
                  className="input"
                  value={config.smtpUsername || ''}
                  onChange={(e) => set('smtpUsername', e.target.value)}
                />
              </div>
              <div>
                <label className="label" htmlFor="smtp-pass">
                  Mật khẩu {config.smtpPasswordSet ? '(đã thiết lập — nhập để đổi)' : ''}
                </label>
                <input
                  id="smtp-pass"
                  type="password"
                  className="input"
                  placeholder="••••••••"
                  value={smtpPassword}
                  onChange={(e) => setSmtpPassword(e.target.value)}
                />
              </div>
              <div className="sm:col-span-2">
                <label className="label" htmlFor="smtp-from">
                  Địa chỉ gửi
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
              Cập nhật lần cuối bởi <strong>{config.updatedBy || '—'}</strong>
            </p>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? <InlineSpinner /> : '💾'} Lưu cấu hình
            </button>
          </div>
        </form>
      ) : null}
    </AppShell>
  )
}
