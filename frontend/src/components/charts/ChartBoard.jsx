import { analyticsApi } from '../../api/endpoints.js'
import { useAsync } from '../../hooks/useAsync.js'
import { ErrorState } from '../ui/EmptyState.jsx'
import { Spinner } from '../ui/Spinner.jsx'
import { ChartCard } from './ChartCard.jsx'

const FETCHERS = {
  STUDENT: analyticsApi.student,
  TEACHER: analyticsApi.teacher,
  ADMIN: analyticsApi.admin,
}

/** Renders the role-specific clickable chart board. */
export function ChartBoard({ role }) {
  const fetchFn = FETCHERS[role] || analyticsApi.student
  const { data, loading, error, reload } = useAsync(() => fetchFn(), [role])

  if (loading) return <Spinner label="Đang tải biểu đồ..." />
  if (error) return <ErrorState message={error.message} onRetry={reload} />

  const charts = data?.data?.charts || []
  if (charts.length === 0) return null

  return (
    <section>
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-base font-bold text-ink-900">Biểu đồ phân tích</h2>
        <span className="text-xs text-ink-400">Bấm vào biểu đồ hoặc nhãn để xem chi tiết</span>
      </div>
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        {charts.map((series) => (
          <ChartCard key={series.key} series={series} />
        ))}
      </div>
    </section>
  )
}
