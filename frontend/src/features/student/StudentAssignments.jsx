import { useRef, useState } from 'react'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { Badge, statusTone } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { Modal } from '../../components/ui/Modal.jsx'
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

export function StudentAssignments() {
  const toast = useToast()
  const { data, loading, error, reload } = useAsync(() => assignmentApi.list(), [])
  const [active, setActive] = useState(null)
  const [history, setHistory] = useState([])
  const [loadingHistory, setLoadingHistory] = useState(false)
  const [file, setFile] = useState(null)
  const [uploading, setUploading] = useState(false)
  const fileRef = useRef(null)

  const assignments = data?.data || []

  const openAssignment = async (assignment) => {
    setActive(assignment)
    setFile(null)
    setHistory([])
    if (fileRef.current) fileRef.current.value = ''
    if (assignment.yourAttempts > 0) {
      setLoadingHistory(true)
      try {
        const res = await submissionApi.history(assignment.id)
        setHistory(res.data || [])
      } catch {
        setHistory([])
      } finally {
        setLoadingHistory(false)
      }
    }
  }

  const closeModal = () => {
    setActive(null)
    setFile(null)
    setHistory([])
  }

  const onUpload = async () => {
    if (!file || uploading) return
    setUploading(true)
    try {
      const res = await submissionApi.submit(active.id, file)
      const status = res.data.submissionStatus === 'LATE' ? 'nộp muộn' : 'đúng hạn'
      toast.success(`Nộp bài thành công (${status}), lần nộp #${res.data.attemptNumber}.`)
      await reload()
      await openAssignmentEvery(res.data.assignmentId)
    } catch (err) {
      const message = LOCK_MESSAGES[err.code] || err.message
      toast.error(message)
    } finally {
      setUploading(false)
    }
  }

  // Re-open the refreshed assignment to show the new attempt.
  const openAssignmentEvery = async (assignmentId) => {
    try {
      const res = await assignmentApi.detail(assignmentId)
      const assignment = res.data
      setActive(assignment)
      const hist = await submissionApi.history(assignmentId)
      setHistory(hist.data || [])
    } catch {
      /* ignore */
    }
  }

  return (
    <AppShell title="Bài tập của tôi" subtitle="Nộp đúng hạn, nộp muộn, hoặc cập nhật bài đã nộp">
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {!loading && !error && assignments.length === 0 ? (
        <EmptyState icon="📚" title="Chưa có bài tập" description="Hiện chưa có bài tập nào cho lớp của bạn." />
      ) : null}

      <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
        {assignments.map((a) => (
          <article key={a.id} className="card flex flex-col">
            <div className="flex items-start justify-between gap-2">
              <h3 className="text-sm font-bold text-ink-900">{a.title}</h3>
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

            <button type="button" className="btn-primary mt-4 w-full" onClick={() => openAssignment(a)}>
              {a.canSubmit ? (a.yourAttempts > 0 ? 'Cập nhật bài nộp' : 'Nộp bài') : 'Xem chi tiết'}
            </button>
          </article>
        ))}
      </div>

      <Modal open={Boolean(active)} onClose={closeModal} title={active?.title || ''} size="lg">
        {active ? (
          <div className="space-y-4">
            <div className="flex flex-wrap gap-2">
              <Badge tone={statusTone(active.status)}>Trạng thái: {active.status}</Badge>
              <Badge tone="sky" icon="🏫">
                {active.classroomName}
              </Badge>
              <Badge tone="sun" icon="⏰">
                Hạn: {formatDateTime(active.dueAt)}
              </Badge>
              <Badge tone="neutral">
                {active.yourAttempts ?? 0}/{active.maxAttempts} lần nộp
              </Badge>
            </div>

            {active.description ? <p className="text-sm text-ink-600">{active.description}</p> : null}

            {active.canSubmit ? (
              <div className="rounded-2xl border border-brand-100 bg-surface-soft p-4">
                <p className="text-sm font-bold text-ink-900">
                  {active.yourAttempts > 0 ? 'Cập nhật tệp bài nộp' : 'Nộp bài của bạn'}
                </p>
                <p className="mt-1 text-xs text-ink-400">
                  Định dạng hỗ trợ: pdf, doc(x), ppt(x), xls(x), txt, zip, ảnh. Tối đa 10MB.
                </p>
                <input
                  ref={fileRef}
                  type="file"
                  className="mt-3 block w-full text-sm text-ink-600 file:mr-3 file:rounded-xl file:border-0 file:bg-brand-500 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-white hover:file:bg-brand-600"
                  onChange={(e) => setFile(e.target.files?.[0] || null)}
                />
                <button
                  type="button"
                  className="btn-primary mt-3 w-full"
                  onClick={onUpload}
                  disabled={!file || uploading}
                >
                  {uploading ? <InlineSpinner /> : '⬆️'}
                  {active.yourAttempts > 0 ? 'Nộp lại (tạo lần nộp mới)' : 'Nộp bài'}
                </button>
              </div>
            ) : (
              <div className="rounded-2xl border border-coral-100 bg-coral-100/40 p-4 text-sm text-ink-900">
                <p className="font-bold">Không thể nộp/cập nhật bài</p>
                <p className="mt-1">{LOCK_MESSAGES[active.lockReason] || 'Bài tập hiện không cho phép nộp.'}</p>
              </div>
            )}

            <div>
              <h4 className="text-sm font-bold text-ink-900">Lịch sử nộp bài</h4>
              {loadingHistory ? (
                <Spinner size="sm" label="Đang tải lịch sử..." />
              ) : history.length === 0 ? (
                <p className="mt-2 text-xs text-ink-400">Bạn chưa nộp lần nào.</p>
              ) : (
                <div className="table-wrap mt-2">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Lần</th>
                        <th>Tệp</th>
                        <th>Thời điểm</th>
                        <th>Trạng thái</th>
                        <th>Điểm</th>
                      </tr>
                    </thead>
                    <tbody>
                      {history.map((h) => (
                        <tr key={h.id}>
                          <td>
                            #{h.attemptNumber} {h.current ? <Badge tone="brand">Đang dùng</Badge> : null}
                          </td>
                          <td className="max-w-[180px] truncate">{h.originalName}</td>
                          <td>{formatDateTime(h.submittedAt)}</td>
                          <td>
                            <Badge tone={h.late ? 'coral' : 'accent'}>{h.late ? 'Muộn' : 'Đúng hạn'}</Badge>
                          </td>
                          <td>
                            {h.score ?? '—'}
                            {h.feedback ? <span className="ml-1 text-xs text-ink-400">({h.feedback})</span> : null}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        ) : null}
      </Modal>
    </AppShell>
  )
}
