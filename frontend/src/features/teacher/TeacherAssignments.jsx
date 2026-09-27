import { useState } from 'react'
import { Link } from 'react-router-dom'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { Badge, statusTone } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { Modal } from '../../components/ui/Modal.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { assignmentApi, classroomApi, submissionApi } from '../../api/endpoints.js'
import { fromLocalInputValue, toLocalInputValue, formatDateTime } from '../../utils/format.js'

const EMPTY_FORM = {
  title: '',
  description: '',
  classroomId: '',
  dueAt: toLocalInputValue(new Date(Date.now() + 7 * 86400000)),
  allowLate: true,
  lateCutoffAt: '',
  latePenaltyPct: 10,
  allowResubmission: true,
  maxAttempts: 3,
  maxScore: 100,
  status: 'PUBLISHED',
}

export function TeacherAssignments() {
  const toast = useToast()
  const assignments = useAsync(() => assignmentApi.list(), [])
  const classrooms = useAsync(() => classroomApi.list(), [])

  const [editorOpen, setEditorOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  const [grading, setGrading] = useState(null)
  const [submissions, setSubmissions] = useState([])
  const [loadingSubs, setLoadingSubs] = useState(false)
  const [gradeDraft, setGradeDraft] = useState({})

  const list = assignments.data?.data || []
  const classList = classrooms.data?.data || []

  const openCreate = () => {
    setEditing(null)
    setForm({ ...EMPTY_FORM, classroomId: classList[0]?.id ? String(classList[0].id) : '' })
    setEditorOpen(true)
  }

  const openEdit = (a) => {
    setEditing(a)
    setForm({
      title: a.title,
      description: a.description || '',
      classroomId: String(a.classroomId),
      dueAt: toLocalInputValue(a.dueAt),
      allowLate: a.allowLate,
      lateCutoffAt: a.lateCutoffAt ? toLocalInputValue(a.lateCutoffAt) : '',
      latePenaltyPct: a.latePenaltyPct,
      allowResubmission: a.allowResubmission,
      maxAttempts: a.maxAttempts,
      maxScore: a.maxScore,
      status: a.status,
    })
    setEditorOpen(true)
  }

  const save = async (event) => {
    event.preventDefault()
    setSaving(true)
    const payload = {
      title: form.title,
      description: form.description,
      classroomId: Number(form.classroomId),
      dueAt: fromLocalInputValue(form.dueAt),
      allowLate: form.allowLate,
      lateCutoffAt: form.lateCutoffAt ? fromLocalInputValue(form.lateCutoffAt) : null,
      latePenaltyPct: Number(form.latePenaltyPct),
      allowResubmission: form.allowResubmission,
      maxAttempts: Number(form.maxAttempts),
      maxScore: Number(form.maxScore),
      status: form.status,
    }
    try {
      if (editing) {
        await assignmentApi.update(editing.id, payload)
        toast.success('Cập nhật bài tập thành công.')
      } else {
        await assignmentApi.create(payload)
        toast.success('Tạo bài tập thành công.')
      }
      setEditorOpen(false)
      await assignments.reload()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  const closeAssignment = async (a) => {
    try {
      await assignmentApi.close(a.id)
      toast.success('Đã đóng bài tập.')
      await assignments.reload()
    } catch (err) {
      toast.error(err.message)
    }
  }

  const openGrading = async (a) => {
    setGrading(a)
    setLoadingSubs(true)
    setGradeDraft({})
    try {
      const res = await submissionApi.forAssignment(a.id)
      setSubmissions(res.data || [])
    } catch (err) {
      toast.error(err.message)
      setSubmissions([])
    } finally {
      setLoadingSubs(false)
    }
  }

  const submitGrade = async (submission) => {
    const draft = gradeDraft[submission.id] || {}
    try {
      await submissionApi.grade(submission.id, {
        score: Number(draft.score),
        feedback: draft.feedback || '',
      })
      toast.success('Đã chấm điểm.')
      await openGrading(grading)
    } catch (err) {
      toast.error(err.message)
    }
  }

  return (
    <AppShell
      title="Quản lý bài tập"
      subtitle="Tạo, chỉnh sửa, đóng bài tập và chấm điểm bài nộp"
      actions={
        <button type="button" className="btn-primary" onClick={openCreate}>
          ➕ Tạo bài tập
        </button>
      }
    >
      {assignments.loading ? <Spinner /> : null}
      {assignments.error ? <ErrorState message={assignments.error.message} onRetry={assignments.reload} /> : null}

      {!assignments.loading && list.length === 0 ? (
        <EmptyState icon="📝" title="Chưa có bài tập" description="Bấm 'Tạo bài tập' để giao bài cho lớp." />
      ) : null}

      {list.length > 0 ? (
        <div className="table-wrap">
          <table className="table min-w-[900px]">
            <thead>
              <tr>
                <th>Tiêu đề</th>
                <th>Lớp</th>
                <th>Hạn nộp</th>
                <th>Cho nộp muộn</th>
                <th>Nộp lại</th>
                <th>Trạng thái</th>
                <th className="text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {list.map((a) => (
                <tr key={a.id}>
                  <td className="font-semibold text-ink-900">{a.title}</td>
                  <td className="whitespace-nowrap">{a.classroomName}</td>
                  <td className="whitespace-nowrap">{formatDateTime(a.dueAt)}</td>
                  <td className="whitespace-nowrap">{a.allowLate ? `Có (${a.latePenaltyPct}%)` : 'Không'}</td>
                  <td className="whitespace-nowrap">
                    {a.allowResubmission ? `Có (tối đa ${a.maxAttempts})` : 'Không'}
                  </td>
                  <td className="whitespace-nowrap">
                    <Badge tone={statusTone(a.status)}>{a.status}</Badge>
                  </td>
                  <td>
                    <div className="table-actions">
                      <Link className="btn-xs btn-ghost" to={`/teacher/assignments/${a.id}`}>
                        Chi tiết
                      </Link>
                      <button type="button" className="btn-xs btn-ghost" onClick={() => openGrading(a)}>
                        Chấm điểm
                      </button>
                      <button type="button" className="btn-xs btn-ghost" onClick={() => openEdit(a)}>
                        Sửa
                      </button>
                      {a.status !== 'CLOSED' ? (
                        <button type="button" className="btn-xs btn-danger" onClick={() => closeAssignment(a)}>
                          Đóng
                        </button>
                      ) : null}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {/* Create / edit */}
      <Modal
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        title={editing ? 'Chỉnh sửa bài tập' : 'Tạo bài tập mới'}
        size="lg"
      >
        <form onSubmit={save} className="space-y-4">
          <div>
            <label className="label" htmlFor="as-title">
              Tiêu đề
            </label>
            <input
              id="as-title"
              className="input"
              required
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
            />
          </div>
          <div>
            <label className="label" htmlFor="as-class">
              Lớp học
            </label>
            <select
              id="as-class"
              className="input"
              required
              value={form.classroomId}
              onChange={(e) => setForm({ ...form, classroomId: e.target.value })}
            >
              <option value="">-- Chọn lớp --</option>
              {classList.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.code})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="label" htmlFor="as-desc">
              Mô tả
            </label>
            <textarea
              id="as-desc"
              className="input min-h-[90px]"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="label" htmlFor="as-due">
                Hạn nộp
              </label>
              <input
                id="as-due"
                type="datetime-local"
                className="input"
                required
                value={form.dueAt}
                onChange={(e) => setForm({ ...form, dueAt: e.target.value })}
              />
            </div>
            <div>
              <label className="label" htmlFor="as-max">
                Điểm tối đa
              </label>
              <input
                id="as-max"
                type="number"
                min="1"
                className="input"
                value={form.maxScore}
                onChange={(e) => setForm({ ...form, maxScore: e.target.value })}
              />
            </div>
          </div>

          <fieldset className="rounded-2xl border border-brand-100 p-4">
            <legend className="px-2 text-xs font-bold uppercase text-ink-600">Chính sách nộp bài</legend>
            <div className="space-y-3">
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={form.allowLate}
                  onChange={(e) => setForm({ ...form, allowLate: e.target.checked })}
                />
                Cho phép nộp muộn (UC-A2)
              </label>
              {form.allowLate ? (
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <div>
                    <label className="label" htmlFor="as-cutoff">
                      Hạn cuối nộp muộn
                    </label>
                    <input
                      id="as-cutoff"
                      type="datetime-local"
                      className="input"
                      value={form.lateCutoffAt}
                      onChange={(e) => setForm({ ...form, lateCutoffAt: e.target.value })}
                    />
                  </div>
                  <div>
                    <label className="label" htmlFor="as-penalty">
                      Trừ điểm muộn (%)
                    </label>
                    <input
                      id="as-penalty"
                      type="number"
                      min="0"
                      max="100"
                      className="input"
                      value={form.latePenaltyPct}
                      onChange={(e) => setForm({ ...form, latePenaltyPct: e.target.value })}
                    />
                  </div>
                </div>
              ) : null}
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={form.allowResubmission}
                  onChange={(e) => setForm({ ...form, allowResubmission: e.target.checked })}
                />
                Cho phép cập nhật / nộp lại bài (UC-A4)
              </label>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <div>
                  <label className="label" htmlFor="as-attempts">
                    Số lần nộp tối đa
                  </label>
                  <input
                    id="as-attempts"
                    type="number"
                    min="1"
                    className="input"
                    value={form.maxAttempts}
                    onChange={(e) => setForm({ ...form, maxAttempts: e.target.value })}
                  />
                </div>
                <div>
                  <label className="label" htmlFor="as-status">
                    Trạng thái
                  </label>
                  <select
                    id="as-status"
                    className="input"
                    value={form.status}
                    onChange={(e) => setForm({ ...form, status: e.target.value })}
                  >
                    <option value="DRAFT">Bản nháp</option>
                    <option value="PUBLISHED">Đang mở</option>
                    <option value="CLOSED">Đã đóng</option>
                  </select>
                </div>
              </div>
            </div>
          </fieldset>

          <div className="flex justify-end gap-2">
            <button type="button" className="btn-ghost" onClick={() => setEditorOpen(false)}>
              Hủy
            </button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? <InlineSpinner /> : '💾'} Lưu
            </button>
          </div>
        </form>
      </Modal>

      {/* Grading */}
      <Modal open={Boolean(grading)} onClose={() => setGrading(null)} title={`Chấm điểm — ${grading?.title || ''}`} size="lg">
        {loadingSubs ? <Spinner /> : null}
        {!loadingSubs && submissions.length === 0 ? (
          <EmptyState icon="📥" title="Chưa có bài nộp" description="Học sinh chưa nộp bài cho bài tập này." />
        ) : null}
        {!loadingSubs && submissions.length > 0 ? (
          <ul className="space-y-3">
            {submissions.map((s) => {
              const draft = gradeDraft[s.id] || { score: s.score ?? '', feedback: s.feedback ?? '' }
              return (
                <li key={s.id} className="rounded-2xl border border-brand-100 p-3">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-bold text-ink-900">{s.studentName}</p>
                      <p className="truncate text-xs text-ink-400">
                        Lần #{s.attemptNumber} · {s.originalName} · {formatDateTime(s.submittedAt)}
                      </p>
                    </div>
                    <Badge tone={s.late ? 'coral' : 'accent'}>{s.late ? 'Muộn' : 'Đúng hạn'}</Badge>
                  </div>
                  <div className="mt-3 flex flex-wrap items-end gap-2">
                    <div className="w-24">
                      <label className="label" htmlFor={`score-${s.id}`}>
                        Điểm
                      </label>
                      <input
                        id={`score-${s.id}`}
                        type="number"
                        min="0"
                        className="input"
                        value={draft.score}
                        onChange={(e) => setGradeDraft({ ...gradeDraft, [s.id]: { ...draft, score: e.target.value } })}
                      />
                    </div>
                    <div className="min-w-[200px] flex-1">
                      <label className="label" htmlFor={`fb-${s.id}`}>
                        Nhận xét
                      </label>
                      <input
                        id={`fb-${s.id}`}
                        className="input"
                        value={draft.feedback}
                        onChange={(e) => setGradeDraft({ ...gradeDraft, [s.id]: { ...draft, feedback: e.target.value } })}
                      />
                    </div>
                    <button type="button" className="btn-accent" onClick={() => submitGrade(s)}>
                      Lưu điểm
                    </button>
                  </div>
                </li>
              )
            })}
          </ul>
        ) : null}
      </Modal>
    </AppShell>
  )
}
