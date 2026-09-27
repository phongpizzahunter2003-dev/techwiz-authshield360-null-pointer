export function EmptyState({ icon = '📭', title = 'Chưa có dữ liệu', description, action }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-brand-100 bg-white px-6 py-12 text-center">
      <span className="text-4xl" aria-hidden="true">
        {icon}
      </span>
      <h3 className="mt-3 text-base font-bold text-ink-900">{title}</h3>
      {description ? <p className="mt-1 max-w-md text-sm text-ink-400">{description}</p> : null}
      {action ? <div className="mt-4">{action}</div> : null}
    </div>
  )
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="rounded-2xl border border-coral-100 bg-coral-100/40 px-6 py-8 text-center">
      <p className="text-3xl" aria-hidden="true">
        🚧
      </p>
      <p className="mt-2 text-sm font-semibold text-ink-900">{message || 'Không thể tải dữ liệu.'}</p>
      {onRetry ? (
        <button type="button" className="btn-ghost mt-4" onClick={onRetry}>
          Thử lại
        </button>
      ) : null}
    </div>
  )
}
