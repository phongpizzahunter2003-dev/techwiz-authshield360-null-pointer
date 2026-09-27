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
    name: 'S1 — Chỉ mật khẩu',
    tone: 'coral',
    summary: 'Baseline: người dùng chỉ cần username + mật khẩu.',
    factors: ['PASSWORD'],
    flow: ['Nhập username + mật khẩu', 'Hệ thống xác minh', 'Cấp phiên và áp quyền theo vai trò'],
    otp: false,
    email: false,
    applies: [
      'Mật khẩu băm BCrypt (work factor 12)',
      'Chống dò mật khẩu: khóa tạm sau 5 lần sai liên tiếp',
      'Ghi nhật ký đăng nhập, đăng xuất, vi phạm phân quyền',
    ],
    notApplies: ['Không có yếu tố xác thực thứ hai', 'Chỉ lộ mật khẩu là đủ để truy cập (điểm yếu baseline)'],
  },
  S2: {
    icon: '📱',
    name: 'S2 — Mật khẩu + Mobile OTP',
    tone: 'sun',
    summary: 'Thêm lớp xác minh thứ hai bằng Mobile OTP (TOTP hoặc SMS mô phỏng).',
    factors: ['PASSWORD', 'MOBILE_OTP'],
    flow: ['Nhập username + mật khẩu', 'Hệ thống gửi/sinh Mobile OTP', 'Nhập OTP', 'Cấp phiên và áp quyền'],
    otp: true,
    email: false,
    applies: [
      'Toàn bộ chính sách của S1',
      'OTP có thời hạn hiệu lực và chỉ dùng một lần',
      'OTP sai cũng tính vào ngưỡng khóa (VĐ-04)',
      'Giới hạn gửi lại: 60 giây mỗi lần, tối đa 3 lần',
    ],
    notApplies: ['Chưa có lớp xác minh qua email'],
  },
  S3: {
    icon: '📧',
    name: 'S3 — Mật khẩu + Mobile OTP + Email OTP',
    tone: 'accent',
    summary: 'Xác thực tuần tự ba lớp: mật khẩu → Mobile OTP → Email OTP.',
    factors: ['PASSWORD', 'MOBILE_OTP', 'EMAIL_OTP'],
    flow: [
      'Nhập username + mật khẩu',
      'Xác minh Mobile OTP',
      'Hệ thống gửi Email OTP tới hộp thư thử nghiệm',
      'Xác minh Email OTP',
      'Cấp phiên và áp quyền theo vai trò',
    ],
    otp: true,
    email: true,
    applies: [
      'Toàn bộ chính sách của S2',
      'Email OTP có thời hạn; gửi lại tối đa 3 lần rồi bắt đầu lại quy trình (VĐ-07)',
      'Cần cấu hình SMTP tới hộp thư thử nghiệm',
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
      toast.success(`Đã áp dụng chế độ ${mode}. Hệ thống sẽ dùng ở lần đăng nhập tiếp theo.`)
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
            📜 Nhật ký theo chế độ
          </Link>
          <Link className="btn-ghost" to={`/admin/config?mode=${mode}`}>
            ⚙️ Cấu hình chi tiết
          </Link>
          <button type="button" className="btn-primary" onClick={applyMode} disabled={applying || isActive}>
            {applying ? <InlineSpinner /> : '✓'} {isActive ? 'Đang sử dụng' : 'Áp dụng chế độ này'}
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
              {isActive ? <Badge tone="brand">Đang dùng</Badge> : <Badge tone="neutral">Chưa kích hoạt</Badge>}
              {meta.factors.map((f) => (
                <Badge key={f} tone="sky">
                  {f}
                </Badge>
              ))}
            </div>
            <p className="mt-3 text-sm text-ink-600">{meta.summary}</p>

            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div className="rounded-xl bg-surface-soft p-3">
                <p className="text-xs font-bold uppercase text-ink-400">Mức bảo mật</p>
                <p className="mt-1 text-sm font-semibold text-ink-900">{metrics?.security || '—'}</p>
              </div>
              <div className="rounded-xl bg-surface-soft p-3">
                <p className="text-xs font-bold uppercase text-ink-400">Khả năng sử dụng</p>
                <p className="mt-1 text-sm font-semibold text-ink-900">{metrics?.usability || '—'}</p>
              </div>
              <div className="rounded-xl bg-surface-soft p-3">
                <p className="text-xs font-bold uppercase text-ink-400">Rủi ro được giảm</p>
                <p className="mt-1 text-sm font-semibold text-ink-900">{metrics?.riskReduced || '—'}</p>
              </div>
            </div>
          </section>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <section className="card">
              <h2 className="text-base font-bold text-ink-900">Luồng đăng nhập</h2>
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
              <h2 className="text-base font-bold text-ink-900">Số liệu đo được</h2>
              <p className="mt-1 text-xs text-ink-400">
                Tổng hợp từ <code className="font-mono">audit_logs</code> (BR-09: tối thiểu 3 lần chạy mỗi chế độ).
              </p>
              <div className="mt-3 grid grid-cols-3 gap-2 text-center">
                <div className="rounded-xl bg-accent-100 p-3">
                  <p className="text-xl font-extrabold text-accent-600">{metrics?.loginSuccesses ?? 0}</p>
                  <p className="text-[11px] text-ink-600">Đăng nhập thành công</p>
                </div>
                <div className="rounded-xl bg-coral-100 p-3">
                  <p className="text-xl font-extrabold text-coral-600">{metrics?.loginFailures ?? 0}</p>
                  <p className="text-[11px] text-ink-600">Đăng nhập thất bại</p>
                </div>
                <div className="rounded-xl bg-sun-100 p-3">
                  <p className="text-xl font-extrabold text-sun-600">{metrics?.otpFailures ?? 0}</p>
                  <p className="text-[11px] text-ink-600">Lỗi OTP</p>
                </div>
              </div>
              <Link className="btn-ghost mt-3 w-full" to={`/admin/audit-logs?mode=${mode}`}>
                Xem nhật ký của chế độ {mode} →
              </Link>
            </section>
          </div>

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Cấu hình áp dụng cho {mode}</h2>
            {configData ? (
              <dl className="mt-3 grid grid-cols-1 gap-x-8 gap-y-2 text-sm sm:grid-cols-2">
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">Loại OTP</dt>
                  <dd className="font-semibold text-ink-900">{meta.otp ? configData.otpType : '— (không dùng)'}</dd>
                </div>
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">Độ dài mã</dt>
                  <dd className="font-semibold text-ink-900">{meta.otp ? configData.otpLength : '—'}</dd>
                </div>
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">Thời gian hiệu lực</dt>
                  <dd className="font-semibold text-ink-900">
                    {meta.otp ? `${configData.otpValiditySeconds} giây` : '—'}
                  </dd>
                </div>
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">Chờ gửi lại / số lần tối đa</dt>
                  <dd className="font-semibold text-ink-900">
                    {meta.otp ? `${configData.resendCooldownSeconds}s / ${configData.maxResend}` : '—'}
                  </dd>
                </div>
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">Email OTP</dt>
                  <dd className="font-semibold text-ink-900">
                    {meta.email ? (configData.emailOtpEnabled ? 'Đang bật' : 'Đang tắt') : '— (không dùng)'}
                  </dd>
                </div>
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">SMTP</dt>
                  <dd className="max-w-[60%] truncate font-semibold text-ink-900">
                    {meta.email ? `${configData.smtpHost || '—'}:${configData.smtpPort || '—'}` : '— (không dùng)'}
                  </dd>
                </div>
                <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">Ngưỡng khóa tài khoản</dt>
                  <dd className="font-semibold text-ink-900">
                    {configData.maxFailedAttempts} lần → {configData.lockoutDurationsSeconds}
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-ink-400">Captcha sau N lần gửi OTP</dt>
                  <dd className="font-semibold text-ink-900">
                    {meta.otp ? configData.requireCaptchaAfter : '—'}
                  </dd>
                </div>
              </dl>
            ) : null}
          </section>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <section className="card">
              <h2 className="text-base font-bold text-ink-900">Áp dụng trong chế độ này</h2>
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
              <h2 className="text-base font-bold text-ink-900">Không áp dụng</h2>
              {meta.notApplies.length === 0 ? (
                <p className="mt-3 text-sm text-accent-600">
                  Chế độ này áp dụng đầy đủ mọi lớp bảo vệ đã triển khai.
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
                Việc <strong>đăng ký MFA</strong> vẫn do từng người dùng tự thực hiện (UC-09). Quản trị viên có thể
                gán chế độ cho từng tài khoản ở trang Người dùng &amp; vai trò.
              </p>
            </section>
          </div>
        </div>
      ) : null}
    </DetailShell>
  )
}
