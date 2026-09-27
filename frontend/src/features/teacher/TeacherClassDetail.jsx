import { useParams } from 'react-router-dom'
import { DetailShell } from '../../components/layout/DetailShell.jsx'
import { Badge } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner } from '../../components/ui/Spinner.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { classroomApi } from '../../api/endpoints.js'
import { formatDate } from '../../utils/format.js'

/** Teacher drill-down: roster + exam results for one class. */
export function TeacherClassDetail() {
  const { id } = useParams()
  const classrooms = useAsync(() => classroomApi.list(), [])
  const students = useAsync(() => classroomApi.studentsInClass(id), [id])
  const results = useAsync(() => classroomApi.results(id), [id])

  const classroom = (classrooms.data?.data || []).find((c) => String(c.id) === String(id))
  const roster = students.data?.data || []
  const scores = results.data?.data || []

  return (
    <DetailShell
      title={classroom?.name || 'Chi tiết lớp học'}
      subtitle={classroom ? `${classroom.code} · ${roster.length} học sinh` : undefined}
      fallback="/teacher/classes"
    >
      {classrooms.loading || students.loading ? <Spinner /> : null}
      {classrooms.error ? <ErrorState message={classrooms.error.message} onRetry={classrooms.reload} /> : null}
      {students.error ? <ErrorState message={students.error.message} onRetry={students.reload} /> : null}

      {classroom ? (
        <div className="space-y-6">
          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Thông tin lớp</h2>
            <div className="mt-3 flex flex-wrap gap-2">
              <Badge tone="sky" icon="🏫">{classroom.code}</Badge>
              <Badge tone="accent" icon="🎒">{roster.length} học sinh</Badge>
              <Badge tone="neutral">Giáo viên: {classroom.teacherName || '—'}</Badge>
            </div>
            {classroom.description ? <p className="mt-3 text-sm text-ink-600">{classroom.description}</p> : null}
          </section>

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Danh sách học sinh</h2>
            {roster.length === 0 ? (
              <EmptyState icon="🧑‍🎓" title="Lớp chưa có học sinh" description="Thêm học sinh từ trang Lớp học." />
            ) : (
              <div className="table-wrap mt-3">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Tên đăng nhập</th>
                      <th>Họ tên</th>
                      <th>Email</th>
                      <th>Số điện thoại</th>
                    </tr>
                  </thead>
                  <tbody>
                    {roster.map((s) => (
                      <tr key={s.id}>
                        <td className="font-semibold text-ink-900">{s.username}</td>
                        <td>{s.fullName || '—'}</td>
                        <td>{s.email}</td>
                        <td>{s.phone || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Kết quả thi của lớp</h2>
            {scores.length === 0 ? (
              <EmptyState icon="📊" title="Chưa có kết quả" description="Chưa có kết quả thi nào cho lớp này." />
            ) : (
              <div className="table-wrap mt-3">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Học sinh</th>
                      <th>Môn</th>
                      <th>Bài thi</th>
                      <th>Ngày</th>
                      <th>Điểm</th>
                    </tr>
                  </thead>
                  <tbody>
                    {scores.map((r) => (
                      <tr key={r.id}>
                        <td className="font-semibold text-ink-900">{r.studentName}</td>
                        <td>{r.subject}</td>
                        <td>{r.examName}</td>
                        <td>{formatDate(r.examDate)}</td>
                        <td className="font-bold">
                          {r.score}/{r.maxScore}
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
