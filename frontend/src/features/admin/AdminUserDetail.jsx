import { useParams } from 'react-router-dom'
import { DetailShell } from '../../components/layout/DetailShell.jsx'
import { Badge, statusTone } from '../../components/ui/Badge.jsx'
import { ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner } from '../../components/ui/Spinner.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { adminApi } from '../../api/endpoints.js'
import { ROLE_LABEL } from '../../i18n/messages.js'
import { formatDateTime } from '../../utils/format.js'

/** Admin drill-down: single account, its role/MFA/lockout state and actions. */
export function AdminUserDetail() {
  const { id } = useParams()
  const toast = useToast()
  const { data, loading, error, reload } = useAsync(() => adminApi.user(id), [id])
  const user = data?.data

  const resetMfa = async () => {
    try {
      await adminApi.resetMfa(id)
      toast.success('Đã đặt lại xác thực hai lớp cho tài khoản.')
      await reload()
    } catch (err) {
      toast.error(err.message)
    }
  }

  const rows = user
    ? [
        ['Tên đăng nhập', user.username],
        ['Email', user.email],
        ['Họ tên', user.fullName || '—'],
        ['Số điện thoại', user.phone || '—'],
        ['Tạo lúc', formatDateTime(user.createdAt)],
      ]
    : []

  return (
    <DetailShell title={user ? `Tài khoản: ${user.username}` : 'Chi tiết người dùng'} fallback="/admin/users">
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {user ? (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <section className="card lg:col-span-2">
            <h2 className="text-base font-bold text-ink-900">Thông tin tài khoản</h2>
            <dl className="mt-4 space-y-3 text-sm">
              {rows.map(([label, value]) => (
                <div key={label} className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">{label}</dt>
                  <dd className="font-semibold text-ink-900">{value}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Trạng thái bảo mật</h2>
            <div className="mt-3 flex flex-wrap gap-2">
              <Badge tone="brand">{ROLE_LABEL[user.role] || user.role}</Badge>
              <Badge tone={statusTone(user.status)}>{user.status}</Badge>
              <Badge tone={user.mfaEnrolled ? 'accent' : 'sun'}>
                {user.mfaEnrolled ? 'MFA đã đăng ký' : user.mfaEnabled ? 'MFA chưa đăng ký' : 'MFA tắt'}
              </Badge>
            </div>
            <p className="mt-3 text-sm text-ink-600">
              Số lần đăng nhập sai liên tiếp: <strong>{user.failedAttempts}</strong>
            </p>
            <p className="mt-1 text-sm text-ink-600">
              Cấp độ khóa: <strong>{user.lockoutLevel}</strong>
            </p>
            <p className="mt-1 text-sm text-ink-600">
              Khóa đến: <strong>{user.lockedUntil ? formatDateTime(user.lockedUntil) : '—'}</strong>
            </p>

            <button type="button" className="btn-danger mt-4 w-full" onClick={resetMfa}>
              🔄 Đặt lại MFA (UC-16)
            </button>
            <p className="mt-2 text-xs text-ink-400">
              Người dùng sẽ phải đăng ký lại yếu tố MFA ở lần sử dụng tiếp theo (UC-09).
            </p>
          </section>
        </div>
      ) : null}
    </DetailShell>
  )
}
