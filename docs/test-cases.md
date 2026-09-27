# test-cases.md — Bộ Test Case đầy đủ cho 3 vai trò

> Owner: QA Lead · Status: **Baseline v1.0**
> Phạm vi: toàn bộ 3 vai trò (Student / Teacher / Administrator) trên cả 3 chế độ xác thực S1/S2/S3.
> Mỗi case đều ghi rõ **dữ liệu mẫu (fixture)** cần dùng — dữ liệu này được tạo tự động bởi
> `backend/.../seed/DataSeeder.java` khi bảng `users` còn rỗng.
> Truy vết: `docs/use-cases.md` (UC), `docs/ba-rules.md` (BR), `docs/qa-rules.md` (luật bằng chứng).

---

## 1. Dữ liệu mẫu (fixtures) — bắt buộc đọc trước khi test

### 1.1 Tài khoản

| Tài khoản | Vai trò | Mật khẩu | Trạng thái | Dùng cho |
|---|---|---|---|---|
| `admin01` | ADMIN | `admin123` | ACTIVE | toàn bộ case nhóm ADM + AUTH |
| `teacher01` | TEACHER | `teacher123` | ACTIVE | **lớp CS101** — mọi case giáo viên |
| `teacher02` | TEACHER | `teacher123` | ACTIVE | lớp CS102 — case "không thấy dữ liệu lớp người khác" |
| `student01` | STUDENT | `student123` | ACTIVE | CS101 + CS102 — case nộp bài |
| `student02` | STUDENT | `student123` | ACTIVE | CS101 + CS102 — case đã nộp / đã chấm / hết lượt |
| `student03` | STUDENT | `student123` | ACTIVE | CS103 + CS104 — **KHÔNG thuộc CS101** → case "không được phép" |
| `locked01` | STUDENT | `student123` | **LOCKED** (khóa 15 phút) | case tài khoản đang bị khóa |
| `disabled01` | STUDENT | `student123` | **DISABLED** | case tài khoản bị vô hiệu hóa |
| `student_mfa01` | STUDENT | `student123` | ACTIVE · **MFA đã đăng ký** | case TOTP thật bằng app xác thực |
| `student_lonely01` | STUDENT | `student123` | ACTIVE · **không thuộc lớp nào** | case empty-state (không có dữ liệu) |

**TOTP secret cố định** cho `student_mfa01` (để dùng app xác thực thật, RFC 6238 sample — chỉ dùng lab):
```
JBSWY3DPEHPK3PXP
otpauth://totp/AuthShield%20360:student_mfa01?secret=JBSWY3DPEHPK3PXP&issuer=AuthShield%20360&algorithm=SHA1&digits=6&period=30
```

### 1.2 Lớp học

| Lớp | Giáo viên | Học sinh | Ghi chú |
|---|---|---|---|
| **CS101** | `teacher01` | `student01`, `student02` | **lớp trọng tâm của mọi case nộp bài** |
| CS102 | `teacher02` | `student01`, `student02`, `student03` | case cách ly dữ liệu giữa 2 giáo viên |
| CS103 … CS120 | teacher03…teacher20 | phân bổ xoay vòng | dữ liệu nền cho biểu đồ/thống kê |
| *(không lớp)* | – | `student_lonely01` | case empty-state |

### 1.3 Bài tập của CS101 (mỗi bài ứng với một quy tắc nộp bài)

| # | Tên bài tập | Hạn nộp | allowLate | lateCutoff | resubmit | maxAttempts | Trạng thái | Dữ liệu có sẵn |
|---|---|---|---|---|---|---|---|---|
| **A1** | Assignment 1 - Loops and arrays | +7 ngày | ✔ | +10 ngày | ✔ | 3 | PUBLISHED | `student02` đã nộp **ON_TIME**; `student01` **chưa nộp** |
| **A2** | Assignment 2 - Recursion (late allowed) | −2 ngày | ✔ | +5 ngày | ✔ | 3 | PUBLISHED | `student02` đã nộp **LATE** |
| **A3** | Assignment 3 - Data structures (no late) | −2 ngày | ✘ | – | ✔ | 3 | PUBLISHED | chưa ai nộp |
| **A4** | Assignment 4 - Resubmission allowed | +3 ngày | ✔ | +6 ngày | ✔ | 5 | PUBLISHED | `student01` nộp lần 1; `student02` **đã chấm 85** |
| **A5** | Assignment 5 - Late window closed | −3 ngày | ✔ | **−1 ngày** | ✔ | 3 | PUBLISHED | chưa ai nộp |
| **A6** | Assignment 6 - Single attempt only | +2 ngày | ✔ | – | ✔ | **1** | PUBLISHED | `student02` **đã dùng hết 1 lượt** |
| **A7** | Assignment 7 - Resubmission not allowed | +2 ngày | ✔ | – | **✘** | 3 | PUBLISHED | `student02` đã nộp 1 lần |
| **A8** | Midterm exam (closed) | −1 ngày | ✘ | – | ✘ | 1 | **CLOSED** | chưa ai nộp |

