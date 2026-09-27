// Canonical user-facing messages (ba-rules.md §4). The backend returns the same wording,
// these are used for client-side hints and fallbacks.
export const M = {
  LOGIN_INVALID: 'Tên đăng nhập hoặc mật khẩu không chính xác. Vui lòng thử lại.',
  LOCKOUT_ACTIVE: 'Tài khoản của bạn đã bị tạm khóa do nhập sai quá nhiều lần.',
  OTP_INVALID: 'Mã xác minh không chính xác. Vui lòng kiểm tra lại.',
  OTP_EXPIRED: 'Mã xác minh đã hết hạn. Vui lòng yêu cầu gửi lại mã mới.',
  OTP_RESENT: 'Mã xác minh mới đã được gửi thành công. Vui lòng kiểm tra.',
  RESEND_TOO_SOON: 'Yêu cầu quá thường xuyên. Vui lòng đợi hết thời gian chờ.',
  RESEND_LIMIT: 'Bạn đã yêu cầu gửi lại mã quá nhiều lần. Vui lòng bắt đầu lại quy trình đăng nhập.',
  FORBIDDEN: 'Bạn không có quyền truy cập vào tài nguyên này. Hành vi vi phạm đã được ghi nhận.',
  SESSION_EXPIRED: 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.',
  LOGOUT_OK: 'Bạn đã đăng xuất thành công.',
  CAPTCHA_REQUIRED: 'Vui lòng hoàn tất xác thực để chứng minh bạn không phải là robot.',
  EXPORT_LIMIT:
    'Dữ liệu tìm kiếm vượt quá 10,000 bản ghi. Hệ thống sẽ tự động xuất 10,000 bản ghi mới nhất. Vui lòng thu hẹp khoảng thời gian hoặc điều kiện lọc để lấy đầy đủ dữ liệu.',
  GENERIC: 'Đã xảy ra lỗi. Vui lòng thử lại sau.',
}

export const ROLE_LABEL = {
  STUDENT: 'Học sinh',
  TEACHER: 'Giáo viên',
  ADMIN: 'Quản trị viên',
}

export const ROLE_HOME = {
  STUDENT: '/student',
  TEACHER: '/teacher',
  ADMIN: '/admin',
}

export const ASSIGNMENT_STATUS_LABEL = {
  DRAFT: 'Bản nháp',
  PUBLISHED: 'Đang mở',
  CLOSED: 'Đã đóng',
}
