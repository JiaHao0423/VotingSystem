import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronRight, Clock, CheckCircle2, Vote, LogOut, XCircle } from 'lucide-react'
import { toast } from 'sonner'
import { SiteHeader } from '@/components/site-header'
import { StatusBadge, TypeBadge } from '@/components/status-badge'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { api } from '@/lib/api'
import { formatDateTime } from '@/lib/labels'
import { useAuth } from '@/context/auth-context'
import { usePolling } from '@/hooks/use-polling'
import { cn } from '@/lib/utils'
import type { ProposalResult, ProposalSummary } from '@/lib/types'

async function loadEndedResults(ended: ProposalSummary[]) {
  const results: Record<number, ProposalResult> = {}
  await Promise.all(
    ended.map(async (p) => {
      try {
        results[p.id] = await api.getResults(p.id)
      } catch {
        // ignore
      }
    }),
  )
  return results
}

export function ProposalsPage() {
  const { session, logout } = useAuth()
  const [proposals, setProposals] = useState<ProposalSummary[]>([])
  const [endedResults, setEndedResults] = useState<Record<number, ProposalResult>>({})
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async (silent = false) => {
    try {
      const list = await api.listProposals()
      setProposals(list)
      const ended = list.filter((p) => p.status === 'ENDED')
      if (ended.length > 0) {
        setEndedResults(await loadEndedResults(ended))
      } else {
        setEndedResults({})
      }
    } catch (err) {
      if (!silent) {
        toast.error(err instanceof Error ? err.message : '無法載入提案')
      }
    } finally {
      if (!silent) setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  usePolling(() => refresh(true))

  const visible = proposals.filter(
    (p) => p.status === 'ACTIVE' || p.status === 'SCHEDULED' || p.status === 'ENDED',
  )
  const activeCount = visible.filter((p) => p.status === 'ACTIVE').length

  async function handleLogout() {
    await logout()
    toast.success('已登出')
  }

  return (
    <div className="min-h-screen bg-background pb-10">
      <SiteHeader subtitle="提案列表" />
      <main className="mx-auto w-full max-w-md px-4 py-6">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h1 className="text-xl font-bold text-foreground">提案投票</h1>
            <p className="text-sm text-muted-foreground">
              您好，{session?.name}（{session?.unitShortName}）
            </p>
          </div>
          <div className="rounded-lg bg-primary/10 px-3 py-2 text-center">
            <p className="text-lg font-black leading-none text-primary">{activeCount}</p>
            <p className="mt-0.5 text-[11px] text-primary/70">進行中</p>
          </div>
        </div>

        {loading ? (
          <p className="mt-8 text-center text-sm text-muted-foreground">載入中…</p>
        ) : visible.length === 0 ? (
          <p className="mt-8 text-center text-sm text-muted-foreground">目前沒有可投票的提案</p>
        ) : (
          <ul className="mt-5 flex flex-col gap-3">
            {visible.map((p) => {
              const canVote = p.status === 'ACTIVE' && !p.hasVoted
              const endedResult = p.status === 'ENDED' ? endedResults[p.id] : null
              const showPassStatus = endedResult && endedResult.totalVotedHouseholds > 0
              return (
                <li key={p.id}>
                  <Card className="overflow-hidden">
                    <CardContent className="p-4">
                      <div className="flex flex-wrap items-center gap-2">
                        <TypeBadge type={p.type} />
                        <StatusBadge status={p.status} />
                        {showPassStatus && (
                          <span
                            className={cn(
                              'ml-auto inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium',
                              endedResult.passed
                                ? 'bg-chart-3/15 text-chart-3'
                                : 'bg-chart-5/15 text-chart-5',
                            )}
                          >
                            {endedResult.passed ? (
                              <>
                                <CheckCircle2 className="size-3.5" aria-hidden="true" /> 已通過
                              </>
                            ) : (
                              <>
                                <XCircle className="size-3.5" aria-hidden="true" /> 未通過
                              </>
                            )}
                          </span>
                        )}
                        {p.hasVoted && !showPassStatus && (
                          <span className="ml-auto inline-flex items-center gap-1 text-xs font-medium text-chart-3">
                            <CheckCircle2 className="size-3.5" aria-hidden="true" /> 已投票
                          </span>
                        )}
                        {p.hasVoted && showPassStatus && (
                          <span className="inline-flex items-center gap-1 text-xs font-medium text-chart-3">
                            <CheckCircle2 className="size-3.5" aria-hidden="true" /> 已投票
                          </span>
                        )}
                      </div>
                      <p className="mt-2 text-xs font-medium text-muted-foreground">{p.proposalNumber}</p>
                      <h2 className="mt-0.5 text-pretty font-bold leading-snug text-foreground">{p.title}</h2>
                      <p className="mt-2 line-clamp-2 text-sm leading-relaxed text-muted-foreground">{p.content}</p>

                      <div className="mt-3 flex items-center gap-1 text-xs text-muted-foreground">
                        <Clock className="size-3.5" aria-hidden="true" />
                        {formatDateTime(p.startTime)} ~ {formatDateTime(p.endTime)}
                      </div>

                      <div className="mt-3 flex gap-2">
                        {canVote ? (
                          <Link to={`/proposals/${p.id}/vote`} className="flex-1">
                            <Button className="w-full">
                              <Vote className="size-4" aria-hidden="true" />
                              前往投票
                            </Button>
                          </Link>
                        ) : (
                          <Link to={`/proposals/${p.id}/result`} className="flex-1">
                            <Button variant="outline" className="w-full">
                              查看結果
                              <ChevronRight className="size-4" aria-hidden="true" />
                            </Button>
                          </Link>
                        )}
                      </div>
                    </CardContent>
                  </Card>
                </li>
              )
            })}
          </ul>
        )}

        <div className="mt-6 flex flex-col items-center gap-2">
          <p className="text-center text-xs text-muted-foreground">
            僅顯示進行中與已公告之提案，避免誤投 · 每 5 秒自動更新
          </p>
          <Button variant="ghost" size="sm" onClick={handleLogout}>
            <LogOut className="size-4" aria-hidden="true" />
            登出
          </Button>
        </div>
      </main>
    </div>
  )
}
