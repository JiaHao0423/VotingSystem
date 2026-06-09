import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import { statusLabel, typeLabel } from '@/lib/labels'
import type { ProposalStatus, ProposalType } from '@/lib/types'

export function StatusBadge({ status }: { status: ProposalStatus }) {
  const styles: Record<ProposalStatus, string> = {
    ACTIVE: 'bg-chart-3/15 text-chart-3 border-chart-3/30',
    ENDED: 'bg-muted text-muted-foreground border-border',
    SCHEDULED: 'bg-chart-4/15 text-chart-4 border-chart-4/30',
    DRAFT: 'bg-secondary text-secondary-foreground border-border',
  }
  return (
    <Badge className={cn('font-medium', styles[status])}>
      {status === 'ACTIVE' && (
        <span className="mr-1 inline-block size-1.5 animate-pulse rounded-full bg-chart-3" aria-hidden="true" />
      )}
      {statusLabel(status)}
    </Badge>
  )
}

export function TypeBadge({ type }: { type: ProposalType }) {
  return (
    <Badge
      className={cn(
        'font-medium',
        type === 'EXTRAORDINARY'
          ? 'border-chart-5/30 bg-chart-5/10 text-chart-5'
          : 'border-primary/20 bg-primary/5 text-primary',
      )}
    >
      {typeLabel(type)}
    </Badge>
  )
}
