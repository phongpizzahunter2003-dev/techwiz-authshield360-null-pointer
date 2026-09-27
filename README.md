# AuthShield 360 — School Portal (AuthenticatorShield360)

Một portal trường học giả lập được bảo vệ bằng nền tảng xác thực đa lớp, dùng để **so sánh 3 chế độ
xác thực** (S1 / S2 / S3), thực hành **RBAC**, **audit log**, **lockout** và các kịch bản nộp bài của
học sinh. Đây là bản triển khai đầy đủ (frontend + backend + database + tài liệu quản trị).

| Thành phần | Công nghệ |
|---|---|
| Frontend | React 18 + Vite + Tailwind CSS + React Router + Axios |
| Backend | Spring Boot 4.1 (Java 17) + Spring Security + Spring Data JPA |
| Database | MySQL 8 (chính) · H2 (profile `dev`, chạy ngay không cần cài MySQL) |
| Bảo mật | BCrypt (work factor 12), HMAC token, session registry, AES-GCM cho secret, RBAC phía máy chủ |

> ⚠️ **Chỉ dùng cho môi trường lab** với tài khoản/dữ liệu giả lập (BR-01). Không dùng dữ liệu người thật.

---

## 1. Kiến trúc tổng thể

```
authshieldtest/
├── backend/        Spring Boot API (Maven wrapper kèm sẵn — không cần cài Maven)
├── frontend/       React SPA (Vite)
├── db/             schema.sql · seed.sql · reset.sql
├── docs/           12 tài liệu quản trị (kiến trúc, BA rules, DB, FE/BE rules, ...)
├── docker-compose.yml
└── .env.example
```

Chi tiết xem [`docs/architecture.md`](docs/architecture.md) và [`docs/codegraph.md`](docs/codegraph.md).

---

## 2. Chạy nhanh (dev, không cần MySQL)

Yêu cầu: **Java 17+**, **Node 18+**.

### Backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev      # Windows: .\mvnw.cmd ...
```
- API: http://localhost:8080
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:authshield360`, user `sa`)
- Dữ liệu mẫu được seed tự động ở lần chạy đầu (xem mục 4).

### Frontend

```bash
cd frontend
npm install
npm run dev
```
- SPA: http://localhost:5173 (Vite proxy `/api` → `http://localhost:8080`)

---

## 3. Chạy với MySQL (mục tiêu chính)

