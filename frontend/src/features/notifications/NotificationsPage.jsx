import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { BackButton } from '../../components/ui/BackButton.jsx'
import { Badge } from '../../components/ui/Badge.jsx'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner, InlineSpinner } from '../../components/ui/Spinner.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { notificationsApi } from '../../api/endpoints.js'
import { timeAgo } from '../../components/layout/NotificationBell.jsx'

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

const TYPE_LABEL = {
  ASSIGNMENT_PUBLISHED: 'New assignment',
  ASSIGNMENT_UPDATED: 'Assignment updated',
  ASSIGNMENT_CLOSED: 'Assignment closed',
  SUBMISSION_RECEIVED: 'Submission received',
  SUBMISSION_LATE: 'Late submission',
  SUBMISSION_GRADED: 'Graded',
  CLASSROOM_ENROLLED: 'Class enrolment',
  ACCOUNT_READY: 'Welcome',
}

const FILTERS = [
  { key: 'unread', label: 'Unread' },
  { key: 'all', label: 'All' },
]

export function NotificationsPage() {
  const [filter, setFilter] = useState('unread')
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [unread, setUnread] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const [markingAll, setMarkingAll] = useState(false)
  const navigate = useNavigate()
  const toast = useToast()

  const load = useCallback(
    async (nextFilter = filter, nextPage = page) => {
      setLoading(true)
      setError(null)
      try {
        const [list, count] = await Promise.all([
          notificationsApi.list({ unread: nextFilter === 'unread', page: nextPage, size: 20 }),
          notificationsApi.unreadCount(),
        ])
        setData(list.data)
        setUnread(count.data?.unread || 0)
        setFilter(nextFilter)
        setPage(nextPage)
      } catch (err) {
        setError(err)
      } finally {
        setLoading(false)
      }
    },
    [filter, page],
  )

  useEffect(() => {
    load('unread', 0)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const openItem = async (n) => {
    setBusyId(n.id)
    try {
      if (!n.read) await notificationsApi.markRead(n.id)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setBusyId(null)
    }
    if (n.targetUrl) navigate(n.targetUrl)
    else await load()
  }

  const markAll = async () => {
    setMarkingAll(true)
    try {
      await notificationsApi.markAllRead()
      toast.success('All notifications marked as read.')
      await load(filter, 0)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setMarkingAll(false)
    }
  }

  const dismiss = async (n) => {
    setBusyId(n.id)
    try {
      await notificationsApi.remove(n.id)
      toast.success('Notification dismissed.')
      await load(filter, 0)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setBusyId(null)
    }
  }

  const items = data?.items || []
  const totalPages = data?.totalPages || 0

  return (
    <AppShell
      title="Notifications"
      subtitle="Assignment, submission and grading updates for your account"
      actions={
        <button type="button" className="btn-ghost" onClick={markAll} disabled={markingAll || unread === 0}>
          {markingAll ? <InlineSpinner /> : '✓'} Mark all as read
        </button>
      }
    >
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <BackButton fallback="/" label="Back" />
        {FILTERS.map((f) => (
          <button
            key={f.key}
            type="button"
            onClick={() => load(f.key, 0)}
            className={`chip ${filter === f.key ? 'border-brand-400 bg-brand-50 text-brand-700' : ''}`}
          >
            {f.label}
            {f.key === 'unread' && unread > 0 ? <strong>{unread}</strong> : null}
          </button>
        ))}
      </div>

      {loading ? <Spinner label="Loading notifications..." /> : null}
      {error ? <ErrorState message={error.message} onRetry={() => load()} /> : null}

      {!loading && !error && items.length === 0 ? (
        <EmptyState
          icon="🔔"
          title={filter === 'unread' ? 'No unread notifications' : 'No notifications yet'}
          description="You will be notified here when assignments, submissions or grades change."
        />
      ) : null}

      {items.length > 0 ? (
        <ul className="space-y-2">
          {items.map((n) => (
            <li
              key={n.id}
              className={`card flex flex-wrap items-start gap-3 !p-4 ${n.read ? '' : 'border-brand-200 bg-brand-50/40'}`}
            >
              <span className="text-2xl" aria-hidden="true">
                {TYPE_ICON[n.type] || '🔔'}
              </span>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-bold text-ink-900">{n.title}</p>
                  <Badge tone={n.read ? 'neutral' : 'brand'}>{n.read ? 'Read' : 'Unread'}</Badge>
                  {TYPE_LABEL[n.type] ? <span className="text-[11px] text-ink-400">{TYPE_LABEL[n.type]}</span> : null}
                </div>
                <p className="mt-1 text-sm text-ink-600">{n.message}</p>
                <p className="mt-1 text-[11px] text-ink-400">{timeAgo(n.createdAt)}</p>
              </div>
              <div className="table-actions">
                {!n.read ? (
                  <button
                    type="button"
                    className="btn-xs btn-ghost"
                    onClick={() => openItem(n)}
                    disabled={busyId === n.id}
                  >
                    {busyId === n.id ? <InlineSpinner /> : '✓'} Mark read
                  </button>
                ) : n.targetUrl ? (
                  <button type="button" className="btn-xs btn-ghost" onClick={() => navigate(n.targetUrl)}>
                    Open
                  </button>
                ) : null}
                <button
                  type="button"
                  className="btn-xs btn-danger"
                  onClick={() => dismiss(n)}
                  disabled={busyId === n.id}
                >
                  Dismiss
                </button>
              </div>
            </li>
          ))}
        </ul>
      ) : null}

      {data && totalPages > 1 ? (
        <div className="mt-4 flex items-center justify-between text-sm text-ink-400">
          <span>
            {data.totalElements} notification(s) · Page {data.page + 1}/{totalPages}
          </span>
          <div className="flex gap-2">
            <button
              type="button"
              className="btn-xs btn-ghost"
              disabled={data.page <= 0}
              onClick={() => load(filter, data.page - 1)}
            >
              ← Previous
            </button>
            <button
              type="button"
              className="btn-xs btn-ghost"
              disabled={data.page + 1 >= totalPages}
              onClick={() => load(filter, data.page + 1)}
            >
              Next →
            </button>
          </div>
        </div>
      ) : null}
    </AppShell>
  )
}
