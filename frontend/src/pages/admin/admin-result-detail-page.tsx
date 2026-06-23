import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, CheckCircle2, XCircle, Scale, Users, Vote } from 'lucide-react'
import { toast } from 'sonner'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { StatusBadge, TypeBadge } from '@/components/status-badge'
import { ResultBars } from '@/components/result-bars'
import { ResultPieChart } from '@/components/result-pie-chart'
import { adminApi } from '@/lib/admin-api'
import { useAdminAuth } from '@/context/admin-auth-context'
import { pct, formatDateTime, thresholdBaseLabel, thresholdLabel } from '@/lib/labels'
import { usePolling } from '@/hooks/use-polling'
import type { AdminResultDetail } from '@/lib/admin-types'

export function AdminResultDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { community } = useAdminAuth()
  const [data, setData] = useState<AdminResultDetail | null>(null)

  const reload = useCallback(async () => {
    if (!community || !id) return
    try {
      const detail = await adminApi.getProposalResultDetail(community.id, Number(id))
      setData(detail)
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '載入失敗')
    }
  }, [community, id])

  useEffect(() => {
    reload()
  }, [community, id])

  const isLive = data?.summary.status === 'ACTIVE'
  usePolling(() => reload(), 5000, isLive)

  if (!data) {
    return <p className="text-sm text-muted-foreground">載入中…</p>
  }

  const r = data.summary
  const hasVotes = r.totalVotedHouseholds > 0
  const thresholdText = `${thresholdLabel(r.passThresholdNumerator, r.passThresholdDenominator)}（${thresholdBaseLabel(r.thresholdBase)}）`

  return (
    <div className="mx-auto w-full max-w-5xl">
      <Link
        to="/admin/results"
        className="mb-2 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" aria-hidden="true" />
        返回結果列表
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <TypeBadge type={r.type} />
            <StatusBadge status={r.status} />
            {isLive && (
              <Badge className="border-chart-3/30 bg-chart-3/10 text-chart-3">即時更新中</Badge>
            )}
          </div>
          <p className="mt-2 text-xs text-muted-foreground">{r.proposalNumber}</p>
          <h1 className="text-pretty text-2xl font-bold leading-snug text-foreground">{r.title}</h1>
        </div>
      </div>

      {r.status === 'ACTIVE' ? (
        <div className="mt-4 flex items-center gap-3 rounded-lg bg-primary/10 p-4 text-primary">
          <Vote className="size-7 shrink-0" aria-hidden="true" />
          <div>
            <p className="text-lg font-bold">投票進行中</p>
            <p className="text-sm opacity-80">
              投票結束後將依門檻判定是否通過（同意票之人數與權數比例均須達 {thresholdText}）
            </p>
          </div>
        </div>
      ) : (
        hasVotes && (
          <div
            className={`mt-4 flex items-center gap-3 rounded-lg p-4 ${
              r.passed ? 'bg-chart-3/10 text-chart-3' : 'bg-chart-5/10 text-chart-5'
            }`}
          >
            {r.passed ? (
              <CheckCircle2 className="size-7 shrink-0" aria-hidden="true" />
            ) : (
              <XCircle className="size-7 shrink-0" aria-hidden="true" />
            )}
            <div>
              <p className="text-lg font-bold">{r.passed ? '本提案已通過' : '本提案未通過'}</p>
              <p className="text-sm opacity-80">
                通過門檻：同意票之人數與權數比例均須達 {thresholdText}
              </p>
            </div>
          </div>
        )
      )}

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">即時票數圓餅圖</CardTitle>
          </CardHeader>
          <CardContent>
            {hasVotes ? (
              <ResultPieChart options={r.options} />
            ) : (
              <p className="py-8 text-center text-sm text-muted-foreground">本提案尚未開始投票</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">各選項得票統計</CardTitle>
          </CardHeader>
          <CardContent>
            {hasVotes ? (
              <ResultBars options={r.options} />
            ) : (
              <p className="py-8 text-center text-sm text-muted-foreground">本提案尚未開始投票</p>
            )}
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Scale className="size-4 text-primary" aria-hidden="true" />
              通過門檻
            </CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">同意比例（人數／{thresholdBaseLabel(r.thresholdBase)}）</span>
              <span className="font-medium">{pct(r.agreeHouseholdRatio)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">同意比例（權數／{thresholdBaseLabel(r.thresholdBase)}）</span>
              <span className="font-medium">{pct(r.agreeWeightRatio)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">門檻要求</span>
              <span className="font-medium">{thresholdText}</span>
            </div>
            <Separator />
            <div className="grid grid-cols-2 gap-3 text-center sm:grid-cols-4">
              <div className="rounded-lg bg-secondary p-3">
                <p className="text-xs text-muted-foreground">已投票戶數</p>
                <p className="text-xl font-black">{r.totalVotedHouseholds}</p>
              </div>
              <div className="rounded-lg bg-secondary p-3">
                <p className="text-xs text-muted-foreground">門檻基準戶數</p>
                <p className="text-xl font-black">{r.thresholdHouseholds}</p>
              </div>
              <div className="rounded-lg bg-secondary p-3">
                <p className="text-xs text-muted-foreground">門檻基準權數</p>
                <p className="text-xl font-black">{Number(r.thresholdWeight).toLocaleString()}</p>
                <p className="text-xs text-muted-foreground">坪</p>
              </div>
              <div className="rounded-lg bg-secondary p-3">
                <p className="text-xs text-muted-foreground">全社區戶數</p>
                <p className="text-xl font-black">{r.totalCommunityHouseholds}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card className="mt-4">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Users className="size-4 text-primary" aria-hidden="true" />
            投票所有權人列表
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>所有權人</TableHead>
                <TableHead>戶別</TableHead>
                <TableHead>投票選項</TableHead>
                <TableHead className="text-right">投票時間</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.voters.map((v) => (
                <TableRow key={v.ownerId}>
                  <TableCell className="font-medium">{v.ownerName}</TableCell>
                  <TableCell className="text-muted-foreground">{v.unitShortName}</TableCell>
                  <TableCell>
                    <Badge className="border-primary/30 bg-primary/10 text-primary">{v.choiceLabel}</Badge>
                  </TableCell>
                  <TableCell className="text-right text-muted-foreground">
                    {formatDateTime(v.votedAt)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {data.voters.length === 0 && (
            <p className="py-10 text-center text-sm text-muted-foreground">尚無投票紀錄</p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
