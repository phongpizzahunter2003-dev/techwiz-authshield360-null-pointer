import { Link } from 'react-router-dom'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { StatGrid } from '../../components/ui/StatCard.jsx'
import { Badge, statusTone, SubmissionStatusBadge } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner } from '../../components/ui/Spinner.jsx'
import { ChartBoard } from '../../components/charts/ChartBoard.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { dashboardApi } from '../../api/endpoints.js'
import { formatDateTime } from '../../utils/format.js'

// Each stat card drills into the matching filtered view.
const STAT_LINKS = {
  classes: '/student/assignments',
  assignments: '/student/assignments',
  submitted: '/student/assignments?bucket=ON_TIME',
  pending: '/student/assignments?bucket=PENDING',
  late: '/student/assignments?bucket=LATE',
}

export function StudentDashboard() {
  const { data, loading, error, reload } = useAsync(() => dashboardApi.student(), [])
  const dashboard = data?.data

  return (
    <AppShell
      title="Bảng điều khiển học sinh"
      subtitle={dashboard?.greeting || 'Theo dõi bài tập và kết quả học tập của bạn'}
    >
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {dashboard ? (
        <div className="space-y-6">
          <StatGrid stats={dashboard.stats} linkFor={(key) => STAT_LINKS[key]} />

          <ChartBoard role="STUDENT" />

          <section>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-base font-bold text-ink-900">Bài tập gần đây</h2>
              <Link className="text-sm font-semibold text-brand-600 hover:underline" to="/student/assignments">
                Xem tất cả
              </Link>
            </div>
            {dashboard.assignments?.length ? (
              <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
                {dashboard.assignments.slice(0, 6).map((a) => (
                  <article key={a.id} className="card flex flex-col">
                    <div className="flex items-start justify-between gap-2">
                      <h3 className="text-sm font-bold text-ink-900">{a.title}</h3>
                      <Badge tone={statusTone(a.status)}>{a.status}</Badge>
                    </div>
                    <p className="mt-1 text-xs text-ink-400">{a.classroomName}</p>
                    <p className="mt-3 text-xs text-ink-600">
                      Hạn nộp: <strong>{formatDateTime(a.dueAt)}</strong>
                    </p>
                    <div className="mt-3 flex flex-wrap items-center gap-2">
                      {a.latestSubmission ? <SubmissionStatusBadge status={a.latestSubmission.submissionStatus} /> : null}
                      {a.lockReason ? <Badge tone="coral" icon="🔒">{a.lockReason}</Badge> : null}
                      {a.canSubmit ? <Badge tone="accent" icon="✍️">Có thể nộp</Badge> : null}
                    </div>
                    <Link className="btn-ghost mt-4 w-full" to={`/student/assignments/${a.id}`}>
                      Xem chi tiết
                    </Link>
                  </article>
                ))}
              </div>
            ) : (
              <EmptyState icon="📚" title="Chưa có bài tập" description="Giáo viên chưa giao bài tập nào cho lớp của bạn." />
            )}
          </section>
        </div>
      ) : null}
    </AppShell>
  )
}
