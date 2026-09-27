import { useParams } from 'react-router-dom'
import { DetailShell } from '../../components/layout/DetailShell.jsx'
import { Badge, statusTone } from '../../components/ui/Badge.jsx'
import { ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner } from '../../components/ui/Spinner.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { adminApi } from '../../api/endpoints.js'
import { formatDateTime } from '../../utils/format.js'

/** Admin drill-down: one audit event with every field exposed (UC-11). */
export function AdminAuditDetail() {
  const { id } = useParams()
  const { data, loading, error, reload } = useAsync(() => adminApi.auditLog(id), [id])
  const event = data?.data

  const rows = event
    ? [
        ['Time', formatDateTime(event.eventTime)],
        ['Event ID', event.eventId],
        ['Action', event.eventAction],
        ['User', event.userIdentifier || '—'],
        ['Role', event.role || '—'],
        ['Authentication factor', event.authFactor || '—'],
        ['Mode', event.authMode || '—'],
        ['IP address', event.clientIp || '—'],
        ['Session ID', event.sessionId || '—'],
        ['Failure reason', event.failureReason || '—'],
        ['Correlation ID', event.correlationId || '—'],
      ]
    : []

  let prettyDetail = null
  if (event?.detail) {
    try {
      prettyDetail = JSON.stringify(JSON.parse(event.detail), null, 2)
    } catch {
      prettyDetail = event.detail
    }
  }

  return (
    <DetailShell
      title={event ? `Event: ${event.eventAction}` : 'Audit log details'}
      subtitle="Audit log — UC-11"
      fallback="/admin/audit-logs"
    >
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {event ? (
        <div className="space-y-6">
          <section className="card">
            <div className="flex flex-wrap items-center gap-2">
              <Badge tone={statusTone(event.status)}>{event.status}</Badge>
              <Badge tone="brand">{event.eventAction}</Badge>
              {event.authMode ? <Badge tone="neutral">Mode {event.authMode}</Badge> : null}
              {event.authFactor ? <Badge tone="sky">{event.authFactor}</Badge> : null}
            </div>
            <dl className="mt-4 grid grid-cols-1 gap-x-8 gap-y-3 text-sm sm:grid-cols-2">
              {rows.map(([label, value]) => (
                <div key={label} className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">{label}</dt>
                  <dd className="max-w-[60%] truncate text-right font-semibold text-ink-900" title={String(value)}>
                    {value}
                  </dd>
                </div>
              ))}
            </dl>
          </section>

          {prettyDetail ? (
            <section className="card">
              <h2 className="text-base font-bold text-ink-900">Additional details</h2>
              <pre className="mt-3 overflow-x-auto rounded-xl bg-surface-soft p-4 text-xs text-ink-600">
                {prettyDetail}
              </pre>
            </section>
          ) : null}
        </div>
      ) : null}
    </DetailShell>
  )
}
