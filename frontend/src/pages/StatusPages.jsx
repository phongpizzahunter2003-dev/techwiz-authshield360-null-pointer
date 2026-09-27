import { Link } from 'react-router-dom'

export function ForbiddenPage() {
  return (
    <div className="grid min-h-screen place-items-center bg-surface-soft px-4">
      <div className="card max-w-md text-center">
        <p className="text-5xl" aria-hidden="true">
          🚫
        </p>
        <h1 className="mt-3 text-2xl font-extrabold text-ink-900">403 — Không có quyền truy cập</h1>
        <p className="mt-2 text-sm text-ink-400">
          Bạn không có quyền truy cập vào tài nguyên này. Hành vi vi phạm đã được ghi nhận.
        </p>
        <Link className="btn-primary mt-5" to="/">
          Về trang chủ
        </Link>
      </div>
    </div>
  )
}

export function NotFoundPage() {
  return (
    <div className="grid min-h-screen place-items-center bg-surface-soft px-4">
      <div className="card max-w-md text-center">
        <p className="text-5xl" aria-hidden="true">
          🧭
        </p>
        <h1 className="mt-3 text-2xl font-extrabold text-ink-900">404 — Không tìm thấy trang</h1>
        <p className="mt-2 text-sm text-ink-400">Đường dẫn bạn truy cập không tồn tại.</p>
        <Link className="btn-primary mt-5" to="/">
          Về trang chủ
        </Link>
      </div>
    </div>
  )
}
