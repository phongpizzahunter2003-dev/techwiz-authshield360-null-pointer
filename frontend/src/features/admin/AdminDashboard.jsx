import { Link } from 'react-router-dom'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { StatGrid } from '../../components/ui/StatCard.jsx'
import { Badge, statusTone } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner } from '../../components/ui/Spinner.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { dashboardApi } from '../../api/endpoints.js'
import { formatDateTime } from '../../utils/format.js'

export function AdminDashboard() {
  const { data, loading, error, reload } = useAsync(() => dashboardApi.admin(), [])
  const dashboard = data?.data

  return (
    <AppShell title="Bảng điều khiển quản trị" subtitle={dashboard?.greeting || 'Giám sát hệ thống và bảo mật'}>
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {dashboard ? (
        <div className="space-y-6">
          <StatGrid stats={dashboard.stats} />

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <Link to="/admin/users" className="card hover:shadow-glow transition">
              <p className="text-2xl" aria-hidden="true">👥</p>
              <p className="mt-2 text-sm font-bold text-ink-900">Người dùng & vai trò</p>
              <p className="text-xs text-ink-400">UC-07</p>
            </Link>
            <Link to="/admin/config" className="card hover:shadow-glow transition">
              <p className="text-2xl" aria-hidden="true">⚙️</p>
              <p className="mt-2 text-sm font-bold text-ink-900">Cấu hình xác thực</p>
              <p className="text-xs text-ink-400">UC-08</p>
            </Link>
            <Link to="/admin/audit-logs" className="card hover:shadow-glow transition">
              <p className="text-2xl" aria-hidden="true">📜</p>
              <p className="mt-2 text-sm font-bold text-ink-900">Nhật ký xác thực</p>
              <p className="text-xs text-ink-400">UC-11</p>
            </Link>
            <Link to="/admin/comparison" className="card hover:shadow-glow transition">
              <p className="text-2xl" aria-hidden="true">📈</p>
              <p className="mt-2 text-sm font-bold text-ink-900">So sánh S1/S2/S3</p>
              <p className="text-xs text-ink-400">UC-15</p>
            </Link>
          </div>

          <section>
            <h2 className="mb-3 text-base font-bold text-ink-900">Sự kiện bảo mật gần đây</h2>
            {dashboard.recentEvents?.length ? (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Thời điểm</th>
                      <th>Hành động</th>
                      <th>Kết quả</th>
                      <th>Người dùng</th>
                      <th>Vai trò</th>
                      <th>Yếu tố</th>
                      <th>Chế độ</th>
                      <th>IP</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dashboard.recentEvents.map((e) => (
                      <tr key={e.id}>
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
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <EmptyState icon="📜" title="Chưa có sự kiện" description="Các sự kiện xác thực sẽ xuất hiện tại đây." />
            )}
          </section>
        </div>
      ) : null}
    </AppShell>
  )
}