### 1.4 Cấu hình mặc định khi seed

| Tham số | Giá trị mặc định | Ghi chú |
|---|---|---|
| `mode` | **S1** | đổi sang S2/S3 ở *Admin → Cấu hình xác thực* |
| `otpValiditySeconds` | 90 | case "OTP hết hạn" cần chờ > 90s, hoặc giảm xuống 30s để test nhanh |
| `resendCooldownSeconds` | 60 | case 429 |
| `maxResend` | 3 | case vượt số lần gửi lại |
| `maxFailedAttempts` | 5 | case lockout |
| `lockoutDurationsSeconds` | 60,300,900 | khóa tăng dần 1' → 5' → 15' |
| `requireCaptchaAfter` | 3 | captcha chống robot |

> **Cách ép trạng thái về ban đầu bất cứ lúc nào:** chạy `db/reset.sql` rồi khởi động lại backend (UC-14).

### 1.5 Ma trận phân quyền (để đối chiếu ở các case RBAC)

| Chức năng | Student | Teacher | Admin |
|---|---|---|---|
| Hồ sơ học sinh | của mình | lớp mình phụ trách | toàn quyền |
| Bài tập | xem + nộp | tạo/sửa/đóng + chấm | xem/quản lý |
| Kết quả thi | của mình | nhập cho lớp mình | xem/quản lý |
| Người dùng & vai trò | ✘ | ✘ | ✔ |
| Cấu hình xác thực | ✘ | ✘ | ✔ |
| Nhật ký xác thực | ✘ | ✘ | ✔ |

---

## 2. NHÓM AUTH — Xác thực & bảo mật (mọi vai trò)

