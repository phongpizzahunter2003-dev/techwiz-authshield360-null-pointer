import { Link, useParams } from 'react-router-dom'
import { DetailShell } from '../../components/layout/DetailShell.jsx'
import { Badge } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner } from '../../components/ui/Spinner.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { classroomApi, submissionApi } from '../../api/endpoints.js'
import { formatBytes, formatDate, formatDateTime } from '../../utils/format.js'

/** Teacher drill-down: one student's profile, submissions and exam results (UC-05). */
export function TeacherStudentDetail() {
  const { id } = useParams()
  const { data, loading, error, reload } = useAsync(() => classroomApi.studentDetail(id), [id])
  const student = data?.data

  const results = student?.results || []
  const submissions = student?.submissions || []
  const avg = results.length
    ? (results.reduce((sum, r) => sum + (r.maxScore ? r.score / r.maxScore : 0), 0) / results.length) * 10
    : null

  return (
    <DetailShell
      title={student ? student.fullName || student.username : 'Chi tiết học sinh'}
      subtitle={student ? `${student.studentCode || student.username} · ${student.className || ''}` : undefined}
      fallback="/teacher/classes"
      actions={
        <Link className="btn-ghost" to="/teacher/assignments">
          📚 Quản lý bài tập
        </Link>
      }
    >
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {student ? (
        <div className="space-y-6">
          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Thông tin học sinh</h2>
            <dl className="mt-4 grid grid-cols-1 gap-x-8 gap-y-3 text-sm sm:grid-cols-2">
              {[
                ['Tên đăng nhập', student.username],
                ['Họ tên', student.fullName || '—'],
                ['Mã học sinh', student.studentCode || '—'],
                ['Lớp', student.className || '—'],
                ['Email', student.email || '—'],
                ['Số điện thoại', student.phone || '—'],
              ].map(([label, value]) => (
                <div key={label} className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                  <dt className="text-ink-400">{label}</dt>
                  <dd className="max-w-[60%] truncate text-right font-semibold text-ink-900" title={String(value)}>
                    {value}
                  </dd>
                </div>
              ))}
            </dl>
            {student.classrooms?.length ? (
              <div className="mt-3 flex flex-wrap items-center gap-2">
                <span className="text-xs text-ink-400">Lớp bạn phụ trách có học sinh này:</span>
                {student.classrooms.map((name) => (
                  <Badge key={name} tone="sky">
                    {name}
                  </Badge>
                ))}
                {avg !== null ? <Badge tone="accent">Điểm TB: {avg.toFixed(1)}/10</Badge> : null}
              </div>
            ) : null}
          </section>

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">
              Bài nộp trong lớp bạn phụ trách ({submissions.length})
            </h2>
            {submissions.length === 0 ? (
              <EmptyState
                icon="📥"
                title="Chưa có bài nộp"
                description="Học sinh chưa nộp bài nào thuộc lớp bạn phụ trách."
              />
            ) : (
              <div className="table-wrap mt-3">
                <table className="table min-w-[820px]">
                  <thead>
                    <tr>
                      <th>Bài tập</th>
                      <th>Lần</th>
                      <th>Thời điểm</th>
                      <th>Trạng thái</th>
                      <th>Tệp</th>
                      <th>Điểm</th>
                      <th>Nhận xét</th>
                      <th className="text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {submissions.map((s) => (
                      <tr key={s.id} className="transition hover:bg-surface-soft">
                        <td className="whitespace-nowrap font-semibold text-ink-900">{s.assignmentTitle}</td>
                        <td>#{s.attemptNumber}</td>
                        <td className="whitespace-nowrap">{formatDateTime(s.submittedAt)}</td>
                        <td className="whitespace-nowrap">
                          <Badge tone={s.late ? 'coral' : 'accent'}>{s.late ? 'Nộp muộn' : 'Đúng hạn'}</Badge>
                        </td>
                        <td className="max-w-[180px] truncate">{s.originalName}</td>
                        <td className="font-bold">{s.score ?? '—'}</td>
                        <td className="max-w-[200px] truncate text-xs text-ink-400">{s.feedback || '—'}</td>
                        <td>
                          <div className="table-actions">
                            <a
                              className="btn-xs btn-ghost"
                              href={submissionApi.fileUrl(s.id)}
                              target="_blank"
                              rel="noreferrer"
                            >
                              Tải tệp
                            </a>
                            <Link className="btn-xs btn-ghost" to={`/teacher/assignments/${s.assignmentId}`}>
                              Chấm điểm
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
            <h2 className="text-base font-bold text-ink-900">Kết quả thi ({results.length})</h2>
            {results.length === 0 ? (
              <EmptyState icon="📊" title="Chưa có kết quả" description="Chưa có kết quả thi nào cho học sinh này." />
            ) : (
              <div className="table-wrap mt-3">
                <table className="table min-w-[700px]">
                  <thead>
                    <tr>
                      <th>Môn</th>
                      <th>Bài thi</th>
                      <th>Ngày</th>
                      <th>Điểm</th>
                      <th>Xếp loại</th>
                    </tr>
                  </thead>
                  <tbody>
                    {results.map((r) => {
                      const ratio = r.maxScore ? r.score / r.maxScore : 0
                      return (
                        <tr key={r.id}>
                          <td className="whitespace-nowrap font-semibold text-ink-900">{r.subject}</td>
                          <td className="whitespace-nowrap">{r.examName}</td>
                          <td className="whitespace-nowrap">{formatDate(r.examDate)}</td>
                          <td className="whitespace-nowrap font-bold">
                            {r.score}/{r.maxScore}
                          </td>
                          <td className="whitespace-nowrap">
                            <Badge tone={ratio >= 0.8 ? 'accent' : ratio >= 0.5 ? 'sun' : 'coral'}>
                              {ratio >= 0.8 ? 'Tốt' : ratio >= 0.5 ? 'Đạt' : 'Cần cố gắng'}
                            </Badge>
                          </td>
                        </tr>
                      )
                    })}
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