```bash
# 1) Tạo schema (tùy chọn — Hibernate cũng tự tạo/ cập nhật ở lần chạy đầu)
mysql -u root -p < db/schema.sql
mysql -u root -p authshield360 < db/seed.sql

# 2) Cấu hình secret qua biến môi trường (KHÔNG commit secret — BR-10)
$env:DB_PASSWORD="..."                       # PowerShell
$env:AUTHSHIELD_SIGNING_KEY="<random 32+ chars>"

# 3) Chạy
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

Profile `mysql` dùng `ddl-auto=update` cho lần provision đầu; sau đó có thể đổi sang `validate`
để bảo vệ chống lệch schema. Sao chép `.env.example` thành `.env` và điền giá trị.

Reset nhanh (UC-14): `docker compose down -v && docker compose up -d` hoặc `db/reset.sql`.

---

## 4. Tài khoản thử nghiệm (giả lập — BR-01)

| Tài khoản | Mật khẩu | Vai trò | Dashboard |
|---|---|---|---|
| `admin01` | `admin123` | Administrator | `/admin` |
| `teacher01` | `teacher123` | Teacher | `/teacher` |
| `student01` | `student123` | Student | `/student` |
| `student02` | `student123` | Student | `/student` |

Mặc định hệ thống chạy ở chế độ **S1 (chỉ mật khẩu)** để đăng nhập ngay. Vào
**Admin → Cấu hình xác thực** để chuyển sang **S2** (thêm Mobile OTP) hoặc **S3** (thêm Email OTP).

> Ở môi trường `dev`, mã OTP được trả về trong phản hồi API và hiển thị trên giao diện để demo —
> không cần SMS/Email thật (VĐ-06). Ở profile `mysql`, `authshield.expose-otp=false`.

---

## 5. Ba chế độ xác thực

| Chế độ | Luồng | Yếu tố |
|---|---|---|
| **S1** | Mật khẩu → phiên | `PASSWORD` |
| **S2** | Mật khẩu → Mobile OTP → phiên | `PASSWORD`, `MOBILE_OTP` |
| **S3** | Mật khẩu → Mobile OTP → Email OTP → phiên | `PASSWORD`, `MOBILE_OTP`, `EMAIL_OTP` |

Mobile OTP hỗ trợ **TOTP** (ứng dụng xác thực, có QR ở trang Hồ sơ) hoặc **SMS mô phỏng**.
Email OTP dùng SMTP cấu hình trong Admin (mặc định mô phỏng, ví dụ Mailtrap).

---

## 6. Ba dashboard theo vai trò (có biểu đồ & drill-down)

- **Học sinh** (`/student`): tổng quan, danh sách bài tập, nộp/ nộp lại bài, lịch sử nộp, kết quả thi.
- **Giáo viên** (`/teacher`): lớp phụ trách, tạo/ sửa/ đóng bài tập, chấm điểm, thêm học sinh vào lớp.
- **Quản trị viên** (`/admin`): người dùng & vai trò, cấu hình xác thực, nhật ký xác thực + xuất file,
  so sánh S1/S2/S3.

### Biểu đồ theo vai trò (mỗi vai trò một logic riêng)

| Vai trò | Biểu đồ | Bấm vào để xem |
|---|---|---|
| Học sinh | PIE trạng thái bài tập; BAR số lần nộp theo bài; BAR điểm theo bài thi | danh sách bài tập đã lọc (`?bucket=`), chi tiết bài tập, kết quả thi |
| Giáo viên | BAR bài nộp theo bài tập; PIE tiến độ chấm; BAR học sinh theo lớp | chi tiết bài tập (chấm điểm), chi tiết lớp (danh sách + kết quả) |
| Quản trị viên | BAR đăng nhập theo chế độ S1/S2/S3; PIE người dùng theo vai trò; BAR sự kiện bảo mật; LINE sự kiện 7 ngày | nhật ký đã lọc (`?mode=`/`?action=`/`?from=`/`?to=`), người dùng theo vai trò, chi tiết sự kiện |

**Mọi thông tin đều bấm được:** thẻ số liệu, biểu đồ, dòng trong bảng đều dẫn tới trang chi tiết;
mỗi trang chi tiết đều có nút **← Quay lại** để trở về trang trước (dùng lịch sử trình duyệt, có
route dự phòng khi mở trực tiếp URL). Xem `docs/fe-rules.md` §8.

## 6.1 Ai thiết lập MFA? (UC-08 / UC-09 / UC-16)

**Mỗi người dùng tự đăng ký MFA cho chính mình** (Học sinh / Giáo viên / Quản trị viên) — quét mã QR
ở trang **Hồ sơ & MFA**. Theo UC-09, sau lần đăng nhập mật khẩu thành công đầu tiên (khi MFA đã bật mà
chưa đăng ký), hệ thống sẽ **nhắc** người dùng thiết lập.

Quản trị viên **không tạo QR thay người dùng**: admin chỉ bật/cấu hình chế độ xác thực (UC-08) và có
thể **đặt lại MFA** cho một tài khoản (UC-16), sau đó người dùng phải đăng ký lại.

### 6.2 Gán S1/S2/S3 cho người dùng (quản trị viên)

Vào **Quản trị → Người dùng & vai trò**:

- Nút **“🛡️ Áp dụng S1/S2/S3 hàng loạt”**: chọn chế độ (S1 / S2 / S3 / Theo cấu hình chung) và phạm vi
  (**Tất cả người dùng** hoặc **chỉ Học sinh / Giáo viên / Quản trị viên**) rồi bấm Áp dụng.
- Trong form sửa từng tài khoản có mục **“Chế độ xác thực áp dụng cho tài khoản”**; cột *Chế độ xác thực*
  trong bảng cho biết tài khoản đang dùng chế độ riêng hay theo cấu hình chung.

Cách hoạt động: mỗi người dùng có trường `auth_mode_override` (S1/S2/S3) trong bảng `users`.
**Chế độ hiệu lực khi đăng nhập** = override của người dùng, nếu trống thì lấy chế độ chung ở
*Cấu hình xác thực*. Gán `S1` sẽ tắt yêu cầu MFA; `S2`/`S3` sẽ bật yêu cầu MFA — nhưng **việc đăng ký
MFA vẫn do chính người dùng thực hiện** (UC-09).

### 6.3 Kiểm tra dữ liệu đã vào CSDL chưa

Vào **Quản trị → Hệ thống & dữ liệu** (`/admin/system`): hiển thị

- Sản phẩm/phiên bản CSDL, JDBC URL, profile đang chạy và CSDL có lưu trữ bền vững hay không.
- **Số bản ghi của từng bảng**, đọc trực tiếp bằng SQL trên kết nối đang hoạt động.

> Lưu ý: profile `dev` dùng **H2 trong bộ nhớ** — dữ liệu *có* được ghi vào CSDL nhưng sẽ mất khi khởi
> động lại backend. Muốn dữ liệu tồn tại lâu dài, chạy profile `mysql` (mục 3).

---

## 7. Kịch bản nộp bài của học sinh (bổ sung theo yêu cầu)

| Mã | Kịch bản | Kết quả |
|---|---|---|
| **UC-A1** | Nộp bài đúng hạn | Lưu `ON_TIME`, lần nộp #1 |
| **UC-A2** | Nộp bài muộn | Nếu `allowLate` → `LATE` (cờ trừ điểm); quá `lateCutoff` hoặc `allowLate=false` → `409 LATE_NOT_ALLOWED` |
| **UC-A3** | Không thể cập nhật bài (đã khóa) | `409 SUBMISSION_LOCKED` / `RESUBMISSION_NOT_ALLOWED` / `MAX_ATTEMPTS_REACHED` + audit `SUBMISSION_BLOCKED` |
| **UC-A4** | Nộp bài và có thể cập nhật tệp đã nộp | Tạo lần nộp mới (`attemptNumber+1`), giữ lịch sử, lần mới nhất là bản chính |

Bảng chân trị đầy đủ nằm ở [`docs/use-cases.md`](docs/use-cases.md) (phần Submission policy).

---

## 8. Bản đồ use case (UC-01 → UC-17)

Toàn bộ 17 use case gốc (đăng nhập, resend OTP, RBAC, logout, quản lý user, cấu hình MFA, lockout,
nhật ký, test matrix, so sánh 3 chế độ, khôi phục tài khoản, step-up auth) được đặc tả tại
[`docs/use-cases.md`](docs/use-cases.md) và ánh xạ FR ↔ UC ở cùng tài liệu.

---

## 9. Tài liệu quản trị (`docs/`)

| File | Nội dung |
|---|---|
| `architecture.md` | Kiến trúc, luồng S1/S2/S3, session/token, ADR |
| `ba-rules.md` | BR-01..BR-12, quyết định VĐ-01..VĐ-08, catalogue thông báo & audit event |
| `database.md` | Mô hình dữ liệu, DDL, ràng buộc |
| `fe-rules.md` | Quy tắc frontend, design tokens, **danh sách element ID bắt buộc** |
| `be-rules.md` | Quy tắc backend, bảo mật, validation, transaction |
| `delivery.md` | Kế hoạch, milestone, deliverable, DoD |
| `go-task-change-spec.md` | Quy trình kiểm soát thay đổi (template + ví dụ) |
| `security-bac.md` | Ma trận kiểm thử broken access control, threat model |
| `codegraph.md` | Đồ thị module, call graph, ownership |
| `devops-rules.md` | Môi trường, build, secret, reset, CI |
| `qa-rules.md` | Chiến lược test, TC-01..TC-12, TC-A1..A4, bằng chứng |
| `use-cases.md` | Đặc tả use case chi tiết + truy vết FR |

---

## 10. Kiểm thử

```bash
cd backend && ./mvnw verify        # unit + slice tests
cd frontend && npm run lint && npm run build
```

Xem [`docs/qa-rules.md`](docs/qa-rules.md) để biết bộ test bắt buộc và quy tắc bằng chứng
(ma trận 7 cột: Test ID · User/Role · Action · Expected · Actual · Pass/Fail · Evidence).

---

## 11. Bảo mật — điểm chính

- Mật khẩu băm **BCrypt work factor 12** (BR-02); OTP lưu dạng hash; secret MFA & SMTP mã hóa **AES-GCM** (BR-10).
- **RBAC kiểm tra phía máy chủ** trên mọi endpoint (`@PreAuthorize` + URL rules) — không tin client.
- **Lockout** 5 lần sai → khóa tăng dần 1→5→15 phút; **OTP sai cũng tính vào ngưỡng** (VĐ-04).
- **Resend OTP**: chặn phía server 60 giây → HTTP 429; tối đa 3 lần rồi bắt đầu lại (VĐ-07).
- **Session registry** phía server: logout/ hết hạn/ replay đều bị từ chối (BR-08).
- Nhật ký audit đầy đủ, che giấu PII khi xuất file, giới hạn 10.000 bản ghi/lần xuất.

Chi tiết: [`docs/security-bac.md`](docs/security-bac.md).

---

## 12. Xử lý sự cố

| Hiện tượng | Cách xử lý |
|---|---|
| Cổng 8080 đã dùng | Đổi `server.port` hoặc dừng tiến trình đang chiếm cổng |
| Không đăng nhập được | Kiểm tra backend đã chạy; xem log audit; tài khoản có thể đang bị khóa tạm |
| Không nhận OTP | Ở `dev`, mã hiển thị trên UI/ log `[SIMULATED-SMS]`; ở S3 cần bật Email OTP |
| Quét QR báo “chỉ mở bằng ứng dụng” | Do camera mặc định của điện thoại không xử lý được liên kết `otpauth://`. Hãy mở **ứng dụng xác thực → Quét mã QR**, hoặc dùng **nhập khóa thủ công** và dán mã bí mật (đã sửa định dạng URI: khoảng trắng thành `%20`) |
| Lỗi kết nối MySQL | Kiểm tra `DB_*` trong `.env`; đảm bảo schema đã tạo |
| `mvn` không có | Dùng `./mvnw` (Maven wrapper đi kèm) |