| ID | Vai trò | Điều kiện trước | Dữ liệu | Bước thực hiện | Kết quả mong đợi | UC/BR |
|---|---|---|---|---|---|---|
| AUTH-01 | cả 3 | mode = S1 | `admin01`, `teacher01`, `student01` | Đăng nhập đúng username + mật khẩu | `AUTHENTICATED`, có token, vào đúng dashboard theo vai trò; log `LOGIN_ATTEMPT` + `LOGIN_SUCCESS` | UC-01 |
| AUTH-02 | cả 3 | mode = S1 | `student01` | Nhập **sai mật khẩu** (lần 1) | Thông báo *"Incorrect username or password…"* (KHÔNG nói sai user hay pass); ô mật khẩu bị xóa; log `LOGIN_FAIL/INVALID_CREDENTIALS`; counter = 1 | UC-01 |
| AUTH-03 | cả 3 | mode = S1 | `student01` | Sai mật khẩu **4 lần liên tiếp** | Vẫn bị từ chối nhưng **chưa khóa**; `failed_attempts = 4`; không có `LOCKOUT_TRIGGERED` | UC-10/BR-04 |
| AUTH-04 | cả 3 | mode = S1 | `student01` | Sai mật khẩu **lần thứ 5** | **`LOCKOUT_TRIGGERED`**; tài khoản `LOCKED`, `locked_until = now + 60s`; UI hiện đếm ngược `mm:ss` và khóa nút Đăng nhập | UC-10/BR-04 |
| AUTH-05 | cả 3 | đang bị khóa | `locked01` | Đăng nhập **bằng mật khẩu đúng** | Bị từ chối `ACCOUNT_LOCKED` + thông báo kèm thời gian còn lại; log ghi nhận | UC-10/A1 |
| AUTH-06 | cả 3 | hết thời gian khóa | `student01` sau 60s | Đăng nhập lại đúng | Thành công; log `LOCKOUT_RELEASED`; counter reset về 0 | UC-10/B5 |
| AUTH-07 | cả 3 | – | user không tồn tại | Đăng nhập username lạ | Thông báo **giống hệt** AUTH-02 (không lộ user tồn tại hay không) | UC-01 |
| AUTH-08 | cả 3 | – | `disabled01` | Đăng nhập bằng **mật khẩu đúng** | Từ chối `ACCOUNT_DISABLED` ("This account is disabled…") | UC-07 |
| AUTH-09 | cả 3 | – | – | Bỏ trống username / mật khẩu < 8 ký tự | Lỗi validate `400` với thông báo tiếng Anh tương ứng; không gọi xuống service | UC-01 §1.2 |
| AUTH-10 | cả 3 | mode = **S2** | `student01` | Nhập đúng username + mật khẩu | Bước 1 OK → trả `OTP_REQUIRED`, factor `MOBILE_OTP`, có `challengeToken`; log `OTP_SENT` | UC-02 |
| AUTH-11 | cả 3 | đang ở bước OTP của AUTH-10 | `deliveryCode` hiển thị (do `AUTHSHIELD_EXPOSE_OTP=true`) | Nhập **đúng** Mobile OTP | `AUTHENTICATED`; log `OTP_VERIFY_SUCCESS` + `LOGIN_SUCCESS` (factor `MOBILE_OTP`) | UC-02 |
| AUTH-12 | cả 3 | đang ở bước OTP | – | Nhập **sai** Mobile OTP | Từ chối `OTP_INVALID`; log `OTP_VERIFY_FAIL/INVALID_OTP`; **counter lockout tăng** (VĐ-04) | UC-02/A2 |
| AUTH-13 | cả 3 | đang ở bước OTP | cấu hình `otpValiditySeconds=30` (test nhanh) | Nhập OTP **sau khi hết hạn** | Từ chối `OTP_EXPIRED`; log `OTP_EXPIRED/EXPIRED_OTP`; UI gợi ý gửi lại mã | UC-02/A3 |
| AUTH-14 | cả 3 | đang ở bước OTP | – | Nhập sai OTP **đủ 5 lần** | Kích hoạt **lockout** giống AUTH-04 → chứng minh OTP sai cũng tính vào ngưỡng (VĐ-04) | UC-10/VĐ-04 |
| AUTH-15 | cả 3 | đang ở bước OTP | – | Bấm **Gửi lại mã** ngay (trong 60s) | Nút chuyển *"Sending…"* → `429 RESEND_OTP_THROTTLED`; **không sinh mã mới**; UI toast *"Too many requests…"* | UC-04 §4.3 |
| AUTH-16 | cả 3 | sau cooldown 60s | – | Bấm **Gửi lại mã** | `RESEND_OTP_SUCCESS`; **mã cũ mất hiệu lực** (nhập lại mã cũ → `OTP_INVALID`); countdown reset 60s | UC-04 §4.4 |
| AUTH-17 | cả 3 | đang ở bước OTP | – | Bấm **Gửi lại mã lần thứ 4** | `RESEND_OTP_LIMIT_EXCEEDED` → *"You have requested too many codes. Please start the sign-in process again."* + quay về bước 1 | UC-04/VĐ-07 |
| AUTH-18 | cả 3 | `requireCaptchaAfter = 3` | – | Yêu cầu OTP **> 3 lần trong 120s** (login lại nhiều lần) | Hệ thống trả `CAPTCHA_REQUIRED` kèm câu hỏi (vd *"Solve: 5 + 3 = ?"*) → **có captcha chống robot** | UC-02/UC-03 |
| AUTH-19 | cả 3 | đang bị yêu cầu captcha | – | Nhập **sai** đáp án captcha | Vẫn `CAPTCHA_REQUIRED` (cấp câu hỏi mới), không cấp OTP | UC-02 |
| AUTH-20 | cả 3 | đang bị yêu cầu captcha | – | Nhập **đúng** đáp án captcha | Bộ đếm reset, OTP được cấp bình thường | UC-02 |
| AUTH-21 | cả 3 | mode = **S3** | `student01` | Nhập đúng mật khẩu → đúng Mobile OTP | Hệ thống yêu cầu tiếp **Email OTP** (`OTP_REQUIRED`, factor `EMAIL_OTP`); log `EMAIL_OTP_SENT` | UC-03 |
| AUTH-22 | cả 3 | đang ở bước Email OTP | – | Nhập **đúng** Email OTP | `AUTHENTICATED`; log `LOGIN_SUCCESS` (factor `EMAIL_OTP`); vào dashboard | UC-03 |
| AUTH-23 | cả 3 | đang ở bước Email OTP | – | Nhập **sai** Email OTP | Từ chối `OTP_INVALID`; log `EMAIL_OTP_FAILED/INVALID_OTP` | UC-03 |
| AUTH-24 | cả 3 | đang ở bước Email OTP | – | Nhập Email OTP đã **hết hạn** | Từ chối `OTP_EXPIRED`; log `EMAIL_OTP_EXPIRED` | UC-03 |
| AUTH-25 | cả 3 | đang ở bước Email OTP | – | Gửi lại Email OTP **4 lần** | `RESEND_OTP_LIMIT_EXCEEDED` → bắt đầu lại từ Bước 1 (VĐ-07) | UC-04 |
| AUTH-26 | cả 3 | mode = S2/S3 | `student_mfa01` (đã đăng ký TOTP) | Nhập đúng mật khẩu, rồi **mã 6 số từ app xác thực** (secret `JBSWY3DPEHPK3PXP`) | `AUTHENTICATED` bằng TOTP; không cần hiển thị mã trên UI | UC-02/UC-09 |
| AUTH-27 | cả 3 | đăng nhập S2/S3, chưa đăng ký MFA | `teacher01` | Sau khi vào app, mở **Hồ sơ & MFA** → *Bắt đầu thiết lập* | Hiện **mã QR** + mã secret; đề xuất nhập mã 6 số để xác nhận (UC-09) | UC-09 |
| AUTH-28 | cả 3 | đang ở màn đăng ký MFA | – | Nhập **sai** mã xác nhận | `INVALID_MFA_CODE`; log `MFA_ENROLL_FAIL/INVALID_OTP` | UC-09 |
| AUTH-29 | cả 3 | đang ở màn đăng ký MFA | – | Nhập **đúng** mã xác nhận | `MFA_ENROLL_SUCCESS`; từ lần đăng nhập sau phải nhập OTP | UC-09 |
| AUTH-30 | cả 3 | đã đăng nhập | bất kỳ | Bấm **Đăng xuất** rồi dùng lại **token cũ** (DevTools/Postman) | Logout thành công + `LOGOUT`; token cũ → `401 SESSION_REPLAY_ATTEMPT/INVALID_SESSION`; nút Back của trình duyệt không mở lại được dashboard | UC-06/BR-08 |
| AUTH-31 | cả 3 | đã đăng nhập | – | Gửi request với **token bị sửa 1 ký tự** | `401` (chữ ký HMAC không hợp lệ) | UC-06/BR-08 |
| AUTH-32 | cả 3 | giảm `sessionTimeoutMinutes=1` (test nhanh) | – | Để yên quá thời gian chờ rồi thao tác | `401` (phiên không còn hợp lệ — `SESSION_EXPIRED` hoặc `UNAUTHENTICATED`) → tự đăng xuất, về trang đăng nhập | UC-06/A1 |

