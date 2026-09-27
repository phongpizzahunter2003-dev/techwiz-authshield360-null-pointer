import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { Badge, statusTone } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { Modal } from '../../components/ui/Modal.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { adminApi, downloadAuditExport } from '../../api/endpoints.js'
import { M } from '../../i18n/messages.js'
import { formatDateTime, toLocalInputValue } from '../../utils/format.js'

const EXPORT_LIMIT = 10000

const ACTIONS = [
  'LOGIN_ATTEMPT', 'LOGIN_FAIL', 'LOGIN_SUCCESS', 'LOCKOUT_TRIGGERED', 'LOCKOUT_RELEASED',
  'OTP_SENT', 'OTP_VERIFY_SUCCESS', 'OTP_VERIFY_FAIL', 'OTP_EXPIRED', 'RESEND_OTP_REQUEST',
  'RESEND_OTP_SUCCESS', 'RESEND_OTP_LIMIT_EXCEEDED', 'RESEND_OTP_THROTTLED', 'EMAIL_OTP_SENT',
  'EMAIL_OTP_FAILED', 'EMAIL_OTP_EXPIRED', 'LOGOUT', 'SESSION_EXPIRED', 'SESSION_REPLAY_ATTEMPT',
  'PRIVILEGE_VIOLATION', 'USER_CREATE', 'USER_UPDATE', 'USER_DELETE', 'ROLE_ASSIGN', 'CONFIG_CHANGE',
  'LOG_VIEW', 'LOG_EXPORT', 'SUBMISSION_CREATE', 'SUBMISSION_UPDATE', 'SUBMISSION_BLOCKED',
  'SUBMISSION_GRADE', 'ASSIGNMENT_CREATE', 'ASSIGNMENT_UPDATE', 'ASSIGNMENT_CLOSE',
  'EXAM_RESULT_UPSERT', 'MFA_ENROLL_SUCCESS', 'MFA_ENROLL_FAIL', 'MFA_RESET',
]

const EMPTY_FILTERS = { q: '', action: '', status: '', role: '', mode: '', from: '', to: '' }

