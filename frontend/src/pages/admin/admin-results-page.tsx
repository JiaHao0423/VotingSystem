import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, BarChart3 } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { StatusBadge, TypeBadge } from '@/components/status-badge'
import { adminApi } from '@/lib/admin-api'
import { useAdminAuth } from '@/context/admin-auth-context'
import { pct } from '@/lib/labels'
import { cn } from '@/lib/utils'
import type { AdminProposal } from '@/lib/admin-types'
import type { ProposalResult } from '@/lib/types'

export function AdminResultsPage() {
  const { community } = useAdminAuth()
  const [items, setItems] = useState<{ proposal: AdminProposal; result: ProposalResult | null }[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!community) return
    Promise.all([adminApi.listProposals(community.id), adminApi.listProposalResults(community.id)])
      .then(([proposals, results]) => {
        const resultById = new Map(results.map((r) => [r.id, r]))
        setItems(
          proposals.map((p) => ({
            proposal: p,
            result: resultById.get(p.id) ?? null,
          })),
        )
      })
      .finally(() => setLoading(false))
  }, [community])

  if (loading) {
    return <p className="text-sm text-muted-foreground">載入中…</p>
  }

  return (
    <div className="mx-auto w-full max-w-5xl">
      <h1 className="text-2xl font-bold text-foreground">投票結果詳情</h1>
      <p className="mt-1 text-sm text-muted-foreground">查看各提案得票數、得票比例與通過門檻判定</p>

      <div className="mt-6 grid gap-3 md:grid-cols-2">
        {items.map(({ proposal: p, result: r }) => {
          const hasVotes = r && r.totalVotedHouseholds > 0
          return (
            <Link key={p.id} to={`/admin/results/${p.id}`}>
              <Card className="h-full transition-colors hover:bg-secondary">
                <CardContent className="p-4">
                  <div className="flex flex-wrap items-center gap-2">
                    <TypeBadge type={p.type} />
                    <StatusBadge status={p.status} />
                    {hasVotes && r && (
                      <span
                        className={cn(
                          'ml-auto rounded-full px-2 py-0.5 text-xs font-medium',
                          r.passed ? 'bg-chart-3/15 text-chart-3' : 'bg-chart-5/15 text-chart-5',
                        )}
                      >
                        {r.passed ? '已通過' : '未通過'}
                      </span>
                    )}
                  </div>
                  <p className="mt-2 text-xs text-muted-foreground">{p.proposalNumber}</p>
                  <p className="text-pretty font-bold leading-snug text-foreground">{p.title}</p>

                  {hasVotes && r ? (
                    <div className="mt-3 grid grid-cols-3 gap-2 text-center">
                      <div className="rounded-md bg-secondary p-2">
                        <p className="text-sm font-black text-foreground">{r.totalVotedHouseholds}</p>
                        <p className="text-[11px] text-muted-foreground">總票數</p>
                      </div>
                      <div className="rounded-md bg-secondary p-2">
                        <p className="text-sm font-black text-foreground">{pct(r.agreeHouseholdRatio)}</p>
                        <p className="text-[11px] text-muted-foreground">同意(人數)</p>
                      </div>
                      <div className="rounded-md bg-secondary p-2">
                        <p className="text-sm font-black text-foreground">{pct(r.agreeWeightRatio)}</p>
                        <p className="text-[11px] text-muted-foreground">同意(權)</p>
                      </div>
                    </div>
                  ) : (
                    <p className="mt-3 text-sm text-muted-foreground">尚未開始投票</p>
                  )}

                  <div className="mt-3 flex items-center gap-1 text-sm font-medium text-primary">
                    <BarChart3 className="size-4" aria-hidden="true" />
                    查看詳情
                    <ArrowRight className="size-4" aria-hidden="true" />
                  </div>
                </CardContent>
              </Card>
            </Link>
          )
        })}
      </div>
    </div>
  )
}