---

## 3. NHÓM STU — Học sinh

> Mọi case dùng lớp **CS101** (giáo viên `teacher01`), trừ khi ghi khác.

| ID | Điều kiện trước | Dữ liệu | Bước thực hiện | Kết quả mong đợi | UC/BR |
|---|---|---|---|---|---|
| STU-01 | đăng nhập `student01` | bài **A1** (chưa nộp) | Nộp 1 tệp hợp lệ (vd `.pdf`/`.txt`) | `201`, `submissionStatus = ON_TIME`, `attemptNumber = 1`; log `SUBMISSION_CREATE`; giáo viên `teacher01` nhận thông báo *"New submission"* | UC-A1 |
| STU-02 | `student01` | bài **A2** (đã quá hạn, còn trong hạn nộp muộn) | Nộp bài | `201`, `submissionStatus = LATE` (cờ trừ điểm theo `latePenaltyPct`); log `SUBMISSION_CREATE` + thông báo **Late submission** cho GV | UC-A2 |
| STU-03 | `student01` | bài **A3** (`allowLate = false`) | Nộp bài sau hạn | `409 LATE_NOT_ALLOWED`; UI: *"The deadline has passed…"*; log `SUBMISSION_BLOCKED/LATE_NOT_ALLOWED` | UC-A2 |
| STU-04 | `student01` | bài **A5** (`allowLate = true` nhưng **đã qua cutoff**) | Nộp bài | `409 LATE_NOT_ALLOWED` (hết cửa sổ nộp muộn); log `SUBMISSION_BLOCKED` | UC-A2/A2 |
| STU-05 | `student01` | bài **A8** (CLOSED) | Nộp bài | `409 SUBMISSION_LOCKED`; UI: *"This assignment is closed…"*; log `SUBMISSION_BLOCKED/SUBMISSION_LOCKED` | UC-A3 |
| STU-06 | đăng nhập `student03` (**không thuộc CS101**) | bài A1 của CS101 (gọi qua API/URL trực tiếp) | Nộp bài vào lớp mình không tham gia | `403 NOT_ENROLLED`; log `PRIVILEGE_VIOLATION/ACCESS_DENIED` — **không được phép** | UC-05/BR-05 |
| STU-07 | `student02` (đã nộp 1 lần) | bài **A6** (`maxAttempts = 1`) | Nộp lại | `409 MAX_ATTEMPTS_REACHED`; bài nộp cũ **không đổi** | UC-A3/A4 |
| STU-08 | `student02` (đã nộp 1 lần) | bài **A7** (`allowResubmission = false`) | Nộp lại | `409 RESUBMISSION_NOT_ALLOWED`; tệp cũ giữ nguyên | UC-A3 |
| STU-09 | `student01` (đã nộp 1 lần, bài chưa chấm) | bài **A4** (cho nộp lại, max 5) | Nộp tệp mới | `201`, `attemptNumber = 2`; **lịch sử giữ cả 2 lần**, lần 2 là *"Đang dùng"*; log `SUBMISSION_UPDATE` | **UC-A4** |
| STU-10 | `student02` (bài đã được chấm 85) | bài **A4** | Nộp lại | `409 RESUBMISSION_NOT_ALLOWED` ("đã được chấm điểm") | UC-A3 |
| STU-11 | `student01` | bài A4 | Tải tệp ở lần nộp của **chính mình** | `200`, tải được tệp | UC-A1 |
| STU-12 | `student01` | lấy `id` bài nộp của `student02` | Tải tệp của người khác (sửa URL) | `403/404` — không xem được bài của người khác; ghi log vi phạm | BR-05 |
| STU-13 | `student01` | – | Xem **Kết quả thi** | Chỉ thấy kết quả của chính mình (2 bài) | UC-05 |
| STU-14 | `student01` | – | Gọi API admin/teacher (vd `/api/v1/admin/users`) | `403` + log `PRIVILEGE_VIOLATION/ACCESS_DENIED` | UC-05 TC-08 |
| STU-15 | `student01` | bài A1 | Nộp **tệp sai định dạng** (vd `.exe`) | `400 INVALID_FILE` ("Unsupported file type") | UC-A1/A1 |
| STU-16 | `student01` | bài A1 | Nộp tệp **> 10 MB** | `400 INVALID_FILE` ("exceeds the allowed size") | UC-A1/A1 |
| STU-17 | `student01` | bài A1 | Nộp tệp **rỗng** | `400 INVALID_FILE` | UC-A1/A1 |
| STU-18 | `student01` | – | Mở **Bài tập của tôi** | Chỉ liệt kê bài tập của các lớp đã tham gia (CS101 + CS102); **không thấy** bài của lớp khác | UC-05 |
| STU-19 | `student_lonely01` (không lớp) | – | Mở dashboard | Trạng thái rỗng đúng thiết kế: 0 lớp, 0 bài tập, biểu đồ hiện *"Chưa phát sinh dữ liệu"* | UC-05 |
| STU-20 | `student01` | có bài tập ở các trạng thái khác nhau | Mở dashboard, bấm vào biểu đồ *Trạng thái bài tập* | Điều hướng tới danh sách đã lọc `?bucket=…` tương ứng; có nút **← Quay lại** | UC-05 |
| STU-21 | `student01` | có thông báo | Mở **chuông**, bấm 1 thông báo | Badge giảm; thông báo chuyển **đã đọc**; điều hướng tới trang liên quan | UC-11*(notify)* |
| STU-22 | `student01` | có ≥ 2 thông báo chưa đọc | Bấm **Mark all as read** | Tất cả chuyển đã đọc, badge về `0`, trạng thái **lưu lại** sau khi tải lại trang | — |

