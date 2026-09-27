import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { Badge, statusTone } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { Modal } from '../../components/ui/Modal.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { adminApi } from '../../api/endpoints.js'
import { AUTH_MODE_LABEL, ROLE_LABEL } from '../../i18n/messages.js'
import { formatDateTime } from '../../utils/format.js'

const EMPTY_FORM = { username: '', email: '', phone: '', fullName: '', password: '', role: 'STUDENT', authMode: 'INHERIT' }

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
  const [confirmDelete, setConfirmDelete] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const [busyId, setBusyId] = useState(null)

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
        toast.success('Account updated successfully.')
      } else {
        await adminApi.createUser({ ...form, authMode: form.authMode })
        toast.success('Account created successfully.')
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
    setDeleting(true)
    try {
      await adminApi.deleteUser(u.id)
      toast.success('Account deleted.')
      setConfirmDelete(null)
      await load()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setDeleting(false)
    }
  }

  const resetMfa = async (u) => {
    if (busyId) return
    setBusyId(u.id)
    try {
      await adminApi.resetMfa(u.id)
      toast.success(`MFA reset for ${u.username}.`)
      await load()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setBusyId(null)
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
      title="Users & roles"
      subtitle="Manage accounts, assign roles, and reset MFA"
      actions={
        <>
          <button type="button" className="btn-accent" onClick={() => setBulkOpen(true)}>
            🛡️ Apply S1/S2/S3 in bulk
          </button>
          <button type="button" className="btn-primary" onClick={openCreate}>
            ➕ Add user
          </button>
        </>
      }
    >
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div className="min-w-[220px] flex-1">
          <label className="label" htmlFor="user-search">
            Search
          </label>
          <input
            id="user-search"
            className="input"
            placeholder="Username, email, full name..."
            value={filters.q}
            onChange={(e) => setFilters({ ...filters, q: e.target.value })}
            onKeyDown={(e) => {
              if (e.key === 'Enter') load({ ...filters, page: 0 })
            }}
          />
        </div>
        <div>
          <label className="label" htmlFor="user-role">
            Role
          </label>
          <select
            id="user-role"
            className="input"
            value={filters.role}
            onChange={(e) => load({ ...filters, role: e.target.value, page: 0 })}
          >
            <option value="">All</option>
            <option value="STUDENT">Student</option>
            <option value="TEACHER">Teacher</option>
            <option value="ADMIN">Administrator</option>
          </select>
        </div>
        <button type="button" className="btn-ghost" onClick={() => load({ ...filters, page: 0 })}>
          🔍 Filter
        </button>
      </div>

      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={() => load()} /> : null}

      {!loading && !error && items.length === 0 ? (
        <EmptyState icon="👥" title="No users found" description="Try changing the filters or create a new account." />
      ) : null}

      {!loading && items.length > 0 ? (
        <>
          <div className="table-wrap">
            <table className="table min-w-[880px]">
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Full name</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Security</th>
                  <th>Failed attempts</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((u) => (
                  <tr key={u.id} className="transition hover:bg-surface-soft">
                    <td className="whitespace-nowrap font-semibold text-ink-900">
                      <Link to={`/admin/users/${u.id}`} className="hover:underline">
                        {u.username}
                      </Link>
                    </td>
                    <td className="whitespace-nowrap">{u.email}</td>
                    <td className="whitespace-nowrap">{u.fullName || '—'}</td>
                    <td className="whitespace-nowrap">
                      <Badge tone="brand">{ROLE_LABEL[u.role]}</Badge>
                    </td>
                    <td className="whitespace-nowrap">
                      <Badge tone={statusTone(u.status)}>{u.status}</Badge>
                    </td>
                    <td className="whitespace-nowrap">
                      <div className="flex items-center gap-1.5">
                        {u.authModeOverride ? (
                          <Badge
                            tone={u.authModeOverride === 'S1' ? 'coral' : u.authModeOverride === 'S2' ? 'sun' : 'accent'}
                          >
                            {u.authModeOverride}
                          </Badge>
                        ) : (
                          <span className="text-xs text-ink-400">Global config</span>
                        )}
                        <span className="text-xs text-ink-400">·</span>
                        <span className="text-xs text-ink-600">
                          {u.mfaEnrolled ? '✅ MFA enrolled' : u.mfaEnabled ? '⏳ Not enrolled' : '— MFA off'}
                        </span>
                      </div>
                    </td>
                    <td className="whitespace-nowrap">
                      {u.failedAttempts}
                      {u.lockedUntil ? (
                        <span className="ml-1 text-xs text-coral-600">(locked until {formatDateTime(u.lockedUntil)})</span>
                      ) : null}
                    </td>
                    <td>
                      <div className="table-actions">
                        <Link className="btn-xs btn-ghost" to={`/admin/users/${u.id}`}>
                          Details
                        </Link>
                        <button type="button" className="btn-xs btn-ghost" onClick={() => openEdit(u)}>
                          Edit
                        </button>
                        <button
                          type="button"
                          className="btn-xs btn-ghost"
                          onClick={() => resetMfa(u)}
                          disabled={busyId === u.id}
                        >
                          {busyId === u.id ? <InlineSpinner /> : null} Reset MFA
                        </button>
                        <button type="button" className="btn-xs btn-danger" onClick={() => setConfirmDelete(u)}>
                          Delete
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
              {data.totalElements} total · Page {data.page + 1}/{Math.max(totalPages, 1)}
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                className="btn-ghost !px-3 !py-1.5"
                disabled={data.page <= 0}
                onClick={() => load({ ...filters, page: data.page - 1 })}
              >
                ← Previous
              </button>
              <button
                type="button"
                className="btn-ghost !px-3 !py-1.5"
                disabled={data.page + 1 >= totalPages}
                onClick={() => load({ ...filters, page: data.page + 1 })}
              >
                Next →
              </button>
            </div>
          </div>
        </>
      ) : null}

      <Modal open={editorOpen} onClose={() => setEditorOpen(false)} title={editing ? 'Edit account' : 'Add user'}>
        <form onSubmit={save} className="space-y-4">
          {!editing ? (
            <div>
              <label className="label" htmlFor="u-username">
                Username
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
                Full name
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
                Phone number
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
                Role
              </label>
              <select
                id="u-role"
                className="input"
                value={form.role}
                onChange={(e) => setForm({ ...form, role: e.target.value })}
              >
                <option value="STUDENT">Student</option>
                <option value="TEACHER">Teacher</option>
                <option value="ADMIN">Administrator</option>
              </select>
            </div>
            {editing ? (
              <div>
                <label className="label" htmlFor="u-status">
                  Status
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
              {editing ? 'New password (leave blank to keep)' : 'Initial password'}
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
              Authentication mode for this account
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
              S1 does not require MFA · S2 requires Mobile OTP · S3 requires Mobile + Email OTP. Choose "Use global
              configuration" to use the mode on the Authentication configuration page.
            </p>
          </div>

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

      {/* Bulk security policy: apply S1/S2/S3 to all users or one role */}
      <Modal
        open={bulkOpen}
        onClose={() => setBulkOpen(false)}
        title="Apply authentication mode in bulk"
      >
        <form onSubmit={applyBulk} className="space-y-4">
          <p className="rounded-xl bg-surface-soft px-3 py-2 text-xs text-ink-400">
            Administrator feature: assign an authentication mode (S1/S2/S3) to multiple accounts at once.
            <strong>Enrolling</strong> MFA factors is still done by each user individually (UC-09) — administrators
            do not create QR codes on their behalf.
          </p>

          <div>
            <label className="label" htmlFor="bulk-mode">
              Authentication mode
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
              Scope
            </label>
            <select
              id="bulk-role"
              className="input"
              value={bulkForm.role}
              onChange={(e) => setBulkForm({ ...bulkForm, role: e.target.value })}
            >
              <option value="">All users</option>
              <option value="STUDENT">Students only</option>
              <option value="TEACHER">Teachers only</option>
              <option value="ADMIN">Administrators only</option>
            </select>
          </div>

          <div className="rounded-xl border border-sun-400 bg-sun-100/60 px-3 py-2 text-xs text-ink-600">
            {bulkForm.mode === 'S1'
              ? 'S1: disables the MFA requirement for accounts in scope.'
              : bulkForm.mode === 'INHERIT'
                ? 'Accounts will use the global mode on the Authentication configuration page.'
                : `${bulkForm.mode}: enables the MFA requirement. Users will be prompted to enroll at their next sign-in.`}
          </div>

          <div className="flex justify-end gap-2">
            <button type="button" className="btn-ghost" onClick={() => setBulkOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn-accent" disabled={bulkSaving}>
              {bulkSaving ? <InlineSpinner /> : '🛡️'} Apply
            </button>
          </div>
        </form>
      </Modal>

      {/* Destructive action guard: Delete deletes immediately, so confirm first */}
      <Modal
        open={Boolean(confirmDelete)}
        onClose={() => setConfirmDelete(null)}
        title="Confirm account deletion"
        size="sm"
        footer={
          <>
            <button type="button" className="btn-ghost" onClick={() => setConfirmDelete(null)}>
              Cancel
            </button>
            <button type="button" className="btn-danger" onClick={() => remove(confirmDelete)} disabled={deleting}>
              {deleting ? <InlineSpinner /> : '🗑'} Delete account
            </button>
          </>
        }
      >
        <p className="text-sm text-ink-600">
          You are about to permanently delete the account <strong className="text-ink-900">{confirmDelete?.username}</strong>
          {confirmDelete?.fullName ? ` (${confirmDelete.fullName})` : ''}.
        </p>
        <p className="mt-2 rounded-xl bg-coral-100/50 px-3 py-2 text-xs text-ink-600">
          This action cannot be undone. The account audit log is still retained per BR-07.
        </p>
      </Modal>
    </AppShell>
  )
}
