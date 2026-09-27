import { DetailShell } from '../../components/layout/DetailShell.jsx'
import { Badge } from '../../components/ui/Badge.jsx'
import { ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { adminApi } from '../../api/endpoints.js'
import { formatDateTime } from '../../utils/format.js'

/**
 * Admin diagnostics: proves which database the API is connected to and how many rows
 * each table actually holds (answers "did the data reach the database?").
 */
export function AdminSystem() {
  const { data, loading, error, reload } = useAsync(() => adminApi.dbStatus(), [])
  const status = data?.data

  return (
    <DetailShell
      title="Hệ thống & dữ liệu"
      subtitle="Kiểm tra kết nối cơ sở dữ liệu và số bản ghi thực tế trong từng bảng"
      fallback="/admin"
      actions={
        <button type="button" className="btn-ghost" onClick={reload} disabled={loading}>
          {loading ? <InlineSpinner /> : '🔄'} Làm mới
        </button>
      }
    >
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {status ? (
        <div className="space-y-6">
          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Kết nối cơ sở dữ liệu</h2>
            <div className="mt-3 flex flex-wrap gap-2">
              <Badge tone="brand">{status.databaseProduct}</Badge>
              <Badge tone="neutral">v{status.databaseVersion}</Badge>
              <Badge tone={status.persistent ? 'accent' : 'sun'}>
                {status.persistent ? 'Lưu trữ bền vững' : 'Trong bộ nhớ (mất khi khởi động lại)'}
              </Badge>
              <Badge tone="sky">Profile: {status.activeProfiles || 'default'}</Badge>
            </div>
            <dl className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                <dt className="text-ink-400">JDBC URL</dt>
                <dd className="max-w-[70%] truncate font-mono text-xs text-ink-900" title={status.jdbcUrl}>
                  {status.jdbcUrl}
                </dd>
              </div>
              <div className="flex justify-between gap-4 border-b border-brand-50 pb-2">
                <dt className="text-ink-400">Tổng số bản ghi</dt>
                <dd className="font-semibold text-ink-900">{status.totalRows}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-ink-400">Kiểm tra lúc</dt>
                <dd className="font-semibold text-ink-900">{formatDateTime(status.checkedAt)}</dd>
              </div>
            </dl>
            {!status.persistent ? (
              <p className="mt-3 rounded-xl border border-sun-400 bg-sun-100/60 px-3 py-2 text-xs text-ink-600">
                Đang chạy profile <strong>dev</strong> với H2 trong bộ nhớ: dữ liệu <strong>có</strong> được ghi vào
                CSDL nhưng sẽ mất khi khởi động lại. Muốn dữ liệu tồn tại lâu dài, hãy chạy với profile
                <strong> mysql</strong> (xem README.md §3).
              </p>
            ) : null}
          </section>

          <section className="card">
            <h2 className="text-base font-bold text-ink-900">Số bản ghi theo bảng</h2>
            <div className="table-wrap mt-3">
              <table className="table">
                <thead>
                  <tr>
                    <th>Bảng</th>
                    <th>Số bản ghi</th>
                    <th>Trạng thái</th>
                  </tr>
                </thead>
                <tbody>
                  {status.tables.map((t) => (
                    <tr key={t.table}>
                      <td className="font-mono text-xs text-ink-900">{t.table}</td>
                      <td className="font-bold">{t.rows < 0 ? '—' : t.rows}</td>
                      <td>
                        {t.rows < 0 ? (
                          <Badge tone="coral">Chưa có bảng</Badge>
                        ) : t.rows === 0 ? (
                          <Badge tone="neutral">Trống</Badge>
                        ) : (
                          <Badge tone="accent">Có dữ liệu</Badge>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="mt-2 text-xs text-ink-400">
              Số bản ghi được đọc trực tiếp bằng SQL trên kết nối đang hoạt động — đây là bằng chứng dữ liệu đã
              vào tới CSDL.
            </p>
          </section>
        </div>
      ) : null}
    </DetailShell>
  )
}
