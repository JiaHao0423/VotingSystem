import { cn } from '@/lib/utils'
import type { VoteOptionResult } from '@/lib/types'

const optionColor: Record<string, string> = {
  AGREE: 'bg-chart-3',
  DISAGREE: 'bg-chart-5',
  ABSTAIN: 'bg-muted-foreground',
}

export function ResultBars({ options }: { options: VoteOptionResult[] }) {
  const totalVotes = options.reduce((s, o) => s + o.votes, 0)

  return (
    <div className="flex flex-col gap-4">
      {options.map((opt) => (
        <div key={opt.choiceKey}>
          <div className="mb-1.5 flex items-baseline justify-between">
            <span className="text-sm font-medium text-foreground">{opt.label}</span>
            <span className="text-sm text-muted-foreground">
              <span className="font-bold text-foreground">{opt.votes}</span> 票 ·{' '}
              {(opt.voteRatio * 100).toFixed(1)}%
            </span>
          </div>
          <div className="relative h-3 w-full overflow-hidden rounded-full bg-secondary">
            <div
              className={cn('h-full rounded-full transition-all', optionColor[opt.choiceKey] ?? 'bg-primary')}
              style={{ width: `${opt.voteRatio * 100}%` }}
            />
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            區分所有權比例 {Number(opt.weight).toLocaleString()} 坪
          </p>
        </div>
      ))}
      <p className="text-right text-xs text-muted-foreground">總投票數 {totalVotes} 票</p>
    </div>
  )
}
