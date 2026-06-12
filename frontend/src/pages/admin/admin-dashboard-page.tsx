import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { FileText, Vote, Users, BarChart3, ArrowRight, Plus, CalendarDays } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { StatusBadge, TypeBadge } from '@/components/status-badge'
import { adminApi } from '@/lib/admin-api'
import { useAdminAuth } from '@/context/admin-auth-context'
import { pct } from '@/lib/labels'
import type { AdminOwner, AdminProposal } from '@/lib/admin-types'
import type { ProposalResult } from '@/lib/types'

export function AdminDashboardPage() {
  const { community } = useAdminAuth()
  const [proposals, setProposals] = useState<AdminProposal[]>([])
  const [owners, setOwners] = useState<AdminOwner[]>([])
  const [activeResults, setActiveResults] = useState<Record<number, ProposalResult>>({})

  useEffect(() => {
    if (!community) return
    Promise.all([
      adminApi.listProposals(community.id),
      adminApi.listOwners(community.id),
    ]).then(async ([p, o]) => {
      setProposals(p)
      setOwners(o)
      const active = p.filter((x) => x.status === 'ACTIVE')
      const results: Record<number, ProposalResult> = {}
      await Promise.all(
        active.map(async (prop) => {
          try {
            results[prop.id] = await adminApi.getProposalResult(community.id, prop.id)
          } catch {
            // ignore
          }
        }),
      )
      setActiveResults(results)
    })
  }, [community])

  const activeProposals = proposals.filter((p) => p.status === 'ACTIVE')
  const attended = owners.filter((o) => o.attended).length

  const stats = [
    {
      label: '進行中提案',
      value: activeProposals.length,
      sub: `共 ${proposals.length} 項提案`,
      icon: Vote,
      tint: 'text-primary bg-primary/10',
    },
    {
      label: '所有權人',
      value: owners.length,
      sub: `全社區 ${community?.totalHouseholds ?? 0} 戶`,
      icon: Users,
      tint: 'text-chart-3 bg-chart-3/10',
    },
    {
      label: '已出席',
      value: attended,
      sub: '完成身份驗證',
      icon: Users,
      tint: 'text-chart-4 bg-chart-4/10',
    },
    {
      label: '區分所有權總計',
      value: `${Number(community?.totalArea ?? 0).toLocaleString()} 坪`,
      sub: community?.name ?? '',
      icon: BarChart3,
      tint: 'text-primary bg-primary/10',
    },
  ]

  return (
    <div className="mx-auto w-full max-w-5xl">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-foreground">管理儀表板</h1>
          <p className="mt-1 flex items-center gap-1.5 text-sm text-muted-foreground">
            <CalendarDays className="size-4" aria-hidden="true" />
            {community?.name} · 區分所有權人會議
          </p>
        </div>
        <Link to="/admin/proposals/new">
          <Button>
            <Plus className="size-4" aria-hidden="true" />
            新增提案
          </Button>
        </Link>
      </div>

      <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((s) => (
          <Card key={s.label}>
            <CardContent className="pt-6">
              <div className={`flex size-9 items-center justify-center rounded-lg ${s.tint}`}>
                <s.icon className="size-5" aria-hidden="true" />
              </div>
              <p className="mt-3 text-2xl font-black text-foreground">{s.value}</p>
              <p className="text-sm font-medium text-foreground">{s.label}</p>
              <p className="text-xs text-muted-foreground">{s.sub}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <CardTitle className="text-base">進行中提案即時概況</CardTitle>
            <Link to="/admin/proposals" className="text-sm text-primary hover:underline">
              全部提案 →
            </Link>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {activeProposals.length === 0 ? (
              <p className="text-sm text-muted-foreground">目前無進行中提案</p>
            ) : (
              activeProposals.map((p) => {
                const r = activeResults[p.id]
                return (
                  <Link
                    key={p.id}
                    to={`/admin/results/${p.id}`}
                    className="flex flex-col gap-2 rounded-lg border border-border p-3 transition-colors hover:bg-secondary"
                  >
                    <div className="flex items-center gap-2">
                      <TypeBadge type={p.type} />
                      <StatusBadge status={p.status} />
                    </div>
                    <p className="text-pretty font-medium leading-snug text-foreground">{p.title}</p>
                    {r && (
                      <div className="flex items-center gap-4 text-xs text-muted-foreground">
                        <span>已投 {r.totalVotedHouseholds} 戶</span>
                        <span>同意 {pct(r.agreeHouseholdRatio)}</span>
                        <span>權重同意 {pct(r.agreeWeightRatio)}</span>
                      </div>
                    )}
                  </Link>
                )
              })
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">快速連結</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            {[
              { href: '/admin/proposals', label: '提案管理', icon: FileText },
              { href: '/admin/results', label: '投票結果詳情', icon: BarChart3 },
              { href: '/admin/owners', label: '所有權人管理', icon: Users },
            ].map((l) => (
              <Link
                key={l.href}
                to={l.href}
                className="flex items-center gap-3 rounded-lg border border-border p-3 transition-colors hover:bg-secondary"
              >
                <l.icon className="size-5 text-primary" aria-hidden="true" />
                <span className="font-medium text-foreground">{l.label}</span>
                <ArrowRight className="ml-auto size-4 text-muted-foreground" aria-hidden="true" />
              </Link>
            ))}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
