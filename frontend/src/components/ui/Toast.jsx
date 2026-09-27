import { createContext, useCallback, useContext, useMemo, useState } from 'react'

const ToastContext = createContext(null)

let seq = 0

/** Never cover the whole screen with alerts. */
const MAX_VISIBLE = 4

const TONES = {
  success: { ring: 'border-accent-400', icon: '✅', text: 'text-ink-900' },
  error: { ring: 'border-coral-400', icon: '⚠️', text: 'text-ink-900' },
  info: { ring: 'border-sky-400', icon: 'ℹ️', text: 'text-ink-900' },
  warning: { ring: 'border-sun-400', icon: '🔔', text: 'text-ink-900' },
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const remove = useCallback((id) => setToasts((list) => list.filter((t) => t.id !== id)), [])

  const push = useCallback(
    (message, tone = 'info', timeout = 4200) => {
      const id = ++seq
      setToasts((list) => [...list, { id, message, tone }])
      if (timeout > 0) window.setTimeout(() => remove(id), timeout)
      return id
    },
    [remove],
  )

  const value = useMemo(
    () => ({
      push,
      success: (m) => push(m, 'success'),
      error: (m) => push(m, 'error', 6000),
      info: (m) => push(m, 'info'),
      warning: (m) => push(m, 'warning', 5500),
      remove,
    }),
    [push, remove],
  )

  return (
    <ToastContext.Provider value={value}>
      {children}
      {/*
        Alert layer: pinned to the very top of the viewport, above the app header, the mobile
        drawer and any modal (z-[9999]). The container ignores pointer events so the header
        buttons underneath stay clickable; each alert re-enables them for itself.
      */}
      <div
        className="pointer-events-none fixed inset-x-0 top-0 z-[9999] flex flex-col items-center gap-2 p-3 sm:p-4"
        role="status"
        aria-live="polite"
      >
        {toasts.slice(0, MAX_VISIBLE).map((toast) => {
          const tone = TONES[toast.tone] || TONES.info
          return (
            <div
              key={toast.id}
              className={`animate-slide-down pointer-events-none flex w-full max-w-md items-start gap-3 rounded-2xl border-l-4 ${tone.ring} bg-white px-4 py-3 shadow-soft`}
            >
              <span aria-hidden="true">{tone.icon}</span>
              <p className={`flex-1 text-sm ${tone.text}`}>{toast.message}</p>
              <button
                type="button"
                onClick={() => remove(toast.id)}
                className="pointer-events-auto text-ink-400 hover:text-ink-600"
                aria-label="Close notification"
              >
                ✕
              </button>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used inside ToastProvider')
  return ctx
}
