import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { Badge, statusTone } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { Modal } from '../../components/ui/Modal.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { adminApi } from '../../api/endpoints.js'
import { ROLE_LABEL } from '../../i18n/messages.js'
import { formatDateTime } from '../../utils/format.js'

const EMPTY_FORM = { username: '', email: '', phone: '', fullName: '', password: '', role: 'STUDENT', authMode: 'INHERIT' }

const AUTH_MODE_LABEL = {
  S1: 'S1 · Chỉ mật khẩu',
  S2: 'S2 · Mật khẩu + Mobile OTP',
  S3: 'S3 · Mật khẩu + Mobile + Email OTP',
  INHERIT: 'Theo cấu hình chung',
}

export function AdminUsers() {
  const toast = useToast()
  const [searchParams] = useSearchParams()
  const initialRole = searchParams.get('role') || ''
  const [filters, setFilters] = useState({ q: '', role: initialRole, page: 0 })
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [editorOpen, setEditorOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  const [bulkOpen, setBulkOpen] = useState(false)
  const [bulkForm, setBulkForm] = useState({ mode: 'S2', role: '' })
  const [bulkSaving, setBulkSaving] = useState(false)

  const load = async (next = filters) => {
    setLoading(true)
    setError(null)
    try {
      const res = await adminApi.users({ q: next.q || undefined, role: next.role || undefined, page: next.page, size: 20 })
      setData(res.data)
      setFilters(next)
    } catch (err) {
      setError(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load({ q: '', role: initialRole, page: 0 })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialRole])

  const openCreate = () => {
    setEditing(null)
    setForm(EMPTY_FORM)
    setEditorOpen(true)
  }

  const openEdit = (u) => {
    setEditing(u)
    setForm({
      username: u.username,
      email: u.email,
      phone: u.phone || '',
      fullName: u.fullName || '',
      password: '',
      role: u.role,
      status: u.status,
      authMode: u.authModeOverride || 'INHERIT',
    })
    setEditorOpen(true)
  }

  const save = async (event) => {
    event.preventDefault()
    setSaving(true)
    try {
      if (editing) {
        const payload = {
          email: form.email,
          phone: form.phone,
          fullName: form.fullName,
          role: form.role,
          status: form.status,
          authMode: form.authMode,
        }
        if (form.password) payload.password = form.password
        await adminApi.updateUser(editing.id, payload)
        toast.success('Cập nhật tài khoản thành công.')
      } else {
        await adminApi.createUser({ ...form, authMode: form.authMode })
        toast.success('Tạo tài khoản thành công.')
      }
      setEditorOpen(false)
      await load()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  const remove = async (u) => {
    try {
      await adminApi.deleteUser(u.id)
      toast.success('Đã xóa tài khoản.')
      await load()
    } catch (err) {
      toast.error(err.message)
    }
  }

  const resetMfa = async (u) => {
    try {
      await adminApi.resetMfa(u.id)
      toast.success(`Đã đặt lại MFA cho ${u.username}.`)
      await load()
    } catch (err) {
      toast.error(err.message)
    }
  }

  const applyBulk = async (event) => {
    event.preventDefault()
    setBulkSaving(true)
    try {
      const res = await adminApi.applyAuthMode({ mode: bulkForm.mode, role: bulkForm.role || null })
      toast.success(res.message)
      setBulkOpen(false)
      await load()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setBulkSaving(false)
    }
  }

  const items = data?.items || []
  const totalPages = data?.totalPages || 0

  return (
    <AppShell
      title="Người dùng & vai trò"
      subtitle="Quản lý tài khoản, gán vai trò và đặt lại MFA"
      actions={
        <>
          <button type="button" className="btn-accent" onClick={() => setBulkOpen(true)}>
            🛡️ Áp dụng S1/S2/S3 hàng loạt
          </button>
          <button type="button" className="btn-primary" onClick={openCreate}>
            ➕ Thêm người dùng
          </button>
        </>
      }
    >
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div className="min-w-[220px] flex-1">
          <label className="label" htmlFor="user-search">
            Tìm kiếm
          </label>
          <input
            id="user-search"
            className="input"
            placeholder="Tên đăng nhập, email, họ tên..."
            value={filters.q}
            onChange={(e) => setFilters({ ...filters, q: e.target.value })}
            onKeyDown={(e) => {
              if (e.key === 'Enter') load({ ...filters, page: 0 })
            }}
          />
        </div>
        <div>
          <label className="label" htmlFor="user-role">
            Vai trò
          </label>
          <select
            id="user-role"
            className="input"
            value={filters.role}
            onChange={(e) => load({ ...filters, role: e.target.value, page: 0 })}
          >
            <option value="">Tất cả</option>
            <option value="STUDENT">Học sinh</option>
            <option value="TEACHER">Giáo viên</option>
            <option value="ADMIN">Quản trị viên</option>
          </select>
        </div>
        <button type="button" className="btn-ghost" onClick={() => load({ ...filters, page: 0 })}>
          🔍 Lọc
        </button>
      </div>

      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={() => load()} /> : null}

      {!loading && !error && items.length === 0 ? (
        <EmptyState icon="👥" title="Không tìm thấy người dùng" description="Thử thay đổi bộ lọc hoặc tạo tài khoản mới." />
      ) : null}

      {!loading && items.length > 0 ? (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Tên đăng nhập</th>
                  <th>Email</th>
                  <th>Họ tên</th>
                  <th>Vai trò</th>
                  <th>Trạng thái</th>
                  <th>MFA</th>
                  <th>Chế độ xác thực</th>
                  <th>Sai liên tiếp</th>
                  <th className="text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {items.map((u) => (
                  <tr key={u.id} className="transition hover:bg-surface-soft">
                    <td className="font-semibold text-ink-900">
                      <Link to={`/admin/users/${u.id}`} className="hover:underline">
                        {u.username}
                      </Link>
                    </td>
                    <td>{u.email}</td>
                    <td>{u.fullName || '—'}</td>
                    <td>
                      <Badge tone="brand">{ROLE_LABEL[u.role]}</Badge>
                    </td>
                    <td>
                      <Badge tone={statusTone(u.status)}>{u.status}</Badge>
                    </td>
                    <td>{u.mfaEnrolled ? '✅ Đã đăng ký' : u.mfaEnabled ? '⏳ Chưa đăng ký' : '—'}</td>
                    <td>
                      {u.authModeOverride ? (
                        <Badge tone={u.authModeOverride === 'S1' ? 'coral' : u.authModeOverride === 'S2' ? 'sun' : 'accent'}>
                          {u.authModeOverride}
                        </Badge>
                      ) : (
                        <span className="text-xs text-ink-400">Theo cấu hình</span>
                      )}
                    </td>
                    <td>
                      {u.failedAttempts}
                      {u.lockedUntil ? (
                        <span className="ml-1 text-xs text-coral-600">(khóa đến {formatDateTime(u.lockedUntil)})</span>
                      ) : null}
                    </td>
                    <td>
                      <div className="flex flex-wrap justify-end gap-2">
                        <Link className="btn-ghost !px-3 !py-1.5" to={`/admin/users/${u.id}`}>
                          Chi tiết
                        </Link>
                        <button type="button" className="btn-ghost !px-3 !py-1.5" onClick={() => openEdit(u)}>
                          Sửa
                        </button>
                        <button type="button" className="btn-ghost !px-3 !py-1.5" onClick={() => resetMfa(u)}>
                          Reset MFA
                        </button>
                        <button type="button" className="btn-danger !px-3 !py-1.5" onClick={() => remove(u)}>
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-4 flex items-center justify-between text-sm text-ink-400">
            <span>
              Tổng {data.totalElements} bản ghi · Trang {data.page + 1}/{Math.max(totalPages, 1)}
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                className="btn-ghost !px-3 !py-1.5"
                disabled={data.page <= 0}
                onClick={() => load({ ...filters, page: data.page - 1 })}
              >
                ← Trước
              </button>
              <button
                type="button"
                className="btn-ghost !px-3 !py-1.5"
                disabled={data.page + 1 >= totalPages}
                onClick={() => load({ ...filters, page: data.page + 1 })}
              >
                Sau →
              </button>
            </div>
          </div>
        </>
      ) : null}

      <Modal open={editorOpen} onClose={() => setEditorOpen(false)} title={editing ? 'Sửa tài khoản' : 'Thêm người dùng'}>
        <form onSubmit={save} className="space-y-4">
          {!editing ? (
            <div>
              <label className="label" htmlFor="u-username">
                Tên đăng nhập
              </label>
              <input
                id="u-username"
                className="input"
                required
                maxLength={50}
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
              />
            </div>
          ) : null}
          <div>
            <label className="label" htmlFor="u-email">
              Email
            </label>
            <input
              id="u-email"
              type="email"
              className="input"
              required
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="label" htmlFor="u-fullname">
                Họ tên
              </label>
              <input
                id="u-fullname"
                className="input"
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              />
            </div>
            <div>
              <label className="label" htmlFor="u-phone">
                Số điện thoại
              </label>
              <input
                id="u-phone"
                className="input"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
              />
            </div>
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="label" htmlFor="u-role">
                Vai trò
              </label>
              <select
                id="u-role"
                className="input"
                value={form.role}
                onChange={(e) => setForm({ ...form, role: e.target.value })}
              >
                <option value="STUDENT">Học sinh</option>
                <option value="TEACHER">Giáo viên</option>
                <option value="ADMIN">Quản trị viên</option>
              </select>
            </div>
            {editing ? (
              <div>
                <label className="label" htmlFor="u-status">
                  Trạng thái
                </label>
                <select
                  id="u-status"
                  className="input"
                  value={form.status}
                  onChange={(e) => setForm({ ...form, status: e.target.value })}
                >
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="LOCKED">LOCKED</option>
                  <option value="DISABLED">DISABLED</option>
                </select>
              </div>
            ) : null}
          </div>
          <div>
            <label className="label" htmlFor="u-password">
              {editing ? 'Mật khẩu mới (để trống nếu không đổi)' : 'Mật khẩu khởi tạo'}
            </label>
            <input
              id="u-password"
              type="password"
              className="input"
              minLength={editing ? 0 : 8}
              required={!editing}
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
          </div>

          <div>
            <label className="label" htmlFor="u-authmode">
              Chế độ xác thực áp dụng cho tài khoản
            </label>
            <select
              id="u-authmode"
              className="input"
              value={form.authMode}
              onChange={(e) => setForm({ ...form, authMode: e.target.value })}
            >
              {Object.entries(AUTH_MODE_LABEL).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
            <p className="mt-1 text-xs text-ink-400">
              S1 không yêu cầu MFA · S2 yêu cầu Mobile OTP · S3 yêu cầu Mobile + Email OTP. Chọn "Theo cấu hình
              chung" để dùng chế độ ở trang Cấu hình xác thực.
            </p>
          </div>

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

      {/* Bulk security policy: apply S1/S2/S3 to all users or one role */}
      <Modal
        open={bulkOpen}
        onClose={() => setBulkOpen(false)}
        title="Áp dụng chế độ xác thực hàng loạt"
      >
        <form onSubmit={applyBulk} className="space-y-4">
          <p className="rounded-xl bg-surface-soft px-3 py-2 text-xs text-ink-400">
            Chức năng dành cho quản trị viên: gán chế độ xác thực (S1/S2/S3) cho nhiều tài khoản cùng lúc.
            Việc <strong>đăng ký</strong> yếu tố MFA vẫn do từng người dùng tự thực hiện (UC-09) — quản trị viên
            không tạo QR thay họ.
          </p>

          <div>
            <label className="label" htmlFor="bulk-mode">
              Chế độ xác thực
            </label>
            <select
              id="bulk-mode"
              className="input"
              value={bulkForm.mode}
              onChange={(e) => setBulkForm({ ...bulkForm, mode: e.target.value })}
            >
              {Object.entries(AUTH_MODE_LABEL).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="label" htmlFor="bulk-role">
              Phạm vi áp dụng
            </label>
            <select
              id="bulk-role"
              className="input"
              value={bulkForm.role}
              onChange={(e) => setBulkForm({ ...bulkForm, role: e.target.value })}
            >
              <option value="">Tất cả người dùng</option>
              <option value="STUDENT">Chỉ Học sinh</option>
              <option value="TEACHER">Chỉ Giáo viên</option>
              <option value="ADMIN">Chỉ Quản trị viên</option>
            </select>
          </div>

          <div className="rounded-xl border border-sun-400 bg-sun-100/60 px-3 py-2 text-xs text-ink-600">
            {bulkForm.mode === 'S1'
              ? 'S1: tắt yêu cầu MFA cho các tài khoản trong phạm vi.'
              : bulkForm.mode === 'INHERIT'
                ? 'Các tài khoản sẽ dùng chế độ chung ở trang Cấu hình xác thực.'
                : `${bulkForm.mode}: bật yêu cầu MFA. Người dùng sẽ được nhắc đăng ký ở lần đăng nhập kế tiếp.`}
          </div>

          <div className="flex justify-end gap-2">
            <button type="button" className="btn-ghost" onClick={() => setBulkOpen(false)}>
              Hủy
            </button>
            <button type="submit" className="btn-accent" disabled={bulkSaving}>
              {bulkSaving ? <InlineSpinner /> : '🛡️'} Áp dụng
            </button>
          </div>
        </form>
      </Modal>
    </AppShell>
  )
}