---

## 4. NHÓM TEA — Giáo viên

| ID | Điều kiện trước | Dữ liệu | Bước thực hiện | Kết quả mong đợi | UC/BR |
|---|---|---|---|---|---|
| TEA-01 | đăng nhập `teacher01` | – | Tạo lớp mới (mã `CS199`, tên hợp lệ) | `201`, lớp xuất hiện trong danh sách; log `ASSIGNMENT_CREATE`-tương ứng (lớp) | UC-07 |
| TEA-02 | `teacher01` | mã lớp đã tồn tại `CS101` | Tạo lớp với mã `CS101` | `409 CONFLICT` — *"That class code already exists."* | UC-07 |
| TEA-03 | `teacher01` | – | Tạo lớp **bỏ trống tên** / tên > 120 ký tự | `400` validate, thông báo lỗi tiếng Anh rõ trường nào sai | UC-07 |
| TEA-04 | `teacher01`, lớp CS101 | `student03` (chưa ở CS101) | Thêm học sinh vào lớp | `200`; `student03` nhận **thông báo** *"Added to a class"*; sĩ số tăng | UC-07 |
| TEA-05 | `teacher01` | chọn tài khoản `teacher02` (không phải học sinh) | Thêm vào lớp | `409` — *"Only student accounts can be added to a class."* | UC-07 |
| TEA-06 | `teacher01` | `student01` **đã ở** CS101 | Thêm lại `student01` | Không tạo bản ghi trùng (idempotent), sĩ số không đổi | UC-07 |
| TEA-07 | `teacher01` | lớp CS101 | Tạo bài tập đầy đủ (hạn tương lai, allowLate, resubmit, maxScore 100) | `201`; **toàn bộ học sinh CS101 nhận thông báo** *"New assignment"*; log `ASSIGNMENT_CREATE` | UC-05/BR-07 |
| TEA-08 | `teacher01` | – | Tạo bài tập **thiếu tiêu đề** hoặc **thiếu hạn nộp** | `400` validate | UC-05 |
| TEA-09 | `teacher01` | – | Tạo bài tập với `maxScore = 0` | `400` — *"Maximum score must be at least 1."* | UC-05 |
| TEA-10 | `teacher01` | – | Tạo bài tập **hạn ở quá khứ** + `allowLate = false` | Vẫn tạo được (`201`) nhưng học sinh sẽ bị chặn khi nộp (`LATE_NOT_ALLOWED`) — chọn thời gian "sai" về mặt nghiệp vụ | UC-A2 |
| TEA-11 | `teacher01` | bài A4 | Sửa **hạn nộp** sang tương lai | `200`; `ASSIGNMENT_UPDATE`; học sinh nhận *"Assignment updated"* | UC-05 |
| TEA-12 | `teacher01` | bài A2 | **Đóng** bài tập | `200`; trạng thái `CLOSED`; học sinh nhận *"Assignment closed"*; mọi nộp/cập nhật sau đó bị `409 SUBMISSION_LOCKED` | UC-A3 |
| TEA-13 | `teacher01` | bài nộp `student02` ở A1 chưa chấm | Chấm điểm **hợp lệ** (vd 90 + nhận xét) | `200`; `SUBMISSION_GRADE`; **học sinh nhận thông báo** *"graded: 90/100"*; bài nộp đánh dấu đã chấm | UC-05/BR-07 |
| TEA-14 | `teacher01` | bài nộp | Chấm điểm **> maxScore** (vd 150) | `400` — *"The score cannot exceed the maximum score (100)."* | UC-05 |
| TEA-15 | `teacher01` | bài nộp | Chấm điểm **âm** (−5) | `400` validate | UC-05 |
| TEA-16 | `teacher01` | bài nộp | Chấm điểm = **đúng bằng maxScore** (100) | `200` (biên trên hợp lệ) | UC-05 |
| TEA-17 | `teacher02` | bài nộp thuộc **CS101** (của `teacher01`) | Chấm điểm bài đó | `403` + log vi phạm — **không được phép** chấm lớp người khác | UC-05/BR-05 |
| TEA-18 | `teacher02` | – | Mở "Quản lý bài tập" | Chỉ thấy bài tập của **CS102**; không thấy CS101 | UC-05 |
| TEA-19 | `teacher01` | – | Mở chi tiết CS101 → **Danh sách học sinh** | Chỉ hiện `student01`, `student02`; bấm 1 học sinh → trang chi tiết, có nút **← Quay lại** | UC-05 |
| TEA-20 | `teacher01` | – | Xem chi tiết một học sinh **không thuộc lớp mình** (vd `student03` qua URL `/teacher/students/<id>`) | `403` (hoặc `404` nếu không phải học sinh) | BR-05 |
| TEA-21 | `teacher01` | – | Mở dashboard + biểu đồ | *Bài nộp theo bài tập* / *Tiến độ chấm* / *Học sinh theo lớp* hiển thị **số liệu thật**; bấm cột → mở đúng bài tập/lớp | UC-05 |
| TEA-22 | `teacher01` | – | Gọi API admin (vd `/api/v1/admin/config`) | `403` + log vi phạm | UC-05 TC-09 |

