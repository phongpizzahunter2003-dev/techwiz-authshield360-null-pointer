import { useState } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import { ROLE_LABEL } from '../../i18n/messages.js'
import { MfaEnrollmentPrompt } from './MfaEnrollmentPrompt.jsx'

const NAV = {
  STUDENT: [
    { to: '/student', label: 'Tổng quan', icon: '🏠', end: true },
    { to: '/student/assignments', label: 'Bài tập của tôi', icon: '📚' },
    { to: '/student/results', label: 'Kết quả thi', icon: '📊' },
    { to: '/profile', label: 'Bảo mật & MFA', icon: '🔐' },
  ],
  TEACHER: [
    { to: '/teacher', label: 'Tổng quan', icon: '🏠', end: true },
    { to: '/teacher/assignments', label: 'Quản lý bài tập', icon: '📝' },
    { to: '/teacher/classes', label: 'Lớp học', icon: '🏫' },
    { to: '/profile', label: 'Bảo mật & MFA', icon: '🔐' },
  ],
  ADMIN: [
    { to: '/admin', label: 'Tổng quan', icon: '🏠', end: true },
    { to: '/admin/users', label: 'Người dùng & vai trò', icon: '👥' },
    { to: '/admin/config', label: 'Cấu hình xác thực', icon: '⚙️' },
    { to: '/admin/audit-logs', label: 'Nhật ký xác thực', icon: '📜' },
    { to: '/admin/comparison', label: 'So sánh S1/S2/S3', icon: '📈' },
    { to: '/admin/system', label: 'Hệ thống & dữ liệu', icon: '🗄️' },
    { to: '/profile', label: 'Bảo mật & MFA', icon: '🔐' },
  ],
}

export function AppShell({ children, title, subtitle, actions }) {
  const { session, role, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const items = NAV[role] || []
  // Show the UC-09 enrolment prompt on the role dashboard only, so its overlay can never
  // block clicks on list pages / forms deeper in the app.
  const isRoleHome = ['/student', '/teacher', '/admin'].includes(location.pathname)

  const navLinks = (
    <nav className="flex flex-col gap-1">
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end}
          onClick={() => setOpen(false)}
          className={({ isActive }) =>
            `flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition ${
              isActive ? 'bg-brand-500 text-white shadow-glow' : 'text-ink-600 hover:bg-brand-50'
            }`
          }
        >
          <span aria-hidden="true">{item.icon}</span>
          {item.label}
        </NavLink>
      ))}
    </nav>
  )

  const brand = (
    <div className="flex items-center gap-2.5">
      <span className="grid h-10 w-10 place-items-center rounded-2xl bg-brand-500 text-lg text-white shadow-glow">
        🛡️
      </span>
      <div className="leading-tight">
        <p className="text-sm font-extrabold text-ink-900">AuthShield 360</p>
        <p className="text-[11px] text-ink-400">School Portal</p>
      </div>
    </div>
  )

  return (
    <div className="min-h-screen bg-surface-soft">
      {/* Desktop sidebar */}
      <aside className="fixed inset-y-0 left-0 hidden w-64 flex-col gap-6 border-r border-brand-50 bg-white px-4 py-5 lg:flex">
        {brand}
        {navLinks}
        <div className="mt-auto rounded-2xl bg-surface-soft p-3 text-xs text-ink-400">
          <p className="font-semibold text-ink-600">{ROLE_LABEL[role]}</p>
          <p className="mt-0.5 truncate">{session?.fullName || session?.username}</p>
        </div>
      </aside>

      {/* Mobile drawer */}
      {open ? (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-ink-900/40" onClick={() => setOpen(false)} aria-hidden="true" />
          <aside className="animate-fade-in absolute inset-y-0 left-0 flex w-72 flex-col gap-6 bg-white px-4 py-5 shadow-soft">
            {brand}
            {navLinks}
          </aside>
        </div>
      ) : null}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-30 flex items-center gap-3 border-b border-brand-50 bg-white/90 px-4 py-3 backdrop-blur lg:px-6">
          <button
            type="button"
            className="btn-ghost !min-h-0 !px-2.5 !py-2 lg:hidden"
            aria-label="Mở menu"
            onClick={() => setOpen(true)}
          >
            ☰
          </button>
          <div className="min-w-0 flex-1">
            <h1 className="truncate text-lg font-extrabold text-ink-900">{title}</h1>
            {subtitle ? <p className="truncate text-xs text-ink-400">{subtitle}</p> : null}
          </div>
          <div className="hidden items-center gap-2 sm:flex">
            <span className="badge bg-brand-100 text-brand-700">{ROLE_LABEL[role]}</span>
          </div>
          <button type="button" className="btn-ghost !px-3" onClick={() => navigate('/profile')}>
            👤
            <span className="hidden sm:inline">Hồ sơ</span>
          </button>
          <button id="btn-logout" type="button" className="btn-danger !px-3" onClick={logout}>
            <span aria-hidden="true">↩</span>
            <span className="hidden sm:inline">Đăng xuất</span>
          </button>
        </header>

        <main className="mx-auto w-full max-w-7xl px-4 py-5 lg:px-6 lg:py-8">
          {actions ? <div className="mb-5 flex flex-wrap items-center justify-end gap-2">{actions}</div> : null}
          {children}
        </main>
      </div>

      {/* UC-09: prompt the user to enrol their own second factor after first login. */}
      {isRoleHome ? <MfaEnrollmentPrompt /> : null}
    </div>
  )
}
