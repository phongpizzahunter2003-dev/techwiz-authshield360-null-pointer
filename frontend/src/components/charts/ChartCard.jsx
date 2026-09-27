import { useNavigate } from 'react-router-dom'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

const TONE_HEX = {
  brand: '#6C5CE7',
  accent: '#00D2A8',
  sun: '#FFC93C',
  coral: '#FF6B6B',
  sky: '#4FC3F7',
}

const colorOf = (slice, index) =>
  TONE_HEX[slice?.tone] || ['#6C5CE7', '#00D2A8', '#FFC93C', '#FF6B6B', '#4FC3F7'][index % 5]

const fmt = (v) => new Intl.NumberFormat('vi-VN').format(v ?? 0)

/**
 * Clickable chart. Clicking a bar / slice / point (or a legend chip) drills down
 * into the detail page carried by the slice.
 */
export function ChartCard({ series }) {
  const navigate = useNavigate()
  const slices = series?.slices || []
  const drill = (slice) => {
    if (slice?.drill) navigate(slice.drill)
  }

  const empty = slices.length === 0 || slices.every((s) => !s.value)

  return (
    <section className="card flex flex-col">
      <header className="mb-1 flex items-start justify-between gap-3">
        <div>
          <h3 className="text-sm font-bold text-ink-900">{series.title}</h3>
          <p className="text-[11px] text-ink-400">Đơn vị: {series.valueLabel}</p>
        </div>
        <span className="badge bg-surface-muted text-ink-600">{series.type}</span>
      </header>
      <p className="mb-2 text-xs text-ink-400">{series.description}</p>

      {empty ? (
        <div className="grid h-52 place-items-center text-sm text-ink-400">Chưa có dữ liệu để hiển thị.</div>
      ) : (
        <div className="h-52 w-full">
          <ResponsiveContainer width="100%" height="100%">
            {series.type === 'PIE' ? (
              <PieChart>
                <Pie
                  data={slices}
                  dataKey="value"
                  nameKey="label"
                  innerRadius={45}
                  outerRadius={80}
                  paddingAngle={2}
                  isAnimationActive={false}
                  onClick={(entry) => drill(entry?.payload || entry)}
                >
                  {slices.map((s, i) => (
                    <Cell key={s.key} fill={colorOf(s, i)} cursor="pointer" />
                  ))}
                </Pie>
                <Tooltip formatter={(v, n) => [fmt(v), n]} />
                <Legend verticalAlign="bottom" height={24} />
              </PieChart>
            ) : series.type === 'LINE' ? (
              <LineChart data={slices} margin={{ top: 8, right: 12, left: -18, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#EEF0FA" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} stroke="#9A9AB0" />
                <YAxis tick={{ fontSize: 11 }} stroke="#9A9AB0" allowDecimals={false} />
                <Tooltip formatter={(v) => [fmt(v), series.valueLabel]} />
                <Line
                  type="monotone"
                  dataKey="value"
                  stroke={TONE_HEX.brand}
                  strokeWidth={3}
                  isAnimationActive={false}
                  dot={{ r: 5, cursor: 'pointer' }}
                  activeDot={{ r: 7, cursor: 'pointer' }}
                  onClick={(entry) => drill(entry?.payload || entry)}
                />
              </LineChart>
            ) : (
              <BarChart data={slices} margin={{ top: 8, right: 12, left: -18, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#EEF0FA" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} stroke="#9A9AB0" interval={0} angle={-12} dy={8} />
                <YAxis tick={{ fontSize: 11 }} stroke="#9A9AB0" allowDecimals={false} />
                <Tooltip formatter={(v) => [fmt(v), series.valueLabel]} cursor={{ fill: '#F7F8FF' }} />
                <Bar dataKey="value" radius={[8, 8, 0, 0]} isAnimationActive={false} onClick={(entry) => drill(entry?.payload || entry)}>
                  {slices.map((s, i) => (
                    <Cell key={s.key} fill={colorOf(s, i)} cursor="pointer" />
                  ))}
                </Bar>
              </BarChart>
            )}
          </ResponsiveContainer>
        </div>
      )}

      {/* Keyboard-accessible drill-down (fe-rules.md §9: colour is never the only signal). */}
      {!empty ? (
        <ul className="mt-2 flex flex-wrap gap-1.5">
          {slices.map((s, i) => (
            <li key={s.key}>
              <button
                type="button"
                onClick={() => drill(s)}
                className="chip"
                title={`Xem chi tiết: ${s.label}`}
              >
                <span
                  className="inline-block h-2.5 w-2.5 rounded-full"
                  style={{ backgroundColor: colorOf(s, i) }}
                  aria-hidden="true"
                />
                {s.label}
                <strong className="text-ink-900">{fmt(s.value)}</strong>
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  )
}