---

## 5. NHÓM ADM — Quản trị viên

| ID | Điều kiện trước | Dữ liệu | Bước thực hiện | Kết quả mong đợi | UC/BR |
|---|---|---|---|---|---|
| ADM-01 | `admin01` | – | Mở **Người dùng & vai trò** | 45 tài khoản (1 admin + 20 GV + 20 HS + 4 fixture); phân trang 20/trang | UC-07 |
| ADM-02 | `admin01` | – | Tạo user hợp lệ (`student21`) | `201`, log `USER_CREATE` | UC-07 |
| ADM-03 | `admin01` | username `student01` đã tồn tại | Tạo user trùng username | `409` — *"That username or email is already taken."* | UC-07 |
| ADM-04 | `admin01` | – | Tạo user với email sai định dạng | `400` validate | UC-07 |
| ADM-05 | `admin01` | – | Đổi vai trò một user (Student → Teacher) | `200`; log `ROLE_ASSIGN` ghi rõ `admin` tác động lên `target_user` | UC-07 |
| ADM-06 | `admin01` | – | Xóa một user (fixture vừa tạo) | Hỏi xác nhận → `200`; log `USER_DELETE`; **audit log của user vẫn giữ lại** | UC-07/BR-07 |
| ADM-07 | `admin01` | `teacher01` đã đăng ký MFA | Bấm **Reset MFA** | `MFA_RESET`; tài khoản về *chưa đăng ký*; lần sau phải đăng ký lại (UC-09) | UC-16 |
| ADM-08 | `admin01` | cấu hình chung = S1 | **Áp dụng S2 cho tất cả user** | `CONFIG_CHANGE`; mọi user có `auth_mode_override = S2`, `mfa_enabled = true`; đăng nhập bắt buộc OTP | C-05 |
| ADM-09 | `admin01` | đang override S2 | **Áp dụng "Use global configuration"** | Xóa override; đăng nhập quay lại theo cấu hình chung (S1) | C-05 |
| ADM-10 | `admin01` | – | Đổi chế độ chung **S1 → S3** | `200`; log `CONFIG_CHANGE` ghi **giá trị cũ → mới**; lần đăng nhập sau yêu cầu Mobile + Email OTP | UC-08 |
| ADM-11 | `admin01` | – | Nhập chế độ không hợp lệ (`S9`) | `400` — *"Invalid authentication mode (allowed: S1, S2, S3 or INHERIT)."* | UC-08 |
| ADM-12 | `admin01` | – | Đặt `otpValiditySeconds = 20` (ngoài 30–300) | `400` — *"OTP validity must be at least 30 seconds."* | UC-08 §8.2 |
| ADM-13 | `admin01` | – | Đặt `maxFailedAttempts = 0` | `400` — ngưỡng tối thiểu 1 | UC-08 |
| ADM-14 | `admin01` | – | Nhập **mật khẩu SMTP** mới rồi lưu, sau đó tải lại trang | Lưu OK; API **không trả lại** mật khẩu (chỉ cờ `smtpPasswordSet = true`); DB lưu dạng **mã hóa AES-GCM** | BR-10 |
| ADM-15 | `admin01` | có nhiều log | Mở **Nhật ký xác thực**, lọc theo `action = LOGIN_FAIL` | Chỉ hiện bản ghi khớp; phân trang **≤ 50 dòng/trang**; log meta `LOG_VIEW` được ghi | UC-11 |
| ADM-16 | `admin01` | – | Bấm **Xuất CSV** (và JSON) | Tải file `auth_logs_S1_YYYYMMDD_HHMMSS.csv`; **email/số điện thoại bị che** (`stu******@mailtrap.io`, `0912***678`); log `LOG_EXPORT` | UC-11 §11.3 |
| ADM-17 | `admin01` | bộ lọc trả về > 10.000 bản ghi (hoặc giả lập) | Bấm **Xuất CSV** | Hiện popup cảnh báo 10.000 bản ghi + nút `btn-confirm-export-limit`; xác nhận → chỉ xuất 10.000 bản ghi mới nhất | UC-11 §11.3 |
| ADM-18 | `admin01` | – | Mở **Hệ thống & dữ liệu** | Hiển thị `MySQL`, `persistent = true`, JDBC URL và **số bản ghi từng bảng** (14 bảng) | C-06 |
| ADM-19 | `admin01` | – | Mở **So sánh S1/S2/S3** | Bảng mức bảo mật / khả năng dùng / rủi ro + **số liệu đo thật** (thành công, thất bại, lỗi OTP) | UC-15 |
| ADM-20 | `student01` | – | Gọi API admin (vd `/api/v1/admin/audit-logs`) | `403` + log vi phạm | UC-05 TC-08 |