export function AdminAuditLogs() {
  const toast = useToast()
  const [searchParams] = useSearchParams()
  // Support drill-down from dashboard charts / stat cards via query params.
  const [filters, setFilters] = useState({
    q: searchParams.get('q') || '',
    action: searchParams.get('action') || '',
    status: searchParams.get('status') || '',
    role: searchParams.get('role') || '',
    mode: searchParams.get('mode') || '',
    from: searchParams.get('from') ? toLocalInputValue(searchParams.get('from')) : '',
    to: searchParams.get('to') ? toLocalInputValue(searchParams.get('to')) : '',
  })
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [exporting, setExporting] = useState(false)
  const [limitDialog, setLimitDialog] = useState(null) // { total, format }

  const params = (extra = {}) => ({
    q: filters.q || undefined,
    action: filters.action || undefined,
    status: filters.status || undefined,
    role: filters.role || undefined,
    mode: filters.mode || undefined,
    from: filters.from ? new Date(filters.from).toISOString() : undefined,
    to: filters.to ? new Date(filters.to).toISOString() : undefined,
    ...extra,
  })

  const load = async (nextPage = page, nextFilters = filters) => {
    setLoading(true)
    setError(null)
    try {
      const res = await adminApi.auditLogs({
        q: nextFilters.q || undefined,
        action: nextFilters.action || undefined,
        status: nextFilters.status || undefined,
        role: nextFilters.role || undefined,
        mode: nextFilters.mode || undefined,
        from: nextFilters.from ? new Date(nextFilters.from).toISOString() : undefined,
        to: nextFilters.to ? new Date(nextFilters.to).toISOString() : undefined,
        page: nextPage,
        size: 50,
      })
      setData(res.data)
      setPage(nextPage)
      setFilters(nextFilters)
    } catch (err) {
      setError(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load(0, {
      q: searchParams.get('q') || '',
      action: searchParams.get('action') || '',
      status: searchParams.get('status') || '',
      role: searchParams.get('role') || '',
      mode: searchParams.get('mode') || '',
      from: searchParams.get('from') ? toLocalInputValue(searchParams.get('from')) : '',
      to: searchParams.get('to') ? toLocalInputValue(searchParams.get('to')) : '',
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams])

  const runExport = async (format) => {
    setExporting(true)
    try {
      // Check the total matching count first to enforce the 10,000-row cap (UC-11 §11.3).
      const probe = await adminApi.auditLogs({ ...params(), page: 0, size: 1 })
      const total = probe.data.totalElements
      if (total > EXPORT_LIMIT) {
        setLimitDialog({ total, format })
        return
      }
      const result = await downloadAuditExport({ ...params(), format })
      toast.success(`Đã xuất ${result.total} bản ghi (${result.filename}).`)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setExporting(false)
    }
  }

  const confirmLimitedExport = async () => {
    const { format } = limitDialog
    setLimitDialog(null)
    setExporting(true)
    try {
      const result = await downloadAuditExport({ ...params(), format })
      toast.warning(`Dữ liệu vượt 10,000 bản ghi — đã xuất 10,000 bản ghi mới nhất (${result.filename}).`)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setExporting(false)
    }
  }

  const items = data?.items || []
  const totalPages = data?.totalPages || 0

  return (
    <AppShell
      title="Nhật ký xác thực"
      subtitle="Theo dõi mọi sự kiện xác thực và phân quyền (UC-11)"
      actions={
        <>
          <button type="button" className="btn-ghost" onClick={() => runExport('csv')} disabled={exporting}>
            {exporting ? <InlineSpinner /> : '📄'} Xuất CSV
          </button>
          <button type="button" className="btn-ghost" onClick={() => runExport('json')} disabled={exporting}>
            {exporting ? <InlineSpinner /> : '🧾'} Xuất JSON
          </button>
        </>
      }
    >
      <div className="card mb-4">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <div className="lg:col-span-2">
            <label className="label" htmlFor="log-q">
              Tìm theo người dùng / IP
            </label>
            <input
              id="log-q"
              className="input"
              placeholder="vd: student01 hoặc 127.0.0.1"
              value={filters.q}
              onChange={(e) => setFilters({ ...filters, q: e.target.value })}
              onKeyDown={(e) => e.key === 'Enter' && load(0, filters)}
            />
          </div>
          <div>
            <label className="label" htmlFor="log-action">
              Hành động
            </label>
            <select
              id="log-action"
              className="input"
              value={filters.action}
              onChange={(e) => setFilters({ ...filters, action: e.target.value })}
            >
              <option value="">Tất cả</option>
              {ACTIONS.map((a) => (
                <option key={a} value={a}>
                  {a}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="label" htmlFor="log-status">
              Kết quả
            </label>
            <select
              id="log-status"
              className="input"
              value={filters.status}
              onChange={(e) => setFilters({ ...filters, status: e.target.value })}
            >
              <option value="">Tất cả</option>
              <option value="SUCCESS">SUCCESS</option>
              <option value="FAILURE">FAILURE</option>
            </select>
          </div>
          <div>
            <label className="label" htmlFor="log-role">
              Vai trò
            </label>
            <select
              id="log-role"
              className="input"
              value={filters.role}
              onChange={(e) => setFilters({ ...filters, role: e.target.value })}
            >
              <option value="">Tất cả</option>
              <option value="STUDENT">STUDENT</option>
              <option value="TEACHER">TEACHER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>
          <div>
            <label className="label" htmlFor="log-mode">
              Chế độ
            </label>
            <select
              id="log-mode"
              className="input"
              value={filters.mode}
              onChange={(e) => setFilters({ ...filters, mode: e.target.value })}
            >
              <option value="">Tất cả</option>
              <option value="S1">S1</option>
              <option value="S2">S2</option>
              <option value="S3">S3</option>
            </select>
          </div>
          <div>
            <label className="label" htmlFor="log-from">
              Từ ngày
            </label>
            <input
              id="log-from"
              type="datetime-local"
              className="input"
              value={filters.from}
              onChange={(e) => setFilters({ ...filters, from: e.target.value })}
            />
          </div>
          <div>
            <label className="label" htmlFor="log-to">
              Đến ngày
            </label>
            <input
              id="log-to"
              type="datetime-local"
              className="input"
              value={filters.to}
              onChange={(e) => setFilters({ ...filters, to: e.target.value })}
            />
          </div>
        </div>
        <div className="mt-3 flex flex-wrap justify-end gap-2">
          <button type="button" className="btn-ghost" onClick={() => load(0, EMPTY_FILTERS)}>
            Xóa lọc
          </button>
          <button type="button" className="btn-primary" onClick={() => load(0, filters)}>
            🔍 Lọc
          </button>
        </div>
      </div>

      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={() => load()} /> : null}

      {!loading && !error && items.length === 0 ? (
        <EmptyState icon="🔎" title="Không tìm thấy bản ghi log nào phù hợp" />
      ) : null}

      {!loading && items.length > 0 ? (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Thời điểm</th>
                  <th>Hành động</th>
                  <th>Kết quả</th>
                  <th>Người dùng</th>
                  <th>Vai trò</th>
                  <th>Yếu tố</th>
                  <th>Chế độ</th>
                  <th>IP</th>
                  <th>Lý do</th>
                  <th className="text-right">Chi tiết</th>
                </tr>
              </thead>
              <tbody>
                {items.map((row) => (
                  <tr key={row.id} className="transition hover:bg-surface-soft">
                    <td className="whitespace-nowrap">{formatDateTime(row.eventTime)}</td>
                    <td className="font-semibold text-ink-900">{row.eventAction}</td>
                    <td>
                      <Badge tone={statusTone(row.status)}>{row.status}</Badge>
                    </td>
                    <td>{row.userIdentifier || '—'}</td>
                    <td>{row.role || '—'}</td>
                    <td>{row.authFactor || '—'}</td>
                    <td>{row.authMode || '—'}</td>
                    <td>{row.clientIp || '—'}</td>
                    <td className="max-w-[180px] truncate text-xs text-ink-400">{row.failureReason || '—'}</td>
                    <td className="text-right">
                      <Link
                        to={`/admin/audit-logs/${row.id}`}
                        className="text-sm font-semibold text-brand-600 hover:underline"
                      >
                        Xem →
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-ink-400">
            <span>
              Tổng {data.totalElements} bản ghi · Trang {data.page + 1}/{Math.max(totalPages, 1)} · Tối đa 50 dòng/trang
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                className="btn-ghost !px-3 !py-1.5"
                disabled={data.page <= 0}
                onClick={() => load(data.page - 1, filters)}
              >
                ← Trước
              </button>
              <button
                type="button"
                className="btn-ghost !px-3 !py-1.5"
                disabled={data.page + 1 >= totalPages}
                onClick={() => load(data.page + 1, filters)}
              >
                Sau →
              </button>
            </div>
          </div>
        </>
      ) : null}

      <Modal
        open={Boolean(limitDialog)}
        onClose={() => setLimitDialog(null)}
        title="Cảnh báo giới hạn xuất dữ liệu"
        footer={
          <>
            <button type="button" className="btn-ghost" onClick={() => setLimitDialog(null)}>
              Hủy
            </button>
            <button id="btn-confirm-export-limit" type="button" className="btn-primary" onClick={confirmLimitedExport}>
              Xuất 10,000 bản ghi mới nhất
            </button>
          </>
        }
      >
        <p className="text-sm text-ink-600">
          Dữ liệu tìm kiếm vượt quá 10,000 bản ghi (Hiện có <strong>{limitDialog?.total?.toLocaleString('vi-VN')}</strong> bản
          ghi). Hệ thống sẽ tự động xuất 10,000 bản ghi mới nhất. Vui lòng thu hẹp khoảng thời gian hoặc điều kiện lọc để lấy
          đầy đủ dữ liệu.
        </p>
        <p className="mt-3 rounded-xl bg-sun-100 px-3 py-2 text-xs text-sun-600">
          Thông tin nhạy cảm (email, số điện thoại) sẽ được che giấu tự động: {M.EXPORT_LIMIT.split('.')[0]}.
        </p>
      </Modal>
    </AppShell>
  )
}
