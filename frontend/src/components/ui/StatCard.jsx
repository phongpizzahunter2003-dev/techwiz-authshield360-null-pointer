const TONES = {
  brand: 'from-brand-100 to-white text-brand-700',
  accent: 'from-accent-100 to-white text-accent-600',
  sun: 'from-sun-100 to-white text-sun-600',
  coral: 'from-coral-100 to-white text-coral-600',
  sky: 'from-sky-100 to-white text-sky-600',
}

export function StatCard({ label, value, tone = 'brand', hint }) {
  return (
    <div className={`card bg-gradient-to-br ${TONES[tone] || TONES.brand} p-4`}>
      <p className="text-xs font-semibold uppercase tracking-wide opacity-80">{label}</p>
      <p className="mt-1 text-2xl font-extrabold text-ink-900">{value}</p>
      {hint ? <p className="mt-1 text-xs text-ink-400">{hint}</p> : null}
    </div>
  )
}

export function StatGrid({ stats = [] }) {
  if (!stats.length) return null
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
      {stats.map((s) => (
        <StatCard key={s.key} label={s.label} value={s.value} tone={s.tone} />
      ))}
    </div>
  )
}
