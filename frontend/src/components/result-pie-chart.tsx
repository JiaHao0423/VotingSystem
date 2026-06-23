import type { VoteOptionResult } from '@/lib/types'

const CHART_COLORS = [
  'var(--chart-3)',
  'var(--chart-5)',
  'var(--chart-1)',
  'var(--chart-2)',
  'var(--chart-4)',
  'var(--primary)',
]

export function ResultPieChart({ options }: { options: VoteOptionResult[] }) {
  const total = options.reduce((sum, option) => sum + option.votes, 0)
  if (total === 0) {
    return <p className="py-8 text-center text-sm text-muted-foreground">尚無投票資料</p>
  }

  let cumulative = 0
  const slices = options
    .filter((option) => option.votes > 0)
    .map((option, index) => {
      const start = cumulative
      cumulative += option.voteRatio
      return {
        ...option,
        start,
        end: cumulative,
        color: CHART_COLORS[index % CHART_COLORS.length],
      }
    })

  const gradient = slices
    .map((slice) => `${slice.color} ${slice.start * 100}% ${slice.end * 100}%`)
    .join(', ')

  return (
    <div className="flex flex-col items-center gap-4 sm:flex-row sm:items-start">
      <div
        className="size-44 shrink-0 rounded-full shadow-inner"
        style={{ background: `conic-gradient(${gradient})` }}
        role="img"
        aria-label="投票結果圓餅圖"
      />
      <ul className="flex w-full flex-col gap-2">
        {options.map((option, index) => (
          <li key={option.choiceKey} className="flex items-center justify-between gap-3 text-sm">
            <div className="flex min-w-0 items-center gap-2">
              <span
                className="size-3 shrink-0 rounded-full"
                style={{ backgroundColor: CHART_COLORS[index % CHART_COLORS.length] }}
              />
              <span className="truncate font-medium text-foreground">{option.label}</span>
            </div>
            <span className="shrink-0 tabular-nums text-muted-foreground">
              {option.votes} 票 · {(option.voteRatio * 100).toFixed(1)}%
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
