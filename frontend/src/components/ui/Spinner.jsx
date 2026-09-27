export function Spinner({ label = 'Đang tải...', size = 'md' }) {
  const dim = size === 'sm' ? 'h-4 w-4 border-2' : 'h-8 w-8 border-[3px]'
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-10 text-ink-400" role="status">
      <span
        className={`${dim} animate-spin rounded-full border-brand-100 border-t-brand-500`}
        aria-hidden="true"
      />
      <span className="text-sm">{label}</span>
    </div>
  )
}

export function InlineSpinner() {
  return (
    <span
      className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-white/50 border-t-white"
      aria-hidden="true"
    />
  )
}
