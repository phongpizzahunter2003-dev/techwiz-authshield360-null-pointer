import { useEffect, useRef, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { InlineSpinner } from '../../components/ui/Spinner.jsx'
import { useCountdown } from '../../hooks/useCountdown.js'
import { M } from '../../i18n/messages.js'

const DEMO_ACCOUNTS = [
  { user: 'student01', pass: 'student123', role: 'Học sinh' },
  { user: 'teacher01', pass: 'teacher123', role: 'Giáo viên' },
  { user: 'admin01', pass: 'admin123', role: 'Quản trị viên' },
]

function parseLockSeconds(message) {
  const match = /(\d{2}):(\d{2})/.exec(message || '')
  if (!match) return 0
  return Number(match[1]) * 60 + Number(match[2])
}

export function LoginPage() {
  const { isAuthenticated, home, login, verifyOtp, resendOtp } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()

  const [step, setStep] = useState('credentials')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [code, setCode] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [resending, setResending] = useState(false)
  const [challenge, setChallenge] = useState(null) // { challengeToken, factor, expiresIn, deliveryCode, deliveryChannel, totpBased }
  const [captcha, setCaptcha] = useState(null) // { challengeId, question, answer }
  const [resendLimitHit, setResendLimitHit] = useState(false)

  const otpCountdown = useCountdown(0)
  const resendCountdown = useCountdown(0)
  const lockCountdown = useCountdown(0)

  const otpInputRef = useRef(null)

  useEffect(() => {
    if (step === 'otp') otpInputRef.current?.focus()
  }, [step, challenge?.factor])

  if (isAuthenticated) return <Navigate to={home} replace />

  const resetToCredentials = (message) => {
    setStep('credentials')
    setChallenge(null)
    setCode('')
    setResendLimitHit(false)
    setError(message || '')
  }

  const applyChallenge = (data, { restarted = false } = {}) => {
    setChallenge(data)
    setStep('otp')
    setCode('')
    otpCountdown.restart(data.expiresIn || 90)
    resendCountdown.restart(restarted ? 60 : 60)
    if (data.deliveryCode) {
      toast.info(`Mã OTP (môi trường thử nghiệm): ${data.deliveryCode}`)
    }
  }

  const handleLoginError = (err) => {
    if (err.code === 'CAPTCHA_REQUIRED' && err.fields) {
      setCaptcha({
        challengeId: err.fields.captchaChallengeId,
        question: err.fields.captchaQuestion,
        answer: '',
      })
      setError(M.CAPTCHA_REQUIRED)
      return
    }
    if (err.code === 'ACCOUNT_LOCKED') {
      const seconds = parseLockSeconds(err.message)
      if (seconds > 0) lockCountdown.restart(seconds)
      setError(err.message)
      return
    }
    setError(err.message || M.GENERIC)
  }

  const onSubmitCredentials = async (event) => {
    event.preventDefault()
    if (busy || lockCountdown.active) return
    setError('')
    setBusy(true)
    try {
      const captchaPayload = captcha ? { captchaChallengeId: captcha.challengeId, captchaAnswer: captcha.answer } : {}
      const data = await login(username.trim(), password, captchaPayload)
      if (data.status === 'AUTHENTICATED') {
        navigate(home, { replace: true })
      } else {
        setCaptcha(null)
        applyChallenge(data)
      }
    } catch (err) {
      handleLoginError(err)
    } finally {
      setBusy(false)
    }
  }

  const onSubmitOtp = async (event) => {
    event.preventDefault()
    if (busy) return
    setError('')
    setBusy(true)
    try {
      const data = await verifyOtp(challenge.challengeToken, code.trim())
      if (data.status === 'AUTHENTICATED') {
        navigate(home, { replace: true })
      } else {
        applyChallenge(data)
      }
    } catch (err) {
      if (err.code === 'OTP_EXPIRED') {
        setError(M.OTP_EXPIRED)
      } else if (err.code === 'OTP_INVALID') {
        setError(M.OTP_INVALID)
      } else if (err.code === 'ACCOUNT_LOCKED') {
        const seconds = parseLockSeconds(err.message)
        if (seconds > 0) lockCountdown.restart(seconds)
        setError(err.message)
      } else if (err.code === 'INVALID_CHALLENGE') {
        resetToCredentials('Phiên xác minh đã hết hạn. Vui lòng đăng nhập lại.')
      } else if (err.code === 'CAPTCHA_REQUIRED' && err.fields) {
        setCaptcha({
          challengeId: err.fields.captchaChallengeId,
          question: err.fields.captchaQuestion,
          answer: '',
        })
        setError(M.CAPTCHA_REQUIRED)
      } else {
        setError(err.message || M.GENERIC)
      }
    } finally {
      setBusy(false)
    }
  }

  const onResend = async () => {
    if (resending || resendCountdown.active || resendLimitHit) return
    setResending(true)
    setError('')
    try {
      const captchaPayload = captcha ? { captchaChallengeId: captcha.challengeId, captchaAnswer: captcha.answer } : {}
      const data = await resendOtp(challenge.challengeToken, captchaPayload)
      setCaptcha(null)
      applyChallenge(data, { restarted: true })
      toast.success(M.OTP_RESENT)
    } catch (err) {
      if (err.code === 'RESEND_THROTTLED') {
        toast.warning(M.RESEND_TOO_SOON)
      } else if (err.code === 'RESEND_LIMIT_EXCEEDED') {
        setResendLimitHit(true)
        setError(M.RESEND_LIMIT)
      } else if (err.code === 'CAPTCHA_REQUIRED' && err.fields) {
        setCaptcha({
          challengeId: err.fields.captchaChallengeId,
          question: err.fields.captchaQuestion,
          answer: '',
        })
        setError(M.CAPTCHA_REQUIRED)
      } else {
        setError(err.message || M.GENERIC)
      }
    } finally {
      setResending(false)
    }
  }

  const isEmailStep = challenge?.factor === 'EMAIL_OTP'
  const otpFieldId = isEmailStep ? 'txt-email-otp' : 'txt-mobile-otp'
  const verifyBtnId = isEmailStep ? 'btn-email-otp-verify' : 'btn-mobile-otp-verify'
  const resendBtnId = isEmailStep ? 'btn-email-otp-resend' : 'btn-mobile-otp-resend'
  const countdownId = isEmailStep ? 'lbl-email-otp-countdown' : 'lbl-mobile-otp-countdown'

  return (
    <div className="hero-gradient min-h-screen">
      <div className="mx-auto grid min-h-screen max-w-6xl grid-cols-1 items-center gap-8 px-4 py-8 lg:grid-cols-2 lg:px-8">
        {/* Brand / pitch */}
        <section className="order-2 lg:order-1">
          <div className="mb-4 inline-flex items-center gap-2.5">
            <span className="grid h-12 w-12 place-items-center rounded-3xl bg-brand-500 text-2xl text-white shadow-glow">
              🛡️
            </span>
            <div>
              <p className="text-lg font-extrabold text-ink-900">AuthShield 360</p>
              <p className="text-xs text-ink-400">VerifyVault — Identity Beyond Passwords</p>
            </div>
          </div>
          <h1 className="text-3xl font-extrabold leading-tight text-ink-900 sm:text-4xl">
            Cổng trường học an toàn với <span className="text-brand-500">xác thực đa lớp</span>
          </h1>
          <p className="mt-3 max-w-md text-sm text-ink-600">
            Ba chế độ xác thực <strong>S1</strong>, <strong>S2</strong>, <strong>S3</strong> giúp so sánh mức độ bảo mật
            và trải nghiệm người dùng: mật khẩu, Mobile OTP và Email OTP.
          </p>

          <div className="mt-5 grid grid-cols-3 gap-2">
            <div className="card !p-3 text-center">
              <p className="text-xl">🔑</p>
              <p className="mt-1 text-xs font-bold text-ink-900">S1</p>
              <p className="text-[11px] text-ink-400">Mật khẩu</p>
            </div>
            <div className="card !p-3 text-center">
              <p className="text-xl">📱</p>
              <p className="mt-1 text-xs font-bold text-ink-900">S2</p>
              <p className="text-[11px] text-ink-400">+ Mobile OTP</p>
            </div>
            <div className="card !p-3 text-center">
              <p className="text-xl">📧</p>
              <p className="mt-1 text-xs font-bold text-ink-900">S3</p>
              <p className="text-[11px] text-ink-400">+ Email OTP</p>
            </div>
          </div>

          <div className="mt-6 rounded-2xl border border-brand-100 bg-white/70 p-4">
            <p className="text-xs font-bold uppercase tracking-wide text-ink-600">Tài khoản thử nghiệm</p>
            <div className="mt-2 grid grid-cols-1 gap-1.5 sm:grid-cols-3">
              {DEMO_ACCOUNTS.map((acc) => (
                <button
                  key={acc.user}
                  type="button"
                  onClick={() => {
                    setUsername(acc.user)
                    setPassword(acc.pass)
                    setError('')
                  }}
                  className="chip w-full justify-center"
                >
                  {acc.user}
                </button>
              ))}
            </div>
          </div>
        </section>

        {/* Auth card */}
        <section className="order-1 lg:order-2">
          <div className="card animate-fade-in mx-auto w-full max-w-md !p-6">
            {step === 'credentials' ? (
              <form onSubmit={onSubmitCredentials} noValidate>
                <h2 className="text-xl font-extrabold text-ink-900">Đăng nhập</h2>
                <p className="mt-1 text-xs text-ink-400">Sử dụng tài khoản giả lập của hệ thống.</p>

                <div className="mt-5 space-y-4">
                  <div>
                    <label className="label" htmlFor="txt-username">
                      Tên đăng nhập
                    </label>
                    <input
                      id="txt-username"
                      name="username"
                      type="text"
                      className="input"
                      maxLength={50}
                      autoComplete="username"
                      required
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      placeholder="vd: student01"
                    />
                  </div>
                  <div>
                    <label className="label" htmlFor="txt-password">
                      Mật khẩu
                    </label>
                    <input
                      id="txt-password"
                      name="password"
                      type="password"
                      className="input"
                      minLength={8}
                      autoComplete="current-password"
                      required
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="••••••••"
                    />
                  </div>

                  {captcha ? (
                    <div className="rounded-xl border border-sun-400 bg-sun-100/60 p-3">
                      <label className="label" htmlFor="txt-captcha">
                        Xác thực chống robot
                      </label>
                      <p className="mb-2 text-sm font-semibold text-ink-900">{captcha.question}</p>
                      <input
                        id="txt-captcha"
                        className="input"
                        inputMode="numeric"
                        value={captcha.answer}
                        onChange={(e) => setCaptcha({ ...captcha, answer: e.target.value })}
                      />
                    </div>
                  ) : null}
                </div>

                <div
                  id="lbl-login-error"
                  role="alert"
                  aria-live="assertive"
                  className={`mt-4 text-sm font-semibold text-coral-600 ${error ? '' : 'hidden'}`}
                >
                  {error}
                </div>

                {lockCountdown.active ? (
                  <p className="mt-2 rounded-xl bg-coral-100 px-3 py-2 text-sm font-semibold text-coral-600">
                    Tài khoản đang bị khóa. Vui lòng thử lại sau {lockCountdown.label}.
                  </p>
                ) : null}

                <button
                  id="btn-login-submit"
                  type="submit"
                  className="btn-primary mt-5 w-full"
                  disabled={busy || lockCountdown.active}
                >
                  {busy ? <InlineSpinner /> : <span aria-hidden="true">→</span>}
                  Đăng nhập
                </button>
              </form>
            ) : (
              <form onSubmit={onSubmitOtp} noValidate>
                <button
                  type="button"
                  onClick={() => resetToCredentials('')}
                  className="mb-3 text-xs font-semibold text-brand-600 hover:underline"
                >
                  ← Quay lại
                </button>
                <h2 className="text-xl font-extrabold text-ink-900">
                  {isEmailStep ? 'Xác thực Email OTP' : 'Xác thực hai yếu tố'}
                </h2>
                <p className="mt-1 text-sm text-ink-600">
                  {isEmailStep
                    ? 'Xác thực Mobile OTP thành công. Một mã xác minh đã được gửi đến email của bạn.'
                    : challenge?.totpBased
                      ? 'Mở ứng dụng xác thực và nhập mã 6 chữ số.'
                      : 'Vui lòng nhập mã OTP đã được gửi đến thiết bị của bạn.'}
                </p>

                <div className="mt-3 flex items-center justify-between rounded-xl bg-surface-soft px-3 py-2 text-xs">
                  <span className="text-ink-400">
                    Kênh: <strong className="text-ink-600">{challenge?.deliveryChannel || '—'}</strong>
                  </span>
                  <span id={countdownId} className="font-semibold text-brand-600" aria-live="polite">
                    Hết hạn sau {otpCountdown.label}
                  </span>
                </div>

                {challenge?.deliveryCode ? (
                  <p className="mt-2 rounded-xl bg-accent-100 px-3 py-2 text-xs text-accent-600">
                    Mã OTP môi trường thử nghiệm: <strong>{challenge.deliveryCode}</strong>
                  </p>
                ) : null}

                <div className="mt-4">
                  <label className="label" htmlFor={otpFieldId}>
                    Mã xác minh (6 chữ số)
                  </label>
                  <input
                    id={otpFieldId}
                    ref={otpInputRef}
                    className="input text-center text-2xl font-bold tracking-[0.5em]"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    maxLength={6}
                    required
                    value={code}
                    onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    placeholder="123456"
                  />
                </div>

                <div
                  role="alert"
                  aria-live="assertive"
                  className={`mt-3 text-sm font-semibold text-coral-600 ${error ? '' : 'hidden'}`}
                >
                  {error}
                </div>

                <button id={verifyBtnId} type="submit" className="btn-primary mt-4 w-full" disabled={busy || code.length !== 6}>
                  {busy ? <InlineSpinner /> : <span aria-hidden="true">✓</span>}
                  Xác nhận
                </button>

                <div className="mt-3 flex items-center justify-between gap-2">
                  <button
                    id={resendBtnId}
                    type="button"
                    className="btn-ghost !px-3"
                    onClick={onResend}
                    disabled={resending || resendCountdown.active || resendLimitHit || challenge?.totpBased}
                  >
                    {resending ? <InlineSpinner /> : '🔄'}
                    {resendCountdown.active ? `Gửi lại mã (${resendCountdown.seconds}s)` : 'Gửi lại mã'}
                  </button>
                  <button
                    id={isEmailStep ? 'btn-email-otp-restart' : 'btn-mobile-otp-back'}
                    type="button"
                    className="btn-ghost !px-3"
                    onClick={() => resetToCredentials('')}
                  >
                    Bắt đầu lại
                  </button>
                </div>

                {challenge?.totpBased ? (
                  <p className="mt-3 text-xs text-ink-400">
                    Với ứng dụng xác thực (TOTP), mã thay đổi mỗi 30 giây — không cần gửi lại.
                  </p>
                ) : null}
              </form>
            )}
          </div>
        </section>
      </div>
    </div>
  )
}
