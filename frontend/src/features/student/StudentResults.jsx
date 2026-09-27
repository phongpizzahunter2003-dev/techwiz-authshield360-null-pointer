import { AppShell } from '../../components/layout/AppShell.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner } from '../../components/ui/Spinner.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { resultApi } from '../../api/endpoints.js'
import { formatDate } from '../../utils/format.js'

export function StudentResults() {
  const { data, loading, error, reload } = useAsync(() => resultApi.mine(), [])
  const results = data?.data || []

  return (
    <AppShell title="Exam results" subtitle="Only your own results are shown (server-side RBAC)">
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {!loading && !error && results.length === 0 ? (
        <EmptyState icon="📊" title="No results yet" description="Your teacher has not entered any exam results for you." />
      ) : null}

      {results.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Subject</th>
                <th>Exam</th>
                <th>Date</th>
                <th>Score</th>
                <th>Result</th>
              </tr>
            </thead>
            <tbody>
              {results.map((r) => {
                const ratio = r.maxScore ? r.score / r.maxScore : 0
                const tone = ratio >= 0.8 ? 'accent' : ratio >= 0.5 ? 'sun' : 'coral'
                return (
                  <tr key={r.id}>
                    <td className="font-semibold text-ink-900">{r.subject}</td>
                    <td>{r.examName}</td>
                    <td>{formatDate(r.examDate)}</td>
                    <td className="font-bold">
                      {r.score}/{r.maxScore}
                    </td>
                    <td>
                      <span
                        className={`badge ${
                          tone === 'accent'
                            ? 'bg-accent-100 text-accent-600'
                            : tone === 'sun'
                              ? 'bg-sun-100 text-sun-600'
                              : 'bg-coral-100 text-coral-600'
                        }`}
                      >
                        {ratio >= 0.8 ? 'Good' : ratio >= 0.5 ? 'Pass' : 'Needs improvement'}
                      </span>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      ) : null}
    </AppShell>
  )
}
