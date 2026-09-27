import { useEffect, useRef } from 'react'

export function Modal({ open, title, onClose, children, footer, size = 'md' }) {
  const panelRef = useRef(null)
  // Keep the latest onClose in a ref so the effect below can be keyed on `open` only.
  // (Putting an inline `onClose` in the dependency array made the effect re-run on every
  // render, which re-focused the dialog container and stole focus from the form fields.)
  const onCloseRef = useRef(onClose)
  onCloseRef.current = onClose

  useEffect(() => {
    if (!open) return undefined
    const onKey = (e) => {
      if (e.key === 'Escape') onCloseRef.current?.()
    }
    document.addEventListener('keydown', onKey)

    // Focus the first real control once, on open — never on subsequent renders.
    // Separate queries keep priority order (a comma-separated selector would return the
    // first match in document order, i.e. the close button in the header).
    const panel = panelRef.current
    if (panel && !panel.contains(document.activeElement)) {
      const firstControl =
        panel.querySelector('input:not([type="hidden"]):not([disabled]), select:not([disabled]), textarea:not([disabled])') ||
        panel.querySelector('button:not([disabled]):not([aria-label="Close"])') ||
        panel
      firstControl.focus()
    }

    return () => document.removeEventListener('keydown', onKey)
  }, [open])

  if (!open) return null
  const width = size === 'lg' ? 'max-w-3xl' : size === 'sm' ? 'max-w-md' : 'max-w-xl'

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-ink-900/40 p-0 sm:items-center sm:p-4"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose?.()
      }}
    >
      <div
        ref={panelRef}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={`animate-pop-in w-full ${width} rounded-t-3xl bg-white p-5 shadow-soft sm:rounded-3xl`}
      >
        <div className="mb-3 flex items-start justify-between gap-4">
          <h2 className="text-lg font-bold text-ink-900">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="rounded-full p-2 text-ink-400 hover:bg-surface-muted hover:text-ink-600"
          >
            ✕
          </button>
        </div>
        <div className="max-h-[70vh] overflow-y-auto">{children}</div>
        {footer ? <div className="mt-5 flex flex-wrap justify-end gap-2">{footer}</div> : null}
      </div>
    </div>
  )
}
