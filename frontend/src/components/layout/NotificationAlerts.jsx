import { useEffect, useRef } from 'react'
import { useAuth } from '../../context/AuthContext.jsx'
import { useToast } from '../ui/Toast.jsx'
import { notificationsApi } from '../../api/endpoints.js'

const POLL_MS = 30000

/**
 * Watches the unread-notification count and raises a top-of-screen alert whenever it grows,
 * so students/teachers are told about new activity on any page of the system.
 * Renders nothing; the alert layer lives in ToastProvider.
 */
export function NotificationAlerts() {
  const { isAuthenticated } = useAuth()
  const toast = useToast()
  const seen = useRef(null)

  useEffect(() => {
    if (!isAuthenticated) {
      seen.current = null
      return undefined
    }

    let active = true

    const check = async () => {
      try {
        const [count, list] = await Promise.all([
          notificationsApi.unreadCount(),
          notificationsApi.list({ size: 1 }),
        ])
        if (!active) return
        const unread = count.data?.unread ?? 0
        const newest = (list.data?.items || [])[0]

        if (seen.current === null) {
          // First pass only records the baseline so a page load does not spam alerts.
          seen.current = unread
          return
        }
        if (unread > seen.current && newest) {
          toast.info(`New notification: ${newest.title}`)
        }
        seen.current = unread
      } catch {
        /* never break the page because of the alert poll */
      }
    }

    check()
    const id = setInterval(check, POLL_MS)
    // Timers are throttled in background tabs: re-check the moment the tab becomes visible.
    const onVisible = () => {
      if (document.visibilityState === 'visible') check()
    }
    document.addEventListener('visibilitychange', onVisible)
    window.addEventListener('focus', onVisible)
    return () => {
      active = false
      clearInterval(id)
      document.removeEventListener('visibilitychange', onVisible)
      window.removeEventListener('focus', onVisible)
    }
  }, [isAuthenticated, toast])

  return null
}
