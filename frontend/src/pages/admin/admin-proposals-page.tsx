import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus, Pencil, Trash2, Play, Square, BarChart3 } from 'lucide-react'
import { toast } from 'sonner'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { StatusBadge, TypeBadge } from '@/components/status-badge'
import { adminApi } from '@/lib/admin-api'
import { useAdminAuth } from '@/context/admin-auth-context'
import { formatDateTime } from '@/lib/labels'
import type { AdminProposal } from '@/lib/admin-types'

export function AdminProposalsPage() {
  const { community } = useAdminAuth()
  const [list, setList] = useState<AdminProposal[]>([])
  const [loading, setLoading] = useState(true)

  const reload = useCallback(async () => {
    if (!community) return
    const data = await adminApi.listProposals(community.id)
    setList(data)
  }, [community])

  useEffect(() => {
    reload().finally(() => setLoading(false))
  }, [reload])

  async function start(id: number) {
    if (!community) return
    try {
      await adminApi.startProposal(community.id, id)
      toast.success('已啟動投票')
      reload()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '操作失敗')
    }
  }

  async function stop(id: number) {
    if (!community) return
    try {
      await adminApi.stopProposal(community.id, id)
      toast.success('已終止投票')
      reload()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '操作失敗')
    }
  }

  async function remove(id: number) {
    if (!community || !confirm('確定刪除此提案？')) return
    try {
      await adminApi.deleteProposal(community.id, id)
      toast.success('提案已刪除')
      reload()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '刪除失敗')
    }
  }

  function ProposalActions({ p }: { p: AdminProposal }) {
    const editable = p.status === 'DRAFT' || p.status === 'SCHEDULED'
    return (
      <div className="flex items-center justify-end gap-1">
        {p.status === 'ACTIVE' ? (
          <Button variant="outline" size="sm" onClick={() => stop(p.id)}>
            <Square className="size-3.5" aria-hidden="true" />
            終止
          </Button>
        ) : (
          editable && (
            <Button variant="outline" size="sm" onClick={() => start(p.id)}>
              <Play className="size-3.5" aria-hidden="true" />
              啟動
            </Button>
          )
        )}
        <Link to={`/admin/results/${p.id}`}>
          <Button variant="ghost" size="sm" aria-label="查看結果">
            <BarChart3 className="size-4" aria-hidden="true" />
          </Button>
        </Link>
        {editable && (
          <>
            <Link to={`/admin/proposals/${p.id}/edit`}>
              <Button variant="ghost" size="sm" aria-label="編輯">
                <Pencil className="size-4" aria-hidden="true" />
              </Button>
            </Link>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => remove(p.id)}
              className="text-destructive hover:text-destructive"
              aria-label="刪除"
            >
              <Trash2 className="size-4" aria-hidden="true" />
            </Button>
          </>
        )}
      </div>
    )
  }

  return (
    <div className="mx-auto w-full max-w-5xl">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-foreground">提案管理</h1>
          <p className="mt-1 text-sm text-muted-foreground">新增、編輯、刪除提案並控制投票流程</p>
        </div>
        <Link to="/admin/proposals/new">
          <Button>
            <Plus className="size-4" aria-hidden="true" />
            新增提案
          </Button>
        </Link>
      </div>

      {loading ? (
        <p className="mt-8 text-sm text-muted-foreground">載入中…</p>
      ) : (
        <>
          <Card className="mt-6 hidden md:block">
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>提案</TableHead>
                    <TableHead>類型</TableHead>
                    <TableHead>狀態</TableHead>
                    <TableHead>顯示</TableHead>
                    <TableHead className="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {list.map((p) => (
                    <TableRow key={p.id}>
                      <TableCell>
                        <p className="text-xs text-muted-foreground">{p.proposalNumber}</p>
                        <p className="max-w-xs font-medium leading-snug text-foreground">{p.title}</p>
                        <p className="mt-0.5 text-xs text-muted-foreground">
                          {formatDateTime(p.startTime)} ~ {formatDateTime(p.endTime)}
                        </p>
                      </TableCell>
                      <TableCell>
                        <TypeBadge type={p.type} />
                      </TableCell>
                      <TableCell>
                        <StatusBadge status={p.status} />
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {p.visible ? '是' : '否'}
                      </TableCell>
                      <TableCell>
                        <ProposalActions p={p} />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <div className="mt-6 flex flex-col gap-3 md:hidden">
            {list.map((p) => (
              <Card key={p.id}>
                <CardContent className="p-4">
                  <div className="flex flex-wrap items-center gap-2">
                    <TypeBadge type={p.type} />
                    <StatusBadge status={p.status} />
                  </div>
                  <p className="mt-2 text-xs text-muted-foreground">{p.proposalNumber}</p>
                  <p className="font-medium text-foreground">{p.title}</p>
                  <div className="mt-3">
                    <ProposalActions p={p} />
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </>
      )}
    </div>
  )
}
