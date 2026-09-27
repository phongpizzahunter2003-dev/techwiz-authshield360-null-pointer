import { Link, useParams } from 'react-router-dom'
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
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-base font-bold text-ink-900">Danh sách học sinh</h2>
              <span className="text-xs text-ink-400">Bấm vào học sinh để xem chi tiết</span>
            </div>
            {roster.length === 0 ? (
              <EmptyState icon="🧑‍🎓" title="Lớp chưa có học sinh" description="Thêm học sinh từ trang Lớp học." />
            ) : (
              <div className="table-wrap mt-3">
                <table className="table min-w-[760px]">
                  <thead>
                    <tr>
                      <th>Tên đăng nhập</th>
                      <th>Họ tên</th>
                      <th>Email</th>
                      <th>Số điện thoại</th>
                      <th className="text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {roster.map((s) => (
                      <tr key={s.id} className="transition hover:bg-surface-soft">
                        <td className="whitespace-nowrap font-semibold text-ink-900">
                          <Link to={`/teacher/students/${s.id}`} className="hover:underline">
                            {s.username}
                          </Link>
                        </td>
                        <td className="whitespace-nowrap">{s.fullName || '—'}</td>
                        <td className="whitespace-nowrap">{s.email}</td>
                        <td className="whitespace-nowrap">{s.phone || '—'}</td>
                        <td>
                          <div className="table-actions">
                            <Link className="btn-xs btn-ghost" to={`/teacher/students/${s.id}`}>
                              Chi tiết
                            </Link>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="card">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-base font-bold text-ink-900">Kết quả thi của lớp</h2>
              <span className="text-xs text-ink-400">Bấm vào học sinh để xem chi tiết</span>
            </div>
            {scores.length === 0 ? (
              <EmptyState icon="📊" title="Chưa có kết quả" description="Chưa có kết quả thi nào cho lớp này." />
            ) : (
              <div className="table-wrap mt-3">
                <table className="table min-w-[760px]">
                  <thead>
                    <tr>
                      <th>Học sinh</th>
                      <th>Môn</th>
                      <th>Bài thi</th>
                      <th>Ngày</th>
                      <th>Điểm</th>
                      <th className="text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {scores.map((r) => (
                      <tr key={r.id} className="transition hover:bg-surface-soft">
                        <td className="whitespace-nowrap font-semibold text-ink-900">
                          <Link to={`/teacher/students/${r.studentId}`} className="hover:underline">
                            {r.studentName}
                          </Link>
                        </td>
                        <td className="whitespace-nowrap">{r.subject}</td>
                        <td className="whitespace-nowrap">{r.examName}</td>
                        <td className="whitespace-nowrap">{formatDate(r.examDate)}</td>
                        <td className="whitespace-nowrap font-bold">
                          {r.score}/{r.maxScore}
                        </td>
                        <td>
                          <div className="table-actions">
                            <Link className="btn-xs btn-ghost" to={`/teacher/students/${r.studentId}`}>
                              Chi tiết
                            </Link>
                          </div>
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
