import { useState } from 'react'
import { Link } from 'react-router-dom'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { Badge } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { Modal } from '../../components/ui/Modal.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { classroomApi } from '../../api/endpoints.js'

export function TeacherClasses() {
  const toast = useToast()
  const classrooms = useAsync(() => classroomApi.list(), [])
  const students = useAsync(() => classroomApi.students(), [])

  const [createOpen, setCreateOpen] = useState(false)
  const [form, setForm] = useState({ code: '', name: '', description: '' })
  const [saving, setSaving] = useState(false)

  const [enrollTarget, setEnrollTarget] = useState(null)
  const [selectedStudent, setSelectedStudent] = useState('')
  const [enrolling, setEnrolling] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(null)
  const [deleting, setDeleting] = useState(false)

  const list = classrooms.data?.data || []
  const studentList = students.data?.data || []

  const create = async (event) => {
    event.preventDefault()
    setSaving(true)
    try {
      await classroomApi.create(form)
      toast.success('Class created successfully.')
      setCreateOpen(false)
      setForm({ code: '', name: '', description: '' })
      await classrooms.reload()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  const enroll = async () => {
    if (!selectedStudent) return
    setEnrolling(true)
    try {
      await classroomApi.enroll(enrollTarget.id, Number(selectedStudent))
      toast.success('Student added to the class.')
      setEnrollTarget(null)
      setSelectedStudent('')
      await classrooms.reload()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setEnrolling(false)
    }
  }

  const remove = async (classroom) => {
    setDeleting(true)
    try {
      await classroomApi.remove(classroom.id)
      toast.success('Class deleted.')
      setConfirmDelete(null)
      await classrooms.reload()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <AppShell
      title="Classes"
      subtitle="Create classes and add students to the classes you teach"
      actions={
        <button type="button" className="btn-primary" onClick={() => setCreateOpen(true)}>
          ➕ Create class
        </button>
      }
    >
      {classrooms.loading ? <Spinner /> : null}
      {classrooms.error ? <ErrorState message={classrooms.error.message} onRetry={classrooms.reload} /> : null}

      {!classrooms.loading && list.length === 0 ? (
        <EmptyState icon="🏫" title="No classes yet" description="Create your first class." />
      ) : null}

      {list.length > 0 ? (
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
          {list.map((c) => (
            <article key={c.id} className="card flex flex-col">
              <div className="flex items-start justify-between gap-2">
                <h3 className="text-sm font-bold text-ink-900">
                  <Link to={`/teacher/classes/${c.id}`} className="hover:underline">
                    {c.name}
                  </Link>
                </h3>
                <Badge tone="sky">{c.code}</Badge>
              </div>
              {c.description ? <p className="mt-1 text-xs text-ink-400">{c.description}</p> : null}
              <p className="mt-3 text-sm font-semibold text-ink-600">🎒 {c.studentCount} students</p>
              <p className="mt-1 text-xs text-ink-400">Teacher: {c.teacherName || '—'}</p>
              <div className="mt-auto flex flex-wrap gap-2 pt-4">
                <Link className="btn-ghost flex-1 whitespace-nowrap" to={`/teacher/classes/${c.id}`}>
                  Details
                </Link>
                <button type="button" className="btn-accent flex-1 whitespace-nowrap" onClick={() => setEnrollTarget(c)}>
                  Add student
                </button>
                <button type="button" className="btn-danger !px-3" onClick={() => setConfirmDelete(c)}>
                  Delete
                </button>
              </div>
            </article>
          ))}
        </div>
      ) : null}

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Create class">
        <form onSubmit={create} className="space-y-4">
          <div>
            <label className="label" htmlFor="cl-code">
              Class code
            </label>
            <input
              id="cl-code"
              className="input"
              required
              maxLength={30}
              value={form.code}
              onChange={(e) => setForm({ ...form, code: e.target.value })}
              placeholder="e.g. CS102"
            />
          </div>
          <div>
            <label className="label" htmlFor="cl-name">
              Class name
            </label>
            <input
              id="cl-name"
              className="input"
              required
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="e.g. Advanced Programming"
            />
          </div>
          <div>
            <label className="label" htmlFor="cl-desc">
              Description
            </label>
            <textarea
              id="cl-desc"
              className="input min-h-[80px]"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>
          <div className="flex justify-end gap-2">
            <button type="button" className="btn-ghost" onClick={() => setCreateOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? <InlineSpinner /> : '💾'} Save
            </button>
          </div>
        </form>
      </Modal>

      <Modal open={Boolean(enrollTarget)} onClose={() => setEnrollTarget(null)} title={`Add student — ${enrollTarget?.name || ''}`}>
        <label className="label" htmlFor="enroll-student">
          Select a student
        </label>
        <select id="enroll-student" className="input" value={selectedStudent} onChange={(e) => setSelectedStudent(e.target.value)}>
          <option value="">-- Select a student --</option>
          {studentList.map((s) => (
            <option key={s.id} value={s.id}>
              {s.fullName || s.username} ({s.username})
            </option>
          ))}
        </select>
        <div className="mt-4 flex justify-end gap-2">
          <button type="button" className="btn-ghost" onClick={() => setEnrollTarget(null)}>
            Cancel
          </button>
          <button type="button" className="btn-accent" onClick={enroll} disabled={!selectedStudent || enrolling}>
            {enrolling ? <InlineSpinner /> : '➕'} Add to class
          </button>
        </div>
      </Modal>

      {/* Destructive action guard */}
      <Modal
        open={Boolean(confirmDelete)}
        onClose={() => setConfirmDelete(null)}
        title="Confirm class deletion"
        size="sm"
        footer={
          <>
            <button type="button" className="btn-ghost" onClick={() => setConfirmDelete(null)}>
              Cancel
            </button>
            <button type="button" className="btn-danger" onClick={() => remove(confirmDelete)} disabled={deleting}>
              {deleting ? <InlineSpinner /> : '🗑'} Delete class
            </button>
          </>
        }
      >
        <p className="text-sm text-ink-600">
          You are about to delete the class <strong className="text-ink-900">{confirmDelete?.name}</strong> ({confirmDelete?.code}).
        </p>
        <p className="mt-2 rounded-xl bg-coral-100/50 px-3 py-2 text-xs text-ink-600">
          All student enrollments in this class will be removed. Assignments and the audit log are kept.
        </p>
      </Modal>
    </AppShell>
  )
}
