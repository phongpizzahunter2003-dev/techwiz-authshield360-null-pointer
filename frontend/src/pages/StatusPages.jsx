import { Link } from 'react-router-dom'

export function ForbiddenPage() {
  return (
    <div className="grid min-h-screen place-items-center bg-surface-soft px-4">
      <div className="card max-w-md text-center">
        <p className="text-5xl" aria-hidden="true">
          🚫
        </p>
        <h1 className="mt-3 text-2xl font-extrabold text-ink-900">403 — Access denied</h1>
        <p className="mt-2 text-sm text-ink-400">
          You do not have permission to access this resource. This attempt has been logged.
        </p>
        <Link className="btn-primary mt-5" to="/">
          Back to home
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
        <h1 className="mt-3 text-2xl font-extrabold text-ink-900">404 — Page not found</h1>
        <p className="mt-2 text-sm text-ink-400">The path you accessed does not exist.</p>
        <Link className="btn-primary mt-5" to="/">
          Back to home
        </Link>
      </div>
    </div>
  )
}
