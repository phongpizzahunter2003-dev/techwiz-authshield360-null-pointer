import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { DetailShell } from '../../components/layout/DetailShell.jsx'
import { Badge, statusTone } from '../../components/ui/Badge.jsx'
import { ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
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

  const [resetting, setResetting] = useState(false)

  const resetMfa = async () => {
    if (resetting) return
    setResetting(true)
    try {
      await adminApi.resetMfa(id)
      toast.success('Two-factor authentication reset for the account.')
      await reload()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setResetting(false)
    }
  }

  const rows = user
    ? [
        ['Username', user.username],
        ['Email', user.email],
        ['Full name', user.fullName || '—'],
        ['Phone number', user.phone || '—'],
        ['Created at', formatDateTime(user.createdAt)],
      ]
    : []

  return (
    <DetailShell title={user ? `Account: ${user.username}` : 'User details'} fallback="/admin/users">
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {user ? (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <section className="card lg:col-span-2">
            <h2 className="text-base font-bold text-ink-900">Account information</h2>
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
            <h2 className="text-base font-bold text-ink-900">Security status</h2>
            <div className="mt-3 flex flex-wrap gap-2">
              <Badge tone="brand">{ROLE_LABEL[user.role] || user.role}</Badge>
              <Badge tone={statusTone(user.status)}>{user.status}</Badge>
              <Badge tone={user.mfaEnrolled ? 'accent' : 'sun'}>
                {user.mfaEnrolled ? 'MFA enrolled' : user.mfaEnabled ? 'Not enrolled' : 'MFA off'}
              </Badge>
            </div>
            <p className="mt-3 text-sm text-ink-600">
              Failed sign-in attempts: <strong>{user.failedAttempts}</strong>
            </p>
            <p className="mt-1 text-sm text-ink-600">
              Lockout level: <strong>{user.lockoutLevel}</strong>
            </p>
            <p className="mt-1 text-sm text-ink-600">
              Locked until: <strong>{user.lockedUntil ? formatDateTime(user.lockedUntil) : '—'}</strong>
            </p>

            <button type="button" className="btn-danger mt-4 w-full" onClick={resetMfa} disabled={resetting}>
              {resetting ? <InlineSpinner /> : '🔄'} Reset MFA (UC-16)
            </button>
            <p className="mt-2 text-xs text-ink-400">
              The user will have to re-enroll an MFA factor on their next use (UC-09).
            </p>
          </section>
        </div>
      ) : null}
    </DetailShell>
  )
}
