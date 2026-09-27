import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import { Spinner } from '../ui/Spinner.jsx'

/** Client-side guard. The server remains the real gate (BR-05). */
export function ProtectedRoute({ roles, children }) {
  const { isAuthenticated, ready, role } = useAuth()
  const location = useLocation()

  if (!ready) {
    return (
      <div className="grid min-h-screen place-items-center">
        <Spinner label="Checking your session..." />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (roles && roles.length > 0 && !roles.includes(role)) {
    return <Navigate to="/403" replace />
  }

  return children
}
