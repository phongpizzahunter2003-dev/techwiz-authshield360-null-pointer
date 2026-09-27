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
      toast.success(`Đã sao chép ${label}.`)
    } catch {
      toast.error('Không thể sao chép tự động. Vui lòng chọn và sao chép thủ công.')
    }
  }

  const startEnroll = async () => {
    setBusy(true)
    try {
      const res = await authApi.enrollMfa()
      setEnroll(res.data)
      toast.info('Quét mã QR bằng ứng dụng xác thực, sau đó nhập mã 6 số để xác nhận.')
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
      toast.success('Thiết lập xác thực hai yếu tố thành công.')
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
    <AppShell title="Hồ sơ & bảo mật" subtitle="Thông tin phiên làm việc và xác thực hai lớp (UC-09)">
      {current.loading ? <Spinner /> : null}
      {current.error ? <ErrorState message={current.error.message} onRetry={current.reload} /> : null}

      {s ? (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Phiên làm việc hiện tại</h2>
            <dl className="mt-4 space-y-3 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Tên đăng nhập</dt>
                <dd className="font-semibold text-ink-900">{s.username}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Họ tên</dt>
                <dd className="font-semibold text-ink-900">{s.fullName || '—'}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Email</dt>
                <dd className="font-semibold text-ink-900">{s.email || '—'}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Vai trò</dt>
                <dd>
                  <Badge tone="brand">{ROLE_LABEL[s.role] || s.role}</Badge>
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Phương thức xác thực</dt>
                <dd className="font-semibold text-ink-900">{s.authMethod || '—'}</dd>
              </div>
              <div className="flex items-start justify-between gap-3">
                <dt className="text-ink-400">Chế độ xác thực hiệu lực</dt>
                <dd className="text-right">
                  <Badge tone="brand">{s.effectiveAuthMode || 'S1'}</Badge>
                  <p className="mt-1 text-xs text-ink-400">
                    {s.authModeOverride
                      ? 'Do quản trị viên gán cho tài khoản này'
                      : 'Theo cấu hình chung của hệ thống'}
                  </p>
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Session ID</dt>
                <dd className="max-w-[220px] truncate font-mono text-xs text-ink-600">{s.sessionId}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Bắt đầu</dt>
                <dd className="font-semibold text-ink-900">{formatDateTime(s.issuedAt)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Hết hạn</dt>
                <dd className="font-semibold text-ink-900">{formatDateTime(s.expiresAt)}</dd>
              </div>
            </dl>
            <button type="button" className="btn-danger mt-5" onClick={logout}>
              ↩ Đăng xuất
            </button>
            <p className="mt-2 text-xs text-ink-400">
              Đăng xuất sẽ hủy phiên phía máy chủ và xóa lịch sử trình duyệt (UC-06).
            </p>
          </section>

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Xác thực hai lớp (MFA)</h2>
            {!enroll ? (
              <>
                <p className="mt-2 text-sm text-ink-600">
                  Thiết lập ứng dụng xác thực (Google Authenticator, FreeOTP) để bảo vệ tài khoản bằng TOTP.
                </p>
                <button type="button" className="btn-primary mt-4" onClick={startEnroll} disabled={busy}>
                  {busy ? <InlineSpinner /> : '🔐'} Bắt đầu thiết lập
                </button>
              </>
            ) : (
              <form onSubmit={confirmEnroll} className="mt-3 space-y-4">
                <ol className="list-decimal space-y-1 pl-5 text-sm text-ink-600">
                  <li>Mở ứng dụng xác thực (Google Authenticator, Microsoft Authenticator, FreeOTP…).</li>
                  <li>
                    Chọn <strong>“Quét mã QR”</strong> <em>trong ứng dụng xác thực</em> rồi quét mã bên dưới.
                    Đừng quét bằng ứng dụng camera mặc định của điện thoại — camera sẽ báo “chỉ mở bằng ứng dụng”.
                  </li>
                  <li>Nhập mã 6 số mà ứng dụng hiển thị để hoàn tất.</li>
                </ol>

                <div className="flex flex-col items-center gap-3 rounded-2xl bg-surface-soft p-4">
                  <div className="rounded-2xl bg-white p-3 shadow-soft">
                    <QRCodeSVG value={enroll.otpauthUri} size={168} />
                  </div>
                  <div className="w-full">
                    <p className="text-center text-xs font-bold uppercase text-ink-400">
                      Hoặc nhập thủ công mã bí mật
                    </p>
                    <div className="mt-1 flex items-center gap-2">
                      <code className="min-w-0 flex-1 break-all rounded-xl bg-white px-3 py-2 font-mono text-xs text-ink-900">
                        {enroll.secret}
                      </code>
                      <button
                        type="button"
                        className="btn-ghost !px-3 !py-2"
                        onClick={() => copy(enroll.secret, 'mã bí mật')}
                        title="Sao chép mã bí mật"
                      >
                        📋
                      </button>
                    </div>
                    <button
                      type="button"
                      className="btn-ghost mt-2 w-full !py-2 text-xs"
                      onClick={() => copy(enroll.otpauthUri, 'liên kết otpauth')}
                    >
                      🔗 Sao chép liên kết otpauth:// (dán vào ứng dụng xác thực)
                    </button>
                  </div>
                </div>

                <details className="rounded-xl border border-sky-300 bg-sky-100/50 px-3 py-2 text-xs text-ink-600">
                  <summary className="cursor-pointer font-semibold text-sky-600">
                    ❓ Không quét được mã QR? Mở hướng dẫn khắc phục
                  </summary>
                  <div className="mt-2 space-y-1.5">
                    <p>
                      • Điện thoại báo <strong>“chỉ mở bằng ứng dụng”</strong>: điện thoại đang mở liên kết{' '}
                      <code className="font-mono">otpauth://</code> bằng trình duyệt/camera nên không có ứng dụng nào
                      nhận. Hãy quét QR <strong>trực tiếp trong ứng dụng xác thực</strong>.
                    </p>
                    <p>
                      • Không quét được? Chọn <strong>“Nhập khóa thủ công / Enter setup key”</strong> trong ứng dụng
                      xác thực rồi dán <strong>mã bí mật</strong> ở trên.
                    </p>
                    <p>
                      • Mã luôn báo sai dù đã nhập đúng? Kiểm tra <strong>thời gian trên điện thoại</strong> đã bật
                      tự động chưa — TOTP cần đồng bộ giờ.
                    </p>
                  </div>
                </details>

                <div className="rounded-2xl border border-sun-400 bg-sun-100/60 p-3 text-sm">
                  <p className="font-semibold text-ink-900">
                    Mã TOTP hiện tại (chỉ dùng cho môi trường thử nghiệm): <strong>{totpPreview}</strong>
                  </p>
                  <p className="text-xs text-ink-600">Mã đổi sau {seconds}s. Trong môi trường thật, mã chỉ hiển thị trên thiết bị.</p>
                </div>

                <div>
                  <label className="label" htmlFor="txt-mfa-code">
                    Nhập mã 6 chữ số từ ứng dụng để xác nhận
                  </label>
                  <input
                    id="txt-mfa-code"
                    className="input text-center text-2xl font-bold tracking-[0.5em]"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    maxLength={6}
                    value={code}
                    onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    placeholder="••••••"
                  />
                </div>

                <div className="flex justify-end gap-2">
                  <button type="button" className="btn-ghost" onClick={() => setEnroll(null)}>
                    Hủy
                  </button>
                  <button type="submit" className="btn-primary" disabled={busy || code.length !== 6}>
                    {busy ? <InlineSpinner /> : '✓'} Xác nhận
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
