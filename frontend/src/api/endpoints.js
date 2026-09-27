import { get, post, put, del, upload, http } from './client.js'

export const authApi = {
  login: (payload) => post('/auth/login', payload),
  verifyOtp: (payload) => post('/auth/otp/verify', payload),
  resendOtp: (payload) => post('/auth/resend-otp', payload),
  logout: () => post('/auth/logout', {}),
  session: () => get('/auth/session'),
  enrollMfa: () => post('/auth/mfa/enroll', {}),
  confirmMfa: (payload) => post('/auth/mfa/enroll/confirm', payload),
}

export const dashboardApi = {
  student: () => get('/dashboard/student'),
  teacher: () => get('/dashboard/teacher'),
  admin: () => get('/dashboard/admin'),
  comparison: () => get('/admin/comparison'),
}

export const notificationsApi = {
  list: (params) => get('/notifications', params),
  unreadCount: () => get('/notifications/unread-count'),
  markRead: (id) => post(`/notifications/${id}/read`, {}),
  markAllRead: () => post('/notifications/read-all', {}),
  remove: (id) => del(`/notifications/${id}`),
}

export const analyticsApi = {
  student: () => get('/analytics/student'),
  teacher: () => get('/analytics/teacher'),
  admin: () => get('/analytics/admin'),
  studentAssignments: (bucket) => get('/analytics/student/assignments', { bucket }),
}

export const assignmentApi = {
  list: () => get('/assignments'),
  detail: (id) => get(`/assignments/${id}`),
  create: (payload) => post('/teacher/assignments', payload),
  update: (id, payload) => put(`/teacher/assignments/${id}`, payload),
  close: (id) => post(`/teacher/assignments/${id}/close`, {}),
}

export const submissionApi = {
  submit: (assignmentId, file) => {
    const form = new FormData()
    form.append('file', file)
    return upload(`/student/assignments/${assignmentId}/submissions`, form)
  },
  history: (assignmentId) => get(`/student/assignments/${assignmentId}/submissions`),
  mine: () => get('/student/submissions'),
  forAssignment: (assignmentId) => get(`/teacher/assignments/${assignmentId}/submissions`),
  grade: (id, payload) => post(`/teacher/submissions/${id}/grade`, payload),
  fileUrl: (id) => `${import.meta.env.VITE_API_BASE_URL || '/api/v1'}/submissions/${id}/file`,
}

export const classroomApi = {
  list: () => get('/classrooms'),
  students: () => get('/teacher/students'),
  studentsInClass: (id) => get(`/teacher/classrooms/${id}/students`),
  studentDetail: (studentId) => get(`/teacher/students/${studentId}`),
  create: (payload) => post('/teacher/classrooms', payload),
  remove: (id) => del(`/teacher/classrooms/${id}`),
  enroll: (id, studentId) => post(`/teacher/classrooms/${id}/enroll`, { studentId }),
  unenroll: (id, studentId) => del(`/teacher/classrooms/${id}/students/${studentId}`),
  results: (id) => get(`/teacher/classrooms/${id}/results`),
}

export const resultApi = {
  mine: () => get('/student/results'),
  upsert: (payload) => post('/teacher/results', payload),
}

export const adminApi = {
  users: (params) => get('/admin/users', params),
  user: (id) => get(`/admin/users/${id}`),
  createUser: (payload) => post('/admin/users', payload),
  updateUser: (id, payload) => put(`/admin/users/${id}`, payload),
  deleteUser: (id) => del(`/admin/users/${id}`),
  resetMfa: (id) => post(`/admin/users/${id}/reset-mfa`, {}),
  config: () => get('/admin/config'),
  updateConfig: (payload) => put('/admin/config', payload),
  auditLogs: (params) => get('/admin/audit-logs', params),
  auditLog: (id) => get(`/admin/audit-logs/${id}`),
  applyAuthMode: (payload) => post('/admin/users/apply-auth-mode', payload),
  dbStatus: () => get('/admin/system/db-status'),
}

/** Authenticated blob download (keeps the Authorization header, unlike a plain <a href>). */
export async function downloadAuditExport(params) {
  const response = await http.get('/admin/audit-logs/export', { params, responseType: 'blob' })
  const disposition = response.headers?.['content-disposition'] || ''
  const match = /filename="?([^";]+)"?/.exec(disposition)
  const filename = match ? match[1] : 'auth_logs.csv'
  const url = window.URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
  return {
    total: Number(response.headers?.['x-export-total'] || 0),
    truncated: response.headers?.['x-export-truncated'] === 'true',
    filename,
  }
}
