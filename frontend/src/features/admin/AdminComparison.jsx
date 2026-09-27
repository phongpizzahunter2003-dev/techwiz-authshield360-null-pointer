import { Link } from 'react-router-dom'
import { AppShell } from '../../components/layout/AppShell.jsx'
import { ErrorState } from '../../components/ui/EmptyState.jsx'
import { Spinner } from '../../components/ui/Spinner.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { dashboardApi } from '../../api/endpoints.js'
import { formatDateTime } from '../../utils/format.js'

const MODE_TONE = { S1: 'coral', S2: 'sun', S3: 'accent' }

export function AdminComparison() {
  const { data, loading, error, reload } = useAsync(() => dashboardApi.comparison(), [])
  const comparison = data?.data
  const modes = comparison?.modes || []

  return (
    <AppShell title="Comparing the 3 authentication modes" subtitle="Security and user experience across S1 / S2 / S3 (UC-15)">
      {loading ? <Spinner /> : null}
      {error ? <ErrorState message={error.message} onRetry={reload} /> : null}

      {comparison ? (
        <div className="space-y-6">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            {modes.map((m) => (
              <Link
                key={m.mode}
                to={`/admin/modes/${m.mode}`}
                className="card transition hover:shadow-glow"
                title={`View mode ${m.mode} details`}
              >
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-extrabold text-ink-900">{m.mode}</h2>
                  <span
                    className={`badge ${
                      MODE_TONE[m.mode] === 'accent'
                        ? 'bg-accent-100 text-accent-600'
                        : MODE_TONE[m.mode] === 'sun'
                          ? 'bg-sun-100 text-sun-600'
                          : 'bg-coral-100 text-coral-600'
                    }`}
                  >
                    {m.name}
                  </span>
                </div>
                <dl className="mt-4 space-y-3 text-sm">
                  <div>
                    <dt className="text-xs font-bold uppercase text-ink-400">Security level</dt>
                    <dd className="text-ink-900">{m.security}</dd>
                  </div>
                  <div>
                    <dt className="text-xs font-bold uppercase text-ink-400">Usability</dt>
                    <dd className="text-ink-900">{m.usability}</dd>
                  </div>
                  <div>
                    <dt className="text-xs font-bold uppercase text-ink-400">Risk reduced</dt>
                    <dd className="text-ink-900">{m.riskReduced}</dd>
                  </div>
                </dl>
                <div className="mt-4 grid grid-cols-3 gap-2 text-center">
                  <div className="rounded-xl bg-accent-100 p-2">
                    <p className="text-lg font-extrabold text-accent-600">{m.loginSuccesses}</p>
                    <p className="text-[11px] text-ink-600">Success</p>
                  </div>
                  <div className="rounded-xl bg-coral-100 p-2">
                    <p className="text-lg font-extrabold text-coral-600">{m.loginFailures}</p>
                    <p className="text-[11px] text-ink-600">Failures</p>
                  </div>
                  <div className="rounded-xl bg-sun-100 p-2">
                    <p className="text-lg font-extrabold text-sun-600">{m.otpFailures}</p>
                    <p className="text-[11px] text-ink-600">OTP errors</p>
                  </div>
                </div>
                <p className="mt-3 text-sm font-semibold text-brand-600">View mode {m.mode} details →</p>
              </Link>
            ))}
          </div>

          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Criterion</th>
                  {modes.map((m) => (
                    <th key={m.mode}>{m.mode}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td className="font-semibold text-ink-900">Security level</td>
                  {modes.map((m) => (
                    <td key={m.mode}>{m.security}</td>
                  ))}
                </tr>
                <tr>
                  <td className="font-semibold text-ink-900">Usability</td>
                  {modes.map((m) => (
                    <td key={m.mode}>{m.usability}</td>
                  ))}
                </tr>
                <tr>
                  <td className="font-semibold text-ink-900">Risk reduced</td>
                  {modes.map((m) => (
                    <td key={m.mode}>{m.riskReduced}</td>
                  ))}
                </tr>
                <tr>
                  <td className="font-semibold text-ink-900">Successful sign-ins</td>
                  {modes.map((m) => (
                    <td key={m.mode}>
                      <strong>{m.loginSuccesses}</strong>
                    </td>
                  ))}
                </tr>
                <tr>
                  <td className="font-semibold text-ink-900">Failed sign-ins</td>
                  {modes.map((m) => (
                    <td key={m.mode}>
                      <strong>{m.loginFailures}</strong>
                    </td>
                  ))}
                </tr>
                <tr>
                  <td className="font-semibold text-ink-900">OTP errors</td>
                  {modes.map((m) => (
                    <td key={m.mode}>
                      <strong>{m.otpFailures}</strong>
                    </td>
                  ))}
                </tr>
              </tbody>
            </table>
          </div>

          <p className="text-xs text-ink-400">
            {comparison.note} · Aggregated at {formatDateTime(comparison.generatedAt)}.
          </p>
        </div>
      ) : null}
    </AppShell>
  )
}
