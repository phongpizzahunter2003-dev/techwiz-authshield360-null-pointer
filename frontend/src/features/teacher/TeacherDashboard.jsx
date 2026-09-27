import { Link } from 'react-router-dom'
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
  classes: '/teacher/classes',
  students: '/teacher/classes',
  assignments: '/teacher/assignments',
  toGrade: '/teacher/assignments',
}

export function TeacherDashboard() {
  const { data, loading, error, reload } = useAsync(() => dashboardApi.teacher(), [])
  const dashboard = data?.data

  return (
    <AppShell title="Teacher dashboard" subtitle={dashboard?.greeting || 'Manage your classes and assignments'}>
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {dashboard ? (
        <div className="space-y-6">
          <StatGrid stats={dashboard.stats} linkFor={(key) => STAT_LINKS[key]} />

          <ChartBoard role="TEACHER" />

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <section>
              <div className="mb-3 flex items-center justify-between">
                <h2 className="text-base font-bold text-ink-900">Classes taught</h2>
                <Link className="text-sm font-semibold text-brand-600 hover:underline" to="/teacher/classes">
                  Manage
                </Link>
              </div>
              {dashboard.classrooms?.length ? (
                <ul className="space-y-2">
                  {dashboard.classrooms.map((c) => (
                    <li key={c.id}>
                      <Link
                        to={`/teacher/classes/${c.id}`}
                        className="card flex items-center justify-between !p-4 transition hover:shadow-glow"
                      >
                        <div>
                          <p className="text-sm font-bold text-ink-900">{c.name}</p>
                          <p className="text-xs text-ink-400">{c.code}</p>
                        </div>
                        <Badge tone="sky" icon="🎒">
                          {c.studentCount} students
                        </Badge>
                      </Link>
                    </li>
                  ))}
                </ul>
              ) : (
                <EmptyState icon="🏫" title="No classes yet" description="Create a class to start assigning work." />
              )}
            </section>

            <section>
              <div className="mb-3 flex items-center justify-between">
                <h2 className="text-base font-bold text-ink-900">Assignments given</h2>
                <Link className="text-sm font-semibold text-brand-600 hover:underline" to="/teacher/assignments">
                  Manage
                </Link>
              </div>
              {dashboard.assignments?.length ? (
                <ul className="space-y-2">
                  {dashboard.assignments.slice(0, 6).map((a) => (
                    <li key={a.id}>
                      <Link
                        to={`/teacher/assignments/${a.id}`}
                        className="card flex items-center justify-between gap-3 !p-4 transition hover:shadow-glow"
                      >
                        <div className="min-w-0">
                          <p className="truncate text-sm font-bold text-ink-900">{a.title}</p>
                          <p className="text-xs text-ink-400">Due: {formatDateTime(a.dueAt)}</p>
                        </div>
                        <Badge tone={statusTone(a.status)}>{a.status}</Badge>
                      </Link>
                    </li>
                  ))}
                </ul>
              ) : (
                <EmptyState icon="📝" title="No assignments yet" description="Create a new assignment for your class." />
              )}
            </section>
          </div>
        </div>
      ) : null}
    </AppShell>
  )
}
