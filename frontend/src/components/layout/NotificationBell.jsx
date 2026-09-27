import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { notificationsApi } from '../../api/endpoints.js'
import { useToast } from '../ui/Toast.jsx'
import { InlineSpinner } from '../ui/Spinner.jsx'

const POLL_MS = 30000

export function timeAgo(value) {
  if (!value) return ''
  const diff = Date.now() - new Date(value).getTime()
  const min = Math.floor(diff / 60000)
  if (min < 1) return 'just now'
  if (min < 60) return `${min}m ago`
  const hours = Math.floor(min / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}d ago`
  return new Date(value).toLocaleDateString('en-GB')
}

const TYPE_ICON = {
  ASSIGNMENT_PUBLISHED: '📚',
  ASSIGNMENT_UPDATED: '✏️',
  ASSIGNMENT_CLOSED: '🔒',
  SUBMISSION_RECEIVED: '📥',
  SUBMISSION_LATE: '🐢',
  SUBMISSION_GRADED: '🎯',
  CLASSROOM_ENROLLED: '🎒',
  ACCOUNT_READY: '👋',
}

/**
 * Top-bar notification bell: unread badge, recent list, mark-one / mark-all read.
 * Polls the unread count so the badge stays current without a socket.
 */
export function NotificationBell() {
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState([])
  const [unread, setUnread] = useState(0)
  const [busy, setBusy] = useState(false)
  const panelRef = useRef(null)
  const navigate = useNavigate()
  const toast = useToast()

  const load = useCallback(async () => {
    try {
      const [list, count] = await Promise.all([
        notificationsApi.list({ size: 6 }),
        notificationsApi.unreadCount(),
      ])
      setItems(list.data?.items || [])
      setUnread(count.data?.unread || 0)
    } catch {
      /* the bell must never break the page */
    }
  }, [])

  useEffect(() => {
    load()
    const id = setInterval(load, POLL_MS)
    return () => clearInterval(id)
  }, [load])

  useEffect(() => {
    if (!open) return undefined
    const onDocClick = (e) => {
      if (panelRef.current && !panelRef.current.contains(e.target)) setOpen(false)
    }
    const onKey = (e) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onDocClick)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDocClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  const openItem = async (n) => {
    setOpen(false)
    if (!n.read) {
      try {
        await notificationsApi.markRead(n.id)
        await load()
      } catch {
        /* ignore, navigation still happens */
      }
    }
    if (n.targetUrl) navigate(n.targetUrl)
  }

  const markAll = async () => {
    setBusy(true)
    try {
      await notificationsApi.markAllRead()
      await load()
      toast.success('All notifications marked as read.')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="relative" ref={panelRef}>
      <button
        type="button"
        id="btn-notifications"
        className="btn-ghost relative !px-3"
        aria-label={`Notifications${unread ? ` (${unread} unread)` : ''}`}
        aria-expanded={open}
        onClick={() => {
          setOpen((v) => !v)
          if (!open) load()
        }}
      >
        <span aria-hidden="true">🔔</span>
        <span className="hidden sm:inline">Alerts</span>
        {unread > 0 ? (
          <span className="absolute -right-0.5 -top-0.5 grid h-5 min-w-[1.25rem] place-items-center rounded-full bg-coral-400 px-1 text-[11px] font-bold text-white">
            {unread > 99 ? '99+' : unread}
          </span>
        ) : null}
      </button>

      {open ? (
        <div className="animate-pop-in absolute right-0 z-50 mt-2 w-[22rem] max-w-[calc(100vw-2rem)] overflow-hidden rounded-2xl border border-brand-50 bg-white shadow-soft">
          <div className="flex items-center justify-between border-b border-brand-50 px-4 py-2.5">
            <p className="text-sm font-bold text-ink-900">Notifications</p>
            <button
              type="button"
              className="text-xs font-semibold text-brand-600 hover:underline disabled:opacity-50"
              onClick={markAll}
              disabled={busy || unread === 0}
            >
              {busy ? <InlineSpinner /> : null} Mark all as read
            </button>
          </div>

          <div className="max-h-80 overflow-y-auto">
            {items.length === 0 ? (
              <p className="px-4 py-8 text-center text-sm text-ink-400">No notifications yet.</p>
            ) : (
              <ul>
                {items.map((n) => (
                  <li key={n.id}>
                    <button
                      type="button"
                      onClick={() => openItem(n)}
                      className={`flex w-full items-start gap-3 px-4 py-3 text-left transition hover:bg-surface-soft ${
                        n.read ? '' : 'bg-brand-50/50'
                      }`}
                    >
                      <span className="text-lg" aria-hidden="true">
                        {TYPE_ICON[n.type] || '🔔'}
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="flex items-center gap-2">
                          <span className="truncate text-sm font-semibold text-ink-900">{n.title}</span>
                          {!n.read ? <span className="h-2 w-2 shrink-0 rounded-full bg-brand-500" aria-label="Unread" /> : null}
                        </span>
                        <span className="mt-0.5 block text-xs text-ink-600">{n.message}</span>
                        <span className="mt-1 block text-[11px] text-ink-400">{timeAgo(n.createdAt)}</span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="border-t border-brand-50 px-4 py-2.5 text-center">
            <button
              type="button"
              className="text-xs font-semibold text-brand-600 hover:underline"
              onClick={() => {
                setOpen(false)
                navigate('/notifications')
              }}
            >
              View all notifications →
            </button>
          </div>
        </div>
      ) : null}
    </div>
  )
}
