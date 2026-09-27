import { useLocation, useNavigate } from 'react-router-dom'

/**
 * Back button used by every detail view.
 * Prefers real browser history; falls back to a safe route when the page was opened
 * directly (no previous entry), so the user is never stranded.
 */
export function BackButton({ fallback = '/', label = 'Back' }) {
  const navigate = useNavigate()
  const location = useLocation()

  const goBack = () => {
    const hasHistory = location.key && location.key !== 'default'
    if (hasHistory) navigate(-1)
    else navigate(fallback, { replace: true })
  }

  return (
    <button type="button" className="btn-ghost !px-3" onClick={goBack}>
      <span aria-hidden="true">←</span>
      {label}
    </button>
  )
}
