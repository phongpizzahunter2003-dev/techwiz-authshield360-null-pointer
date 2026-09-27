import { useState } from 'react'
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

  const list = classrooms.data?.data || []
  const studentList = students.data?.data || []

  const create = async (event) => {
    event.preventDefault()
    setSaving(true)
    try {
      await classroomApi.create(form)
      toast.success('Tạo lớp học thành công.')
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
      toast.success('Đã thêm học sinh vào lớp.')
      setEnrollTarget(null)
      setSelectedStudent('')
      await classrooms.reload()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setEnrolling(false)
    }
  }

  const remove = async (id) => {
    try {
      await classroomApi.remove(id)
      toast.success('Đã xóa lớp học.')
      await classrooms.reload()
    } catch (err) {
      toast.error(err.message)
    }
  }

  return (
    <AppShell
      title="Lớp học"
      subtitle="Tạo lớp và thêm học sinh vào lớp bạn phụ trách"
      actions={
        <button type="button" className="btn-primary" onClick={() => setCreateOpen(true)}>
          ➕ Tạo lớp học
        </button>
      }
    >
      {classrooms.loading ? <Spinner /> : null}
      {classrooms.error ? <ErrorState message={classrooms.error.message} onRetry={classrooms.reload} /> : null}

      {!classrooms.loading && list.length === 0 ? (
        <EmptyState icon="🏫" title="Chưa có lớp học" description="Tạo lớp học đầu tiên của bạn." />
      ) : null}

      {list.length > 0 ? (
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
          {list.map((c) => (
            <article key={c.id} className="card flex flex-col">
              <div className="flex items-start justify-between gap-2">
                <h3 className="text-sm font-bold text-ink-900">{c.name}</h3>
                <Badge tone="sky">{c.code}</Badge>
              </div>
              {c.description ? <p className="mt-1 text-xs text-ink-400">{c.description}</p> : null}
              <p className="mt-3 text-sm font-semibold text-ink-600">🎒 {c.studentCount} học sinh</p>
              <p className="mt-1 text-xs text-ink-400">Giáo viên: {c.teacherName || '—'}</p>
              <div className="mt-auto flex gap-2 pt-4">
                <button type="button" className="btn-accent flex-1" onClick={() => setEnrollTarget(c)}>
                  Thêm học sinh
                </button>
                <button type="button" className="btn-danger !px-3" onClick={() => remove(c.id)}>
                  Xóa
                </button>
              </div>
            </article>
          ))}
        </div>
      ) : null}

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Tạo lớp học">
        <form onSubmit={create} className="space-y-4">
          <div>
            <label className="label" htmlFor="cl-code">
              Mã lớp
            </label>
            <input
              id="cl-code"
              className="input"
              required
              maxLength={30}
              value={form.code}
              onChange={(e) => setForm({ ...form, code: e.target.value })}
              placeholder="vd: CS102"
            />
          </div>
          <div>
            <label className="label" htmlFor="cl-name">
              Tên lớp
            </label>
            <input
              id="cl-name"
              className="input"
              required
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="vd: Lập trình nâng cao"
            />
          </div>
          <div>
            <label className="label" htmlFor="cl-desc">
              Mô tả
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
              Hủy
            </button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? <InlineSpinner /> : '💾'} Lưu
            </button>
          </div>
        </form>
      </Modal>

      <Modal open={Boolean(enrollTarget)} onClose={() => setEnrollTarget(null)} title={`Thêm học sinh — ${enrollTarget?.name || ''}`}>
        <label className="label" htmlFor="enroll-student">
          Chọn học sinh
        </label>
        <select id="enroll-student" className="input" value={selectedStudent} onChange={(e) => setSelectedStudent(e.target.value)}>
          <option value="">-- Chọn học sinh --</option>
          {studentList.map((s) => (
            <option key={s.id} value={s.id}>
              {s.fullName || s.username} ({s.username})
            </option>
          ))}
        </select>
        <div className="mt-4 flex justify-end gap-2">
          <button type="button" className="btn-ghost" onClick={() => setEnrollTarget(null)}>
            Hủy
          </button>
          <button type="button" className="btn-accent" onClick={enroll} disabled={!selectedStudent || enrolling}>
            {enrolling ? <InlineSpinner /> : '➕'} Thêm vào lớp
          </button>
        </div>
      </Modal>
    </AppShell>
  )
}
