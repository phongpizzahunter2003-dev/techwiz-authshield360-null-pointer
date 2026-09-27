import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { DetailShell } from '../../components/layout/DetailShell.jsx'
import { Badge, statusTone } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { assignmentApi, submissionApi } from '../../api/endpoints.js'
import { formatBytes, formatDateTime } from '../../utils/format.js'

/** Teacher drill-down: assignment policy + all submissions with inline grading. */
export function TeacherAssignmentDetail() {
  const { id } = useParams()
  const toast = useToast()
  const detail = useAsync(() => assignmentApi.detail(id), [id])
  const subs = useAsync(() => submissionApi.forAssignment(id), [id])
  const [draft, setDraft] = useState({})
  const [savingId, setSavingId] = useState(null)
  const [closing, setClosing] = useState(false)

  const assignment = detail.data?.data
  const submissions = subs.data?.data || []

  const save = async (submission) => {
    if (savingId) return
    const d = draft[submission.id] || { score: submission.score ?? '', feedback: submission.feedback ?? '' }
    setSavingId(submission.id)
    try {
      await submissionApi.grade(submission.id, { score: Number(d.score), feedback: d.feedback || '' })
      toast.success('Graded.')
      await subs.reload()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSavingId(null)
    }
  }

  const close = async () => {
    if (closing) return
    setClosing(true)
    try {
      await assignmentApi.close(id)
      toast.success('Assignment closed.')
      await detail.reload()
    } catch (err) {
      toast.error(err.message)
    }
  }

  return (
    <DetailShell
      title={assignment?.title || 'Assignment details'}
      subtitle={assignment?.classroomName}
      fallback="/teacher/assignments"
      actions={
        assignment && assignment.status !== 'CLOSED' ? (
          <button type="button" className="btn-danger" onClick={close} disabled={closing}>
            {closing ? <InlineSpinner /> : '🔒'} Close assignment
          </button>
        ) : null
      }
    >
      {detail.loading ? <Spinner /> : null}
      {detail.error ? <ErrorState message={detail.error.message} onRetry={detail.reload} /> : null}

      {assignment ? (
        <div className="space-y-6">
          <section className="card">
            <div className="flex flex-wrap items-center gap-2">
              <Badge tone={statusTone(assignment.status)}>{assignment.status}</Badge>
              <Badge tone="sun" icon="⏰">Due date: {formatDateTime(assignment.dueAt)}</Badge>
              <Badge tone="sky" icon="🎒">{assignment.classroomName}</Badge>
              <Badge tone="neutral">
                {assignment.allowResubmission ? `Resubmit allowed (max ${assignment.maxAttempts})` : 'Resubmission not allowed'}
              </Badge>
              <Badge tone="neutral">{assignment.allowLate ? `Late -${assignment.latePenaltyPct}%` : 'No late submissions'}</Badge>
            </div>
            {assignment.description ? (
              <p className="mt-3 whitespace-pre-line text-sm text-ink-600">{assignment.description}</p>
            ) : null}
          </section>

          <section className="card">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-bold text-ink-900">Submissions ({submissions.length})</h2>
              <Link className="text-sm font-semibold text-brand-600 hover:underline" to="/teacher/assignments">
                Back to list
              </Link>
            </div>

            {subs.loading ? <Spinner /> : null}
            {subs.error ? <ErrorState message={subs.error.message} onRetry={subs.reload} /> : null}
            {!subs.loading && submissions.length === 0 ? (
              <EmptyState icon="📥" title="No submissions yet" description="Students have not submitted anything for this assignment." />
            ) : null}

            {submissions.length > 0 ? (
              <ul className="mt-3 space-y-3">
                {submissions.map((s) => {
                  const d = draft[s.id] || { score: s.score ?? '', feedback: s.feedback ?? '' }
                  return (
                    <li key={s.id} className="rounded-2xl border border-brand-100 p-3">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <div className="min-w-0">
                          <Link
                            to={`/teacher/students/${s.studentId}`}
                            className="truncate text-sm font-bold text-ink-900 hover:underline"
                          >
                            {s.studentName}
                          </Link>
                          <p className="truncate text-xs text-ink-400">
                            Attempt #{s.attemptNumber} · {s.originalName} · {formatBytes(s.sizeBytes)} ·{' '}
                            {formatDateTime(s.submittedAt)}
                          </p>
                        </div>
                        <div className="flex items-center gap-2">
                          <Badge tone={s.late ? 'coral' : 'accent'}>{s.late ? 'Late' : 'On time'}</Badge>
                          <a
                            className="text-sm font-semibold text-brand-600 hover:underline"
                            href={submissionApi.fileUrl(s.id)}
                            target="_blank"
                            rel="noreferrer"
                          >
                            Download file
                          </a>
                        </div>
                      </div>
                      <div className="mt-3 flex flex-wrap items-end gap-2">
                        <div className="w-24">
                          <label className="label" htmlFor={`score-${s.id}`}>
                            Score
                          </label>
                          <input
                            id={`score-${s.id}`}
                            type="number"
                            min="0"
                            max={assignment.maxScore}
                            className="input"
                            value={d.score}
                            onChange={(e) => setDraft({ ...draft, [s.id]: { ...d, score: e.target.value } })}
                          />
                        </div>
                        <div className="min-w-[200px] flex-1">
                          <label className="label" htmlFor={`fb-${s.id}`}>
                            Feedback
                          </label>
                          <input
                            id={`fb-${s.id}`}
                            className="input"
                            value={d.feedback}
                            onChange={(e) => setDraft({ ...draft, [s.id]: { ...d, feedback: e.target.value } })}
                          />
                        </div>
                        <button
                          type="button"
                          className="btn-accent"
                          onClick={() => save(s)}
                          disabled={savingId === s.id}
                        >
                          {savingId === s.id ? <InlineSpinner /> : null} Save score
                        </button>
                      </div>
                    </li>
                  )
                })}
              </ul>
            ) : null}
          </section>
        </div>
      ) : null}
    </DetailShell>
  )
}
