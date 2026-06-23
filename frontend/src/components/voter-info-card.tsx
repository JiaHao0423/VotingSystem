import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { buildingTypeLabel, formatOwnershipRatio } from '@/lib/labels'
import { cn } from '@/lib/utils'
import type { VoterSession } from '@/lib/types'

export function VoterInfoCard({ session }: { session: VoterSession }) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-base">{session.name}</CardTitle>
        <CardDescription>所有權人資訊</CardDescription>
      </CardHeader>
      <CardContent>
        <dl className="flex flex-col gap-2.5 text-sm">
          <InfoRow label="戶別" value={session.unitShortName} />
          <InfoRow label="棟別" value={buildingTypeLabel(session.buildingType)} />
          <InfoRow label="門牌" value={session.fullAddress} valueClassName="text-right" />
          <InfoRow label="坪數" value={`${Number(session.area).toLocaleString()} 坪`} />
          <InfoRow label="區分所有權比例" value={formatOwnershipRatio(session.ownershipRatio)} />
          <InfoRow label="出席狀態" value={session.attended ? '已出席' : '未出席'} />
        </dl>
      </CardContent>
    </Card>
  )
}

function InfoRow({
  label,
  value,
  valueClassName,
}: {
  label: string
  value: string
  valueClassName?: string
}) {
  return (
    <div className="flex justify-between gap-4">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className={cn('font-medium text-foreground', valueClassName)}>{value}</dd>
    </div>
  )
}
