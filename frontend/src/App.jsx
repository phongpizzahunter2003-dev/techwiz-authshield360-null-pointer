import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import { ProtectedRoute } from './components/routing/ProtectedRoute.jsx'
import { ForbiddenPage, NotFoundPage } from './pages/StatusPages.jsx'
import { LoginPage } from './features/auth/LoginPage.jsx'
import { ProfilePage } from './features/profile/ProfilePage.jsx'
import { StudentDashboard } from './features/student/StudentDashboard.jsx'
import { StudentAssignments } from './features/student/StudentAssignments.jsx'
import { StudentAssignmentDetail } from './features/student/StudentAssignmentDetail.jsx'
import { StudentResults } from './features/student/StudentResults.jsx'
import { TeacherDashboard } from './features/teacher/TeacherDashboard.jsx'
import { TeacherAssignments } from './features/teacher/TeacherAssignments.jsx'
import { TeacherAssignmentDetail } from './features/teacher/TeacherAssignmentDetail.jsx'
import { TeacherClasses } from './features/teacher/TeacherClasses.jsx'
import { TeacherClassDetail } from './features/teacher/TeacherClassDetail.jsx'
import { TeacherStudentDetail } from './features/teacher/TeacherStudentDetail.jsx'
import { AdminDashboard } from './features/admin/AdminDashboard.jsx'
import { AdminUsers } from './features/admin/AdminUsers.jsx'
import { AdminUserDetail } from './features/admin/AdminUserDetail.jsx'
import { AdminConfig } from './features/admin/AdminConfig.jsx'
import { AdminAuditLogs } from './features/admin/AdminAuditLogs.jsx'
import { AdminAuditDetail } from './features/admin/AdminAuditDetail.jsx'
import { AdminComparison } from './features/admin/AdminComparison.jsx'
import { AdminAuthMode } from './features/admin/AdminAuthMode.jsx'
import { AdminSystem } from './features/admin/AdminSystem.jsx'
import { NotificationsPage } from './features/notifications/NotificationsPage.jsx'
import { Spinner } from './components/ui/Spinner.jsx'

function HomeRedirect() {
  const { isAuthenticated, ready, home } = useAuth()
  if (!ready) {
    return (
      <div className="grid min-h-screen place-items-center">
        <Spinner label="Loading..." />
      </div>
    )
  }
  return <Navigate to={isAuthenticated ? home : '/login'} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/403" element={<ForbiddenPage />} />

      <Route
        path="/student"
        element={
          <ProtectedRoute roles={['STUDENT']}>
            <StudentDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/student/assignments"
        element={
          <ProtectedRoute roles={['STUDENT']}>
            <StudentAssignments />
          </ProtectedRoute>
        }
      />
      <Route
        path="/student/results"
        element={
          <ProtectedRoute roles={['STUDENT']}>
            <StudentResults />
          </ProtectedRoute>
        }
      />
      <Route
        path="/student/assignments/:id"
        element={
          <ProtectedRoute roles={['STUDENT']}>
            <StudentAssignmentDetail />
          </ProtectedRoute>
        }
      />

      <Route
        path="/teacher"
        element={
          <ProtectedRoute roles={['TEACHER']}>
            <TeacherDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/teacher/assignments"
        element={
          <ProtectedRoute roles={['TEACHER']}>
            <TeacherAssignments />
          </ProtectedRoute>
        }
      />
      <Route
        path="/teacher/classes"
        element={
          <ProtectedRoute roles={['TEACHER']}>
            <TeacherClasses />
          </ProtectedRoute>
        }
      />
      <Route
        path="/teacher/assignments/:id"
        element={
          <ProtectedRoute roles={['TEACHER']}>
            <TeacherAssignmentDetail />
          </ProtectedRoute>
        }
      />
      <Route
        path="/teacher/classes/:id"
        element={
          <ProtectedRoute roles={['TEACHER']}>
            <TeacherClassDetail />
          </ProtectedRoute>
        }
      />
      <Route
        path="/teacher/students/:id"
        element={
          <ProtectedRoute roles={['TEACHER']}>
            <TeacherStudentDetail />
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <AdminDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/users"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <AdminUsers />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/config"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <AdminConfig />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/audit-logs"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <AdminAuditLogs />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/comparison"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <AdminComparison />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/users/:id"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <AdminUserDetail />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/audit-logs/:id"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <AdminAuditDetail />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/system"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <AdminSystem />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/modes/:mode"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <AdminAuthMode />
          </ProtectedRoute>
        }
      />

      <Route
        path="/profile"
        element={
          <ProtectedRoute roles={['STUDENT', 'TEACHER', 'ADMIN']}>
            <ProfilePage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/notifications"
        element={
          <ProtectedRoute roles={['STUDENT', 'TEACHER', 'ADMIN']}>
            <NotificationsPage />
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
