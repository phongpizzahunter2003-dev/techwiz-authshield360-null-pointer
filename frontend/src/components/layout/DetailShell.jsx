import { AppShell } from './AppShell.jsx'
import { BackButton } from '../ui/BackButton.jsx'

/**
 * Standard wrapper for every drill-down / detail page: provides the page chrome
 * plus a consistent back button so the user can always return to the previous page.
 */
export function DetailShell({ title, subtitle, fallback = '/', actions, children }) {
  return (
    <AppShell title={title} subtitle={subtitle} actions={actions}>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <BackButton fallback={fallback} />
        <span className="text-xs text-ink-400">You can return to the previous page at any time.</span>
      </div>
      {children}
    </AppShell>
  )
}
