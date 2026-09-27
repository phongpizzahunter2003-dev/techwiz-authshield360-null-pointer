const TONE_MAP = {
  brand: 'bg-brand-100 text-brand-700',
  accent: 'bg-accent-100 text-accent-600',
  sun: 'bg-sun-100 text-sun-600',
  coral: 'bg-coral-100 text-coral-600',
  sky: 'bg-sky-100 text-sky-600',
  neutral: 'bg-surface-muted text-ink-600',
}

export function Badge({ tone = 'neutral', children, icon }) {
  return (
    <span className={`badge ${TONE_MAP[tone] || TONE_MAP.neutral}`}>
      {icon ? <span aria-hidden="true">{icon}</span> : null}
      {children}
    </span>
  )
}

export function statusTone(status) {
  switch (status) {
    case 'PUBLISHED':
    case 'SUCCESS':
    case 'ACTIVE':
    case 'ON_TIME':
      return 'accent'
    case 'CLOSED':
    case 'FAILURE':
    case 'LATE':
    case 'LOCKED':
      return 'coral'
    case 'DRAFT':
    case 'PENDING':
      return 'sun'
    default:
      return 'neutral'
  }
}

export function SubmissionStatusBadge({ status }) {
  if (status === 'ON_TIME') return <Badge tone="accent" icon="⏱">Đúng hạn</Badge>
  if (status === 'LATE') return <Badge tone="coral" icon="🐢">Nộp muộn</Badge>
  return <Badge tone="neutral">{status}</Badge>
}