---

## 6. NHÓM X — Xuyên suốt & phi chức năng

| ID | Kiểm thử | Dữ liệu | Kết quả mong đợi | BR |
|---|---|---|---|---|
| X-01 | Mật khẩu lưu dạng băm | DB `users.password_hash` | Chuỗi `$2a$12$…` (BCrypt cost 12), **không** có mật khẩu rõ | BR-02 |
| X-02 | Không lộ secret qua API | `GET /admin/config` | Không trả `smtpPassword`; OTP/secret không bao giờ có trong log | BR-10 |
| X-03 | Audit đầy đủ trường | bảng `audit_logs` | Có `event_time, event_id, event_action, status, user_identifier, role, auth_factor, auth_mode, client_ip, session_id, failure_reason, correlation_id` | BR-07 |
| X-04 | Thời gian hiển thị log < 5 giây | đăng nhập 1 lần rồi mở Nhật ký | Bản ghi xuất hiện **ngay** (< 5s) | BR-06 |
| X-05 | Che dữ liệu khi xuất log | file CSV/JSON xuất ra | Email/số điện thoại bị mask một phần | UC-11 |
| X-06 | Dữ liệu bền vững sau restart | restart backend | Tài khoản, vai trò, cấu hình MFA, audit log **còn nguyên**; seeder **không** chạy lại | BR-12 |
| X-07 | Reset môi trường | `db/reset.sql` + restart | Hệ thống về trạng thái chuẩn (45 tài khoản seed lại) trong ≤ 60s | UC-14 |
| X-08 | RBAC kiểm tra ở **server** | gọi API trực tiếp bằng Postman (bỏ qua UI) | Vẫn bị `403` — không tin tưởng client | BR-05 |
| X-09 | Thông báo đúng người nhận | GV tạo bài → HS nhận; HS nộp → GV nhận; GV chấm → HS nhận | Không gửi nhầm vai trò | — |
| X-10 | Trạng thái thông báo lưu bền | đánh dấu đã đọc → tải lại trang | Vẫn ở trạng thái đã đọc | — |
| X-11 | Biểu đồ dùng dữ liệu động | tạo 1 bài nộp mới | Biểu đồ *Tiến độ chấm* tăng ngay (`Awaiting grade +1`) | UC-15 |
| X-12 | Bảng dài bị ép cột? | trang Người dùng ở màn hình nhỏ | Bảng **cuộn ngang**, nút thao tác **cùng 1 dòng**, không vỡ layout | — |

