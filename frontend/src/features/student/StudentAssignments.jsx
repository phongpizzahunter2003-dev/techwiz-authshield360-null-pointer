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
  { key: 'ALL', label: 'Tất cả', icon: '📚' },
  { key: 'PENDING', label: 'Cần nộp', icon: '✍️' },
  { key: 'ON_TIME', label: 'Đã nộp đúng hạn', icon: '✅' },
  { key: 'LATE', label: 'Đã nộp muộn', icon: '🐢' },
  { key: 'LOCKED', label: 'Không thể nộp', icon: '🔒' },
]

const LOCK_MESSAGES = {
  SUBMISSION_LOCKED: 'Bài tập đã đóng.',
  RESUBMISSION_NOT_ALLOWED: 'Không cho phép nộp lại.',
  MAX_ATTEMPTS_REACHED: 'Đã đạt số lần nộp tối đa.',
  LATE_NOT_ALLOWED: 'Đã quá hạn nộp bài.',
}

/** Filterable, clickable assignment list. The `bucket` query param comes from dashboard charts. */
export function StudentAssignments() {
  const [params, setParams] = useSearchParams()
  const bucket = (params.get('bucket') || 'ALL').toUpperCase()
  const { data, loading, error, reload } = useAsync(() => analyticsApi.studentAssignments(bucket), [bucket])

  const assignments = data?.data || []

  return (
    <AppShell title="Bài tập của tôi" subtitle="Nộp đúng hạn, nộp muộn, hoặc cập nhật bài đã nộp">
      <div className="mb-4 flex flex-wrap items-center gap-2">
        {bucket !== 'ALL' ? <BackButton fallback="/student" label="Quay lại" /> : null}
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
          title="Không có bài tập nào trong mục này"
          description="Hãy chọn bộ lọc khác hoặc quay lại tổng quan."
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
                <dt className="text-ink-400">Hạn nộp</dt>
                <dd className="font-semibold">{formatDateTime(a.dueAt)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Số lần nộp</dt>
                <dd className="font-semibold">
                  {a.yourAttempts ?? 0}/{a.maxAttempts}
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-ink-400">Nộp muộn</dt>
                <dd className="font-semibold">{a.allowLate ? 'Được phép' : 'Không'}</dd>
              </div>
            </dl>

            <div className="mt-3 flex flex-wrap gap-2">
              {a.latestSubmission ? <SubmissionStatusBadge status={a.latestSubmission.submissionStatus} /> : null}
              {a.canSubmit ? (
                <Badge tone="accent" icon="✍️">
                  {a.yourAttempts > 0 ? 'Có thể cập nhật' : 'Có thể nộp'}
                </Badge>
              ) : (
                <Badge tone="coral" icon="🔒">
                  {LOCK_MESSAGES[a.lockReason] || 'Không thể nộp'}
                </Badge>
              )}
            </div>

            <Link className="btn-primary mt-4 w-full" to={`/student/assignments/${a.id}`}>
              {a.canSubmit ? (a.yourAttempts > 0 ? 'Cập nhật bài nộp' : 'Nộp bài') : 'Xem chi tiết'}
            </Link>
          </article>
        ))}
      </div>
    </AppShell>
  )
}
