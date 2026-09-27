import { Link, useNavigate } from 'react-router-dom'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { StatGrid } from '../../components/ui/StatCard.jsx'
import { Badge, statusTone } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner } from '../../components/ui/Spinner.jsx'
import { ChartBoard } from '../../components/charts/ChartBoard.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { dashboardApi } from '../../api/endpoints.js'
import { formatDateTime } from '../../utils/format.js'

const STAT_LINKS = {
  users: '/admin/users',
  students: '/admin/users?role=STUDENT',
  teachers: '/admin/users?role=TEACHER',
  assignments: '/admin/audit-logs?action=ASSIGNMENT_CREATE',
  submissions: '/admin/audit-logs?action=SUBMISSION_CREATE',
  lockouts: '/admin/audit-logs?action=LOCKOUT_TRIGGERED',
  loginFailures: '/admin/audit-logs?action=LOGIN_FAIL',
  privilegeViolations: '/admin/audit-logs?action=PRIVILEGE_VIOLATION',
}

export function AdminDashboard() {
  const { data, loading, error, reload } = useAsync(() => dashboardApi.admin(), [])
  const dashboard = data?.data
  const navigate = useNavigate()

  return (
    <AppShell title="Admin dashboard" subtitle={dashboard?.greeting || 'Monitor the system and security'}>
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {dashboard ? (
        <div className="space-y-6">
          <StatGrid stats={dashboard.stats} linkFor={(key) => STAT_LINKS[key]} />

          <ChartBoard role="ADMIN" />

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <Link to="/admin/users" className="card transition hover:shadow-glow">
              <p className="text-2xl" aria-hidden="true">👥</p>
              <p className="mt-2 text-sm font-bold text-ink-900">Users & roles</p>
              <p className="text-xs text-ink-400">UC-07</p>
            </Link>
            <Link to="/admin/config" className="card transition hover:shadow-glow">
              <p className="text-2xl" aria-hidden="true">⚙️</p>
              <p className="mt-2 text-sm font-bold text-ink-900">Auth configuration</p>
              <p className="text-xs text-ink-400">UC-08</p>
            </Link>
            <Link to="/admin/audit-logs" className="card transition hover:shadow-glow">
              <p className="text-2xl" aria-hidden="true">📜</p>
              <p className="mt-2 text-sm font-bold text-ink-900">Audit log</p>
              <p className="text-xs text-ink-400">UC-11</p>
            </Link>
            <Link to="/admin/comparison" className="card transition hover:shadow-glow">
              <p className="text-2xl" aria-hidden="true">📈</p>
              <p className="mt-2 text-sm font-bold text-ink-900">S1/S2/S3 comparison</p>
              <p className="text-xs text-ink-400">UC-15</p>
            </Link>
            <Link to="/admin/system" className="card transition hover:shadow-glow">
              <p className="text-2xl" aria-hidden="true">🗄️</p>
              <p className="mt-2 text-sm font-bold text-ink-900">System & data</p>
              <p className="text-xs text-ink-400">Check the database</p>
            </Link>
          </div>

          <section>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-base font-bold text-ink-900">Recent security events</h2>
              <Link className="text-sm font-semibold text-brand-600 hover:underline" to="/admin/audit-logs">
                View all
              </Link>
            </div>
            {dashboard.recentEvents?.length ? (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Time</th>
                      <th>Action</th>
                      <th>Result</th>
                      <th>User</th>
                      <th>Role</th>
                      <th>Factor</th>
                      <th>Mode</th>
                      <th>IP</th>
                      <th className="text-right">Details</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dashboard.recentEvents.map((e) => (
                      <tr
                        key={e.id}
                        className="cursor-pointer transition hover:bg-surface-soft"
                        onClick={() => navigate(`/admin/audit-logs/${e.id}`)}
                      >
                        <td className="whitespace-nowrap">{formatDateTime(e.eventTime)}</td>
                        <td className="font-semibold text-ink-900">{e.eventAction}</td>
                        <td>
                          <Badge tone={statusTone(e.status)}>{e.status}</Badge>
                        </td>
                        <td>{e.userIdentifier || '—'}</td>
                        <td>{e.role || '—'}</td>
                        <td>{e.authFactor || '—'}</td>
                        <td>{e.authMode || '—'}</td>
                        <td>{e.clientIp || '—'}</td>
                        <td className="text-right">
                          <Link
                            to={`/admin/audit-logs/${e.id}`}
                            className="text-sm font-semibold text-brand-600 hover:underline"
                            onClick={(ev) => ev.stopPropagation()}
                          >
                            View →
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <EmptyState icon="📜" title="No events yet" description="Authentication events will appear here." />
            )}
          </section>
        </div>
      ) : null}
    </AppShell>
  )
}
