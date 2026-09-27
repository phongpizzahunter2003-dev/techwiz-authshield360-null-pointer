package com.authshield360.common;

import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes with the canonical Vietnamese message (ba-rules.md §4).
 */
public enum ErrorCode {

    // --- generic ---
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu yêu cầu."),
    CONFLICT(HttpStatus.CONFLICT, "Dữ liệu đã tồn tại."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."),

    // --- auth (UC-01..UC-04) ---
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không chính xác. Vui lòng thử lại."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "Tài khoản của bạn đã bị tạm khóa do nhập sai quá nhiều lần."),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên."),
    OTP_REQUIRED(HttpStatus.UNAUTHORIZED, "Cần xác minh OTP."),
    OTP_INVALID(HttpStatus.UNAUTHORIZED, "Mã xác minh không chính xác. Vui lòng kiểm tra lại."),
    OTP_EXPIRED(HttpStatus.UNAUTHORIZED, "Mã xác minh đã hết hạn. Vui lòng yêu cầu gửi lại mã mới."),
    OTP_RESENT(HttpStatus.OK, "Mã xác minh mới đã được gửi thành công. Vui lòng kiểm tra."),
    RESEND_THROTTLED(HttpStatus.TOO_MANY_REQUESTS, "Yêu cầu quá thường xuyên. Vui lòng đợi hết thời gian chờ."),
    RESEND_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Bạn đã yêu cầu gửi lại mã quá nhiều lần. Vui lòng bắt đầu lại quy trình đăng nhập."),
    CAPTCHA_REQUIRED(HttpStatus.BAD_REQUEST, "Vui lòng hoàn tất xác thực để chứng minh bạn không phải là robot."),
    INVALID_CHALLENGE(HttpStatus.UNAUTHORIZED, "Phiên xác minh không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại."),
    MFA_NOT_ENROLLED(HttpStatus.CONFLICT, "Tài khoản chưa đăng ký yếu tố xác thực hai lớp."),
    MFA_ALREADY_ENROLLED(HttpStatus.CONFLICT, "Tài khoản đã đăng ký xác thực hai lớp."),
    INVALID_MFA_CODE(HttpStatus.BAD_REQUEST, "Mã xác nhận không đúng. Vui lòng thử lại."),

    // --- session (UC-06) ---
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Phiên làm việc không hợp lệ. Vui lòng đăng nhập lại."),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại."),

    // --- authorization (UC-05) ---
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập vào tài nguyên này. Hành vi vi phạm đã được ghi nhận."),

    // --- users (UC-07) ---
    USERNAME_EXISTS(HttpStatus.CONFLICT, "Tên đăng nhập hoặc Email đã tồn tại. Vui lòng chọn giá trị khác."),
    EMAIL_EXISTS(HttpStatus.CONFLICT, "Tên đăng nhập hoặc Email đã tồn tại. Vui lòng chọn giá trị khác."),

    // --- config (UC-08) ---
    SMTP_CONNECTION_FAILED(HttpStatus.BAD_REQUEST, "Không thể kết nối đến máy chủ SMTP. Vui lòng kiểm tra lại thông tin cấu hình."),
    EMAIL_OTP_UNSUPPORTED(HttpStatus.BAD_REQUEST, "Nền tảng hiện tại không hỗ trợ Email OTP. Vui lòng chọn giải pháp mở rộng được phép."),

    // --- audit (UC-11) ---
    EXPORT_LIMIT_EXCEEDED(HttpStatus.CONTENT_TOO_LARGE, "Dữ liệu tìm kiếm vượt quá giới hạn cho phép của một lần xuất."),

    // --- assignments / submissions (UC-A1..A4) ---
    SUBMISSION_LOCKED(HttpStatus.CONFLICT, "Bài tập đã đóng. Bạn không thể cập nhật bài nộp."),
    RESUBMISSION_NOT_ALLOWED(HttpStatus.CONFLICT, "Bài tập này không cho phép nộp lại."),
    MAX_ATTEMPTS_REACHED(HttpStatus.CONFLICT, "Bạn đã đạt số lần nộp tối đa."),
    LATE_NOT_ALLOWED(HttpStatus.CONFLICT, "Đã quá hạn nộp bài. Hệ thống không nhận bài muộn."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "Tệp không hợp lệ. Vui lòng kiểm tra định dạng và dung lượng."),
    NOT_ENROLLED(HttpStatus.FORBIDDEN, "Bạn không thuộc lớp học của bài tập này.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String message() { return message; }
}
