import { Link, useSearchParams } from 'react-router-dom'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { Badge, statusTone, SubmissionStatusBadge } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner } from '../../components/ui/Spinner.jsx'
import { BackButton } from '../../components/ui/BackButton.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { analyticsApi } from '../../api/endpoints.js'
import { formatDateTime } from '../../utils/format.js'

const BUCKETS = [
  { key: 'ALL', label: 'All', icon: '📚' },
  { key: 'PENDING', label: 'To submit', icon: '✍️' },
  { key: 'ON_TIME', label: 'Submitted on time', icon: '✅' },
  { key: 'LATE', label: 'Submitted late', icon: '🐢' },
  { key: 'LOCKED', label: 'Cannot submit', icon: '🔒' },
]

const LOCK_MESSAGES = {
  SUBMISSION_LOCKED: 'This assignment is closed. You cannot update your submission.',
  RESUBMISSION_NOT_ALLOWED: 'Resubmission is not allowed for this assignment.',
  MAX_ATTEMPTS_REACHED: 'You have reached the maximum number of submissions.',
  LATE_NOT_ALLOWED: 'The deadline has passed. Late submissions are not accepted.',
}

/** Filterable, clickable assignment list. The `bucket` query param comes from dashboard charts. */
export function StudentAssignments() {
  const [params, setParams] = useSearchParams()
  const bucket = (params.get('bucket') || 'ALL').toUpperCase()
  const { data, loading, error, reload } = useAsync(() => analyticsApi.studentAssignments(bucket), [bucket])

  const assignments = data?.data || []

  return (
    <AppShell title="My assignments" subtitle="Submit on time, submit late, or update a submitted assignment">
      <div className="mb-4 flex flex-wrap items-center gap-2">
        {bucket !== 'ALL' ? <BackButton fallback="/student" label="Back" /> : null}
        {BUCKETS.map((b) => (
          <Link
            key={b.key}
            to={b.key === 'ALL' ? '/student/assignments' : `/student/assignments?bucket=${b.key}`}
            className={`chip ${bucket === b.key ? 'border-brand-400 bg-brand-50 text-brand-700' : ''}`}
          >
            <span aria-hidden="true">{b.icon}</span>
            {b.label}
          </Link>
        ))}
      </div>

      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {!loading && !error && assignments.length === 0 ? (
        <EmptyState
          icon="🔎"
          title="No assignments in this section"
          description="Choose a different filter or go back to the overview."
        />
      ) : null}

      <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
        {assignments.map((a) => (
          <article key={a.id} className="card flex flex-col">
            <div className="flex items-start justify-between gap-2">
              <Link to={`/student/assignments/${a.id}`} className="text-sm font-bold text-ink-900 hover:underline">
                {a.title}
              </Link>
              <Badge tone={statusTone(a.status)}>{a.status}</Badge>
            </div>
            <p className="mt-1 text-xs text-ink-400">{a.classroomName}</p>
            {a.description ? <p className="mt-2 line-clamp-2 text-xs text-ink-600">{a.description}</p> : null}

            <dl className="mt-3 space-y-1 text-xs text-ink-600">
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Due date</dt>
                <dd className="font-semibold">{formatDateTime(a.dueAt)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Attempts</dt>
                <dd className="font-semibold">
                  {a.yourAttempts ?? 0}/{a.maxAttempts}
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Late</dt>
                <dd className="font-semibold">{a.allowLate ? 'Allowed' : 'No'}</dd>
              </div>
            </dl>

            <div className="mt-3 flex flex-wrap gap-2">
              {a.latestSubmission ? <SubmissionStatusBadge status={a.latestSubmission.submissionStatus} /> : null}
              {a.canSubmit ? (
                <Badge tone="accent" icon="✍️">
                  {a.yourAttempts > 0 ? 'Can update' : 'Can submit'}
                </Badge>
              ) : (
                <Badge tone="coral" icon="🔒">
                  {LOCK_MESSAGES[a.lockReason] || 'Cannot submit'}
                </Badge>
              )}
            </div>

            <div className="mt-4 flex gap-2">
              <Link className="btn-ghost flex-1" to={`/student/assignments/${a.id}`}>
                View details
              </Link>
              {a.canSubmit ? (
                <Link className="btn-primary flex-1" to={`/student/assignments/${a.id}?action=submit`}>
                  {a.yourAttempts > 0 ? 'Update submission' : 'Submit'}
                </Link>
              ) : (
                <button
                  type="button"
                  className="btn-primary flex-1"
                  disabled
                  title={LOCK_MESSAGES[a.lockReason] || 'This assignment does not allow submissions right now'}
                >
                  Submit
                </button>
              )}
            </div>
          </article>
        ))}
      </div>
    </AppShell>
  )
}
