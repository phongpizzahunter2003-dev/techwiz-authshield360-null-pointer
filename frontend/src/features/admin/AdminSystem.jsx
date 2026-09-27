import { DetailShell } from '../../components/layout/DetailShell.jsx'
import { Badge } from '../../components/ui/Badge.jsx'
import { ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { adminApi } from '../../api/endpoints.js'
import { formatDateTime } from '../../utils/format.js'

/**
 * Admin diagnostics: proves which database the API is connected to and how many rows
 * each table actually holds (answers "did the data reach the database?").
 */
export function AdminSystem() {
  const { data, loading, error, reload } = useAsync(() => adminApi.dbStatus(), [])
  const status = data?.data

  return (
    <DetailShell
      title="System & data"
      subtitle="Check the database connection and the actual row count in each table"
      fallback="/admin"
      actions={
        <button type="button" className="btn-ghost" onClick={reload} disabled={loading}>
          {loading ? <InlineSpinner /> : '🔄'} Refresh
        </button>
      }
    >
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {status ? (
        <div className="space-y-6">
          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Database connection</h2>
            <div className="mt-3 flex flex-wrap gap-2">
              <Badge tone="brand">{status.databaseProduct}</Badge>
              <Badge tone="neutral">v{status.databaseVersion}</Badge>
              <Badge tone={status.persistent ? 'accent' : 'sun'}>
                {status.persistent ? 'Durable storage' : 'In memory (lost on restart)'}
              </Badge>
              <Badge tone="sky">Profile: {status.activeProfiles || 'default'}</Badge>
            </div>
            <dl className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                <dt className="text-ink-400">JDBC URL</dt>
                <dd className="max-w-[70%] truncate font-mono text-xs text-ink-900" title={status.jdbcUrl}>
                  {status.jdbcUrl}
                </dd>
              </div>
              <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                <dt className="text-ink-400">Total rows</dt>
                <dd className="font-semibold text-ink-900">{status.totalRows}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-ink-400">Checked at</dt>
                <dd className="font-semibold text-ink-900">{formatDateTime(status.checkedAt)}</dd>
              </div>
            </dl>
            {!status.persistent ? (
              <p className="mt-3 rounded-xl border border-sun-400 bg-sun-100/60 px-3 py-2 text-xs text-ink-600">
                Running profile <strong>dev</strong> with in-memory H2: data <strong>is</strong> written to
                the database but will be lost on restart. To keep data long-term, run with the
                <strong> mysql</strong> profile (see README.md §3).
              </p>
            ) : null}
          </section>

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Rows per table</h2>
            <div className="table-wrap mt-3">
              <table className="table">
                <thead>
                  <tr>
                    <th>Table</th>
                    <th>Rows</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {status.tables.map((t) => (
                    <tr key={t.table}>
                      <td className="font-mono text-xs text-ink-900">{t.table}</td>
                      <td className="font-bold">{t.rows < 0 ? '—' : t.rows}</td>
                      <td>
                        {t.rows < 0 ? (
                          <Badge tone="coral">Table missing</Badge>
                        ) : t.rows === 0 ? (
                          <Badge tone="neutral">Empty</Badge>
                        ) : (
                          <Badge tone="accent">Has data</Badge>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="mt-2 text-xs text-ink-400">
              Row counts are read directly with SQL on the active connection — this is proof the data made it
              into the database.
            </p>
          </section>
        </div>
      ) : null}
    </DetailShell>
  )
}
