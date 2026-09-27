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
  const [closingId, setClosingId] = useState(null)
  const [openingId, setOpeningId] = useState(null)
  const [savingGradeId, setSavingGradeId] = useState(null)
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
        toast.success('Assignment updated successfully.')
      } else {
        await assignmentApi.create(payload)
        toast.success('Assignment created successfully.')
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
    if (closingId) return
    setClosingId(a.id)
    try {
      await assignmentApi.close(a.id)
      toast.success('Assignment closed.')
      await assignments.reload()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setClosingId(null)
    }
  }

  const openGrading = async (a) => {
    if (openingId) return
    setOpeningId(a.id)
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
      setOpeningId(null)
    }
  }

  const submitGrade = async (submission) => {
    if (savingGradeId) return
    const draft = gradeDraft[submission.id] || {}
    setSavingGradeId(submission.id)
    try {
      await submissionApi.grade(submission.id, {
        score: Number(draft.score),
        feedback: draft.feedback || '',
      })
      toast.success('Graded.')
      await openGrading(grading)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSavingGradeId(null)
    }
  }

  return (
    <AppShell
      title="Assignment management"
      subtitle="Create, edit and close assignments, and grade submissions"
      actions={
        <button type="button" className="btn-primary" onClick={openCreate}>
          ➕ Create assignment
        </button>
      }
    >
      {assignments.loading ? <Spinner /> : null}
      {assignments.error ? <ErrorState message={assignments.error.message} onRetry={assignments.reload} /> : null}

      {!assignments.loading && list.length === 0 ? (
        <EmptyState icon="📝" title="No assignments yet" description="Click 'Create assignment' to assign work to a class." />
      ) : null}

      {list.length > 0 ? (
        <div className="table-wrap">
          <table className="table min-w-[900px]">
            <thead>
              <tr>
                <th>Title</th>
                <th>Class</th>
                <th>Due date</th>
                <th>Allow late submission</th>
                <th>Resubmission</th>
                <th>Status</th>
                <th className="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {list.map((a) => (
                <tr key={a.id}>
                  <td className="font-semibold text-ink-900">{a.title}</td>
                  <td className="whitespace-nowrap">{a.classroomName}</td>
                  <td className="whitespace-nowrap">{formatDateTime(a.dueAt)}</td>
                  <td className="whitespace-nowrap">{a.allowLate ? `Yes (${a.latePenaltyPct}%)` : 'No'}</td>
                  <td className="whitespace-nowrap">
                    {a.allowResubmission ? `Yes (max ${a.maxAttempts})` : 'No'}
                  </td>
                  <td className="whitespace-nowrap">
                    <Badge tone={statusTone(a.status)}>{a.status}</Badge>
                  </td>
                  <td>
                    <div className="table-actions">
                      <Link className="btn-xs btn-ghost" to={`/teacher/assignments/${a.id}`}>
                        Details
                      </Link>
                      <button
                        type="button"
                        className="btn-xs btn-ghost"
                        onClick={() => openGrading(a)}
                        disabled={openingId === a.id}
                      >
                        {openingId === a.id ? <InlineSpinner /> : null} Grade
                      </button>
                      <button type="button" className="btn-xs btn-ghost" onClick={() => openEdit(a)}>
                        Edit
                      </button>
                      {a.status !== 'CLOSED' ? (
                        <button
                          type="button"
                          className="btn-xs btn-danger"
                          onClick={() => closeAssignment(a)}
                          disabled={closingId === a.id}
                        >
                          {closingId === a.id ? <InlineSpinner /> : null} Close
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
        title={editing ? 'Edit assignment' : 'Create new assignment'}
        size="lg"
      >
        <form onSubmit={save} className="space-y-4">
          <div>
            <label className="label" htmlFor="as-title">
              Title
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
              Class
            </label>
            <select
              id="as-class"
              className="input"
              required
              value={form.classroomId}
              onChange={(e) => setForm({ ...form, classroomId: e.target.value })}
            >
              <option value="">-- Select a class --</option>
              {classList.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.code})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="label" htmlFor="as-desc">
              Description
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
                Due date
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
                Max score
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
            <legend className="px-2 text-xs font-bold uppercase text-ink-600">Submission policy</legend>
            <div className="space-y-3">
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={form.allowLate}
                  onChange={(e) => setForm({ ...form, allowLate: e.target.checked })}
                />
                Allow late submission (UC-A2)
              </label>
              {form.allowLate ? (
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <div>
                    <label className="label" htmlFor="as-cutoff">
                      Late submission deadline
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
                      Late penalty (%)
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
                Allow updating / resubmitting (UC-A4)
              </label>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <div>
                  <label className="label" htmlFor="as-attempts">
                    Maximum attempts
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
                    Status
                  </label>
                  <select
                    id="as-status"
                    className="input"
                    value={form.status}
                    onChange={(e) => setForm({ ...form, status: e.target.value })}
                  >
                    <option value="DRAFT">Draft</option>
                    <option value="PUBLISHED">Open</option>
                    <option value="CLOSED">Closed</option>
                  </select>
                </div>
              </div>
            </div>
          </fieldset>

          <div className="flex justify-end gap-2">
            <button type="button" className="btn-ghost" onClick={() => setEditorOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? <InlineSpinner /> : '💾'} Save
            </button>
          </div>
        </form>
      </Modal>

      {/* Grading */}
      <Modal open={Boolean(grading)} onClose={() => setGrading(null)} title={`Grade — ${grading?.title || ''}`} size="lg">
        {loadingSubs ? <Spinner /> : null}
        {!loadingSubs && submissions.length === 0 ? (
          <EmptyState icon="📥" title="No submissions yet" description="Students have not submitted anything for this assignment." />
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
                        Attempt #{s.attemptNumber} · {s.originalName} · {formatDateTime(s.submittedAt)}
                      </p>
                    </div>
                    <Badge tone={s.late ? 'coral' : 'accent'}>{s.late ? 'Late' : 'On time'}</Badge>
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
                        className="input"
                        value={draft.score}
                        onChange={(e) => setGradeDraft({ ...gradeDraft, [s.id]: { ...draft, score: e.target.value } })}
                      />
                    </div>
                    <div className="min-w-[200px] flex-1">
                      <label className="label" htmlFor={`fb-${s.id}`}>
                        Feedback
                      </label>
                      <input
                        id={`fb-${s.id}`}
                        className="input"
                        value={draft.feedback}
                        onChange={(e) => setGradeDraft({ ...gradeDraft, [s.id]: { ...draft, feedback: e.target.value } })}
                      />
                    </div>
                    <button
                      type="button"
                      className="btn-accent"
                      onClick={() => submitGrade(s)}
                      disabled={savingGradeId === s.id}
                    >
                      {savingGradeId === s.id ? <InlineSpinner /> : null} Save score
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