---

## 7. Quy tắc thực thi & bằng chứng

1. **Ma trận 7 cột** (theo UC-13): `Test ID · User/Role · Test Action · Expected Result · Actual Result · Pass/Fail · Evidence`.
2. Mỗi lần chạy ghi rõ **chế độ xác thực** (S1/S2/S3) và **phiên bản commit** (`git rev-parse --short HEAD`).
3. Bằng chứng: ảnh chụp màn hình **hoặc** request/response (Burp/ZAP) **kèm dòng audit log tương ứng**.
4. Case **Fail** → ghi nguyên nhân, sửa, chạy lại và **giữ cả hai kết quả**.
5. Ca kiểm thử bảo mật chỉ trên **môi trường lab** (BR-11).
6. Với ca lockout: dùng `locked01` để kiểm tra trạng thái đang khóa; dùng `student01` + sai mật khẩu 5 lần để kiểm tra cơ chế kích hoạt (nhớ chờ hết 60s hoặc reset DB).
7. Với ca OTP hết hạn: giảm `otpValiditySeconds` xuống **30** ở *Admin → Cấu hình xác thực* để test nhanh thay vì chờ 90s.
8. Với ca captcha: đặt `requireCaptchaAfter = 3`, đăng nhập lại vài lần để vượt ngưỡng.

### 7.1 Tự động hoá (đã hiện thực)

Toàn bộ case ở trên đã được tự động hoá ở hai tầng, chạy lại được bất cứ lúc nào:

| Tầng | Vị trí | Bao phủ | Lệnh | Báo cáo |
|---|---|---|---|---|
| **API** (JUnit + Spring Boot Test, H2) | `backend/src/test/java/com/authshield360/automation/` | `AuthApiIT` (AUTH-01…AUTH-33) · `StudentApiIT` (STU-01…STU-22) · `TeacherApiIT` (TEA-01…TEA-22) · `AdminApiIT` (ADM-01…ADM-20) · `CrossCuttingApiIT` (X-01…X-11) | `cd backend && .\mvnw.cmd verify` | `target/surefire-reports/` |
| **Giao diện** (Playwright/Chromium) | `e2e/tests/` | login wizard S1/S2/S3 · khoá nút khi chờ · captcha · TOTP · alert nổi trên cùng · trang 403 · nút Back · UX nộp/chấm điểm · hộp thoại admin + tải CSV | `cd e2e && run-e2e.cmd` | `e2e/playwright-report/` |

Ánh xạ: mã case trong tài liệu này trùng tên với `@DisplayName`/tiền tố phương thức trong mã test
(ví dụ `AUTH-04` → `auth04FifthFailureLocks`), nên có thể đối chiếu kết quả tự động với ma trận 7 cột.
Các case chỉ kiểm chứng được bằng mắt hoặc bằng môi trường MySQL (X-06 bền vững sau restart, X-07 reset)
vẫn thực hiện theo quy trình thủ công ở §7 bước 5–6 và ghi bằng chứng vào ma trận.

## 8. Truy vết nhanh

| Nhóm | UC liên quan | Test ID |
|---|---|---|
| Đăng nhập S1/S2/S3 | UC-01, UC-02, UC-03 | AUTH-01…AUTH-26 |
| Resend / Captcha | UC-04, UC-02 §2.3 | AUTH-15…AUTH-20, AUTH-25 |
| RBAC | UC-05 | STU-06, STU-14, TEA-17, TEA-18, TEA-20, TEA-22, ADM-20, X-08 |
| Session / Logout | UC-06 | AUTH-30…AUTH-32 |
| User & Role mgmt | UC-07 | ADM-01…ADM-09, TEA-01…TEA-06 |
| Cấu hình xác thực | UC-08 | ADM-10…ADM-14 |
| MFA enrollment | UC-09 | AUTH-26…AUTH-29 |
| Lockout | UC-10 | AUTH-03…AUTH-06, AUTH-14 |
| Nhật ký & xuất log | UC-11 | ADM-15…ADM-17, X-03…X-05 |
| Nộp bài A1–A4 | UC-A1…UC-A4 | STU-01…STU-12 |
| Bền vững / Reset | UC-14, BR-12 | X-06, X-07 |
| So sánh 3 chế độ | UC-15 | ADM-19, X-11 |
| Reset MFA | UC-16 | ADM-07 |
