import { useCallback, useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { ArrowLeft, CheckCircle2, XCircle, Scale, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { SiteHeader } from '@/components/site-header'
import { StatusBadge, TypeBadge } from '@/components/status-badge'
import { ResultBars } from '@/components/result-bars'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { api } from '@/lib/api'
import { pct } from '@/lib/labels'
import { usePolling } from '@/hooks/use-polling'
import type { ProposalResult } from '@/lib/types'

export function ResultPage() {
  const { id } = useParams<{ id: string }>()
  const proposalId = Number(id)
  const [searchParams] = useSearchParams()
  const voted = searchParams.get('voted')
  const [result, setResult] = useState<ProposalResult | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(
    async (silent = false) => {
      if (!proposalId) return
      try {
        const data = await api.getResults(proposalId)
        setResult(data)
      } catch (err) {
        if (!silent) {
          toast.error(err instanceof Error ? err.message : '無法載入結果')
        }
      } finally {
        if (!silent) setLoading(false)
      }
    },
    [proposalId],
  )

  useEffect(() => {
    void refresh()
  }, [refresh])

  const isLive = result?.status === 'ACTIVE'
  usePolling(() => refresh(true), 5000, isLive)

  if (loading || !result) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-muted-foreground">
        載入中…
      </div>
    )
  }

  const hasVotes = result.totalVotedHouseholds > 0

  return (
    <div className="min-h-screen bg-background pb-10">
      <SiteHeader subtitle="投票結果" />
      <main className="mx-auto w-full max-w-md px-4 py-5">
        <Link
          to="/proposals"
          className="mb-2 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          返回提案列表
        </Link>

        {voted === '1' && (
          <div className="mb-4 flex items-center gap-3 rounded-lg border border-chart-3/30 bg-chart-3/10 p-3">
            <CheckCircle2 className="size-5 shrink-0 text-chart-3" aria-hidden="true" />
            <div>
              <p className="text-sm font-bold text-foreground">投票成功</p>
              <p className="text-xs text-muted-foreground">您的選擇已記錄，感謝您的參與。</p>
            </div>
          </div>
        )}

        <Card>
          <CardContent className="p-4">
            <div className="flex flex-wrap items-center gap-2">
              <TypeBadge type={result.type} />
              <StatusBadge status={result.status} />
              {result.status === 'ENDED' && hasVotes && (
                <span
                  className={`ml-auto rounded-full px-2 py-0.5 text-xs font-medium ${
                    result.passed ? 'bg-chart-3/15 text-chart-3' : 'bg-chart-5/15 text-chart-5'
                  }`}
                >
                  {result.passed ? '已通過' : '未通過'}
                </span>
              )}
            </div>
            <p className="mt-2 text-xs font-medium text-muted-foreground">{result.proposalNumber}</p>
            <h1 className="mt-0.5 text-pretty text-lg font-bold leading-snug text-foreground">{result.title}</h1>
          </CardContent>
        </Card>

        <Card className="mt-4">
          <CardHeader className="flex-row items-center justify-between">
            <CardTitle className="text-base">即時投票結果</CardTitle>
            {isLive && (
              <span className="inline-flex items-center gap-1 text-xs text-chart-3">
                <RefreshCw className="size-3.5 animate-spin" aria-hidden="true" /> 即時更新
              </span>
            )}
          </CardHeader>
          <CardContent>
            {hasVotes ? (
              <ResultBars options={result.options} />
            ) : (
              <p className="py-6 text-center text-sm text-muted-foreground">本提案尚未開始投票</p>
            )}
          </CardContent>
        </Card>

        {hasVotes && (
          <Card className="mt-4">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <Scale className="size-4 text-primary" aria-hidden="true" />
                法規門檻計算
              </CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-lg bg-secondary p-3">
                  <p className="text-xs text-muted-foreground">已投票戶數</p>
                  <p className="mt-1 text-xl font-black text-foreground">{result.totalVotedHouseholds}</p>
                  <p className="text-xs text-muted-foreground">/ 全社區 {result.totalCommunityHouseholds} 戶</p>
                </div>
                <div className="rounded-lg bg-secondary p-3">
                  <p className="text-xs text-muted-foreground">同意表決權數</p>
                  <p className="mt-1 text-xl font-black text-foreground">
                    {Number(
                      result.options.find((o) => o.choice === 'AGREE')?.weight ?? 0,
                    ).toLocaleString()}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    / 全社區 {Number(result.totalCommunityWeight).toLocaleString()} 坪
                  </p>
                </div>
              </div>

              <Separator />

              <div className="flex flex-col gap-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">同意比例（人數／全社區）</span>
                  <span className="font-medium text-foreground">{pct(result.agreeHouseholdRatio)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">同意比例（區分所有權／全社區）</span>
                  <span className="font-medium text-foreground">{pct(result.agreeWeightRatio)}</span>
                </div>
              </div>

              <div
                className={`flex items-center gap-3 rounded-lg p-3 ${
                  result.passed ? 'bg-chart-3/10 text-chart-3' : 'bg-chart-5/10 text-chart-5'
                }`}
              >
                {result.passed ? (
                  <CheckCircle2 className="size-6 shrink-0" aria-hidden="true" />
                ) : (
                  <XCircle className="size-6 shrink-0" aria-hidden="true" />
                )}
                <div>
                  <p className="text-sm font-bold">{result.passed ? '本提案已通過' : '本提案未通過'}</p>
                  <p className="text-xs opacity-80">
                    依《公寓大廈管理條例》需同意人數與區分所有權比例均超過全社區 50%
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </main>
    </div>
  )
}
