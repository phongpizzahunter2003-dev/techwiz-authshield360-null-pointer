import { useEffect, useRef, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { DetailShell } from '../../components/layout/DetailShell.jsx'
import { Badge, statusTone, SubmissionStatusBadge } from '../../components/ui/Badge.jsx'
import { ErrorState, EmptyState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { assignmentApi, submissionApi } from '../../api/endpoints.js'
import { formatBytes, formatDateTime } from '../../utils/format.js'

const LOCK_MESSAGES = {
  SUBMISSION_LOCKED: 'Bài tập đã đóng. Bạn không thể cập nhật bài nộp.',
  RESUBMISSION_NOT_ALLOWED: 'Bài tập này không cho phép nộp lại (có thể đã được chấm điểm).',
  MAX_ATTEMPTS_REACHED: 'Bạn đã đạt số lần nộp tối đa.',
  LATE_NOT_ALLOWED: 'Đã quá hạn nộp bài. Hệ thống không nhận bài muộn.',
}

/** UC-A1..A4 detail view: full assignment info, submit/resubmit panel and attempt history. */
export function StudentAssignmentDetail() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const toast = useToast()
  const { data, loading, error, reload } = useAsync(() => assignmentApi.detail(id), [id])
  const [file, setFile] = useState(null)
  const [uploading, setUploading] = useState(false)
  const fileRef = useRef(null)
  const submitRef = useRef(null)

  const assignment = data?.data
  const history = assignment?.submissions || []

  // Arrived via the "Nộp bài" button → jump straight to the upload panel.
  useEffect(() => {
    if (assignment && searchParams.get('action') === 'submit' && submitRef.current) {
      submitRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }, [assignment, searchParams])

  const upload = async () => {
    if (!file || uploading) return
    setUploading(true)
    try {
      const res = await submissionApi.submit(id, file)
      const status = res.data.submissionStatus === 'LATE' ? 'nộp muộn' : 'đúng hạn'
      toast.success(`Nộp bài thành công (${status}), lần nộp #${res.data.attemptNumber}.`)
      setFile(null)
      if (fileRef.current) fileRef.current.value = ''
      await reload()
    } catch (err) {
      toast.error(LOCK_MESSAGES[err.code] || err.message)
    } finally {
      setUploading(false)
    }
  }

  return (
    <DetailShell
      title={assignment?.title || 'Chi tiết bài tập'}
      subtitle={assignment?.classroomName}
      fallback="/student/assignments"
    >
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {assignment ? (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <section className="card lg:col-span-2">
            <h2 className="text-base font-bold text-ink-900">Thông tin bài tập</h2>
            <div className="mt-3 flex flex-wrap gap-2">
              <Badge tone={statusTone(assignment.status)}>Trạng thái: {assignment.status}</Badge>
              <Badge tone="sun" icon="⏰">Hạn nộp: {formatDateTime(assignment.dueAt)}</Badge>
              <Badge tone="sky" icon="🎒">{assignment.classroomName}</Badge>
              <Badge tone="neutral">Điểm tối đa: {assignment.maxScore}</Badge>
            </div>
            {assignment.description ? (
              <p className="mt-4 whitespace-pre-line text-sm text-ink-600">{assignment.description}</p>
            ) : null}

            <dl className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div className="rounded-xl bg-surface-soft p-3">
                <dt className="text-xs font-bold uppercase text-ink-400">Số lần đã nộp</dt>
                <dd className="text-lg font-extrabold text-ink-900">
                  {assignment.yourAttempts ?? 0}/{assignment.maxAttempts}
                </dd>
              </div>
              <div className="rounded-xl bg-surface-soft p-3">
                <dt className="text-xs font-bold uppercase text-ink-400">Nộp muộn</dt>
                <dd className="text-lg font-extrabold text-ink-900">
                  {assignment.allowLate ? `Được phép (trừ ${assignment.latePenaltyPct}%)` : 'Không cho phép'}
                </dd>
              </div>
              <div className="rounded-xl bg-surface-soft p-3">
                <dt className="text-xs font-bold uppercase text-ink-400">Cho phép nộp lại</dt>
                <dd className="text-lg font-extrabold text-ink-900">{assignment.allowResubmission ? 'Có' : 'Không'}</dd>
              </div>
              <div className="rounded-xl bg-surface-soft p-3">
                <dt className="text-xs font-bold uppercase text-ink-400">Giáo viên giao bài</dt>
                <dd className="text-sm font-semibold text-ink-900">{assignment.createdByName || '—'}</dd>
              </div>
            </dl>
          </section>

          <section
            ref={submitRef}
            className={`card ${searchParams.get('action') === 'submit' && assignment.canSubmit ? 'ring-2 ring-brand-400' : ''}`}
          >
            <h2 className="text-base font-bold text-ink-900">
              {assignment.canSubmit
                ? assignment.yourAttempts > 0
                  ? 'Cập nhật bài nộp (UC-A4)'
                  : 'Nộp bài (UC-A1/A2)'
                : 'Không thể nộp (UC-A3)'}
            </h2>

            {assignment.canSubmit ? (
              <>
                <p className="mt-1 text-xs text-ink-400">
                  Định dạng: pdf, doc(x), ppt(x), xls(x), txt, zip, ảnh · tối đa 10MB.
                </p>
                <input
                  ref={fileRef}
                  type="file"
                  className="mt-3 block w-full text-sm text-ink-600 file:mr-3 file:rounded-xl file:border-0 file:bg-brand-500 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-white hover:file:bg-brand-600"
                  onChange={(e) => setFile(e.target.files?.[0] || null)}
                />
                <button type="button" className="btn-primary mt-3 w-full" onClick={upload} disabled={!file || uploading}>
                  {uploading ? <InlineSpinner /> : '⬆️'}
                  {assignment.yourAttempts > 0 ? 'Nộp lại (tạo lần nộp mới)' : 'Nộp bài'}
                </button>
              </>
            ) : (
              <p className="mt-2 rounded-xl border border-coral-100 bg-coral-100/40 p-3 text-sm text-ink-900">
                {LOCK_MESSAGES[assignment.lockReason] || 'Bài tập hiện không cho phép nộp.'}
              </p>
            )}
          </section>

          <section className="card lg:col-span-3">
            <h2 className="text-base font-bold text-ink-900">Lịch sử nộp bài</h2>
            {history.length === 0 ? (
              <EmptyState icon="📥" title="Chưa nộp lần nào" description="Bạn chưa nộp bài cho bài tập này." />
            ) : (
              <div className="table-wrap mt-3">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Lần</th>
                      <th>Tệp</th>
                      <th>Dung lượng</th>
                      <th>Thời điểm</th>
                      <th>Trạng thái</th>
                      <th>Điểm</th>
                      <th>Nhận xét</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((h) => (
                      <tr key={h.id}>
                        <td>
                          #{h.attemptNumber} {h.current ? <Badge tone="brand">Đang dùng</Badge> : null}
                        </td>
                        <td className="max-w-[200px] truncate">{h.originalName}</td>
                        <td>{formatBytes(h.sizeBytes)}</td>
                        <td>{formatDateTime(h.submittedAt)}</td>
                        <td>
                          <SubmissionStatusBadge status={h.submissionStatus} />
                        </td>
                        <td className="font-bold">{h.score ?? '—'}</td>
                        <td className="max-w-[220px] truncate text-xs text-ink-400">{h.feedback || '—'}</td>
                        <td>
                          <a
                            className="text-sm font-semibold text-brand-600 hover:underline"
                            href={submissionApi.fileUrl(h.id)}
                            target="_blank"
                            rel="noreferrer"
                          >
                            Tải
                          </a>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      ) : null}
    </DetailShell>
  )
}
