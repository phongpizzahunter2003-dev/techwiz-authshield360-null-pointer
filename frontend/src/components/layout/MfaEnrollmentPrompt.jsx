import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import { Modal } from '../ui/Modal.jsx'
import { ROLE_LABEL } from '../../i18n/messages.js'

const DISMISS_KEY = 'mfa_prompt_dismissed'

/**
 * UC-09: after the first successful password login, a user whose role requires MFA
 * but who has not enrolled yet is asked to enrol. Each portal role enrols MFA for
 * themselves via the QR code — the administrator never creates it for them
 * (admin only enables MFA in UC-08 and may reset it in UC-16).
 */
export function MfaEnrollmentPrompt() {
  const { session } = useAuth()
  const navigate = useNavigate()
  const [dismissed, setDismissed] = useState(
    () => typeof sessionStorage !== 'undefined' && sessionStorage.getItem(DISMISS_KEY) === '1',
  )

  const required = Boolean(session && session.mfaEnabled && !session.mfaEnrolled)
  if (!required || dismissed) return null

  const dismiss = () => {
    sessionStorage.setItem(DISMISS_KEY, '1')
    setDismissed(true)
  }

  return (
    <Modal
      open
      title="Thiết lập xác thực hai yếu tố (UC-09)"
      onClose={dismiss}
      footer={
        <>
          <button type="button" className="btn-ghost" onClick={dismiss}>
            Để sau
          </button>
          <button
            type="button"
            className="btn-primary"
            onClick={() => {
              dismiss()
              navigate('/profile')
            }}
          >
            🔐 Thiết lập ngay
          </button>
        </>
      }
    >
      <p className="text-sm text-ink-600">
        Hệ thống đã <strong>bật xác thực hai yếu tố</strong> cho vai trò{' '}
        <strong>{ROLE_LABEL[session.role] || session.role}</strong>, nhưng tài khoản của bạn chưa đăng ký yếu tố thứ hai.
      </p>
      <p className="mt-3 text-sm text-ink-600">
        Theo UC-09, sau lần đăng nhập mật khẩu thành công đầu tiên bạn cần <strong>tự đăng ký</strong> yếu tố MFA: quét mã QR
        bằng ứng dụng xác thực (Google Authenticator, FreeOTP) và nhập mã 6 số để xác nhận.
      </p>
      <p className="mt-3 rounded-xl bg-surface-soft px-3 py-2 text-xs text-ink-400">
        Mỗi người dùng tự thiết lập MFA cho chính mình. Quản trị viên chỉ cấu hình chế độ xác thực (UC-08) và có thể đặt lại
        MFA khi cần (UC-16) — không tạo QR thay người dùng.
      </p>
    </Modal>
  )
}
