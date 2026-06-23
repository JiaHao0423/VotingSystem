import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus, Pencil, Trash2, BarChart3, Eye, GripVertical, RotateCcw } from 'lucide-react'
import { toast } from 'sonner'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Switch } from '@/components/ui/switch'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { StatusBadge, TypeBadge } from '@/components/status-badge'
import { adminApi } from '@/lib/admin-api'
import { useAdminAuth } from '@/context/admin-auth-context'
import { formatDateTime } from '@/lib/labels'
import { cn } from '@/lib/utils'
import type { AdminProposal } from '@/lib/admin-types'
import type { ProposalStatus, ProposalType } from '@/lib/types'

type SortMode = 'manual' | 'type' | 'status' | 'time'

export function AdminProposalsPage() {
  const { community } = useAdminAuth()
  const [list, setList] = useState<AdminProposal[]>([])
  const [loading, setLoading] = useState(true)
  const [sortMode, setSortMode] = useState<SortMode>('manual')
  const [draggingId, setDraggingId] = useState<number | null>(null)
  const [dropTargetId, setDropTargetId] = useState<number | null>(null)
  const dragIdRef = useRef<number | null>(null)
  const dropTargetIdRef = useRef<number | null>(null)
  const didDropRef = useRef(false)
  const [togglingId, setTogglingId] = useState<number | null>(null)

  const reload = useCallback(async () => {
    if (!community) return
    try {
      const data = await adminApi.listProposals(community.id)
      setList(data)
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '無法載入提案列表')
      setList([])
    }
  }, [community])

  useEffect(() => {
    reload().finally(() => setLoading(false))
  }, [reload])

  const displayed = useMemo(() => {
    const items = [...list]
    if (sortMode === 'type') {
      return items.sort((a, b) => a.type.localeCompare(b.type) || a.sortOrder - b.sortOrder)
    }
    if (sortMode === 'status') {
      const order: Record<ProposalStatus, number> = {
        ACTIVE: 0,
        SCHEDULED: 1,
        DRAFT: 2,
        ENDED: 3,
      }
      return items.sort((a, b) => order[a.status] - order[b.status] || a.sortOrder - b.sortOrder)
    }
    if (sortMode === 'time') {
      return items.sort((a, b) => {
        const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0
        const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0
        return tb - ta
      })
    }
    return items
  }, [list, sortMode])

  function reorderList(prev: AdminProposal[], fromId: number, toId: number): AdminProposal[] {
    const next = [...prev]
    const from = next.findIndex((p) => p.id === fromId)
    const to = next.findIndex((p) => p.id === toId)
    if (from < 0 || to < 0 || from === to) return prev
    const [moved] = next.splice(from, 1)
    next.splice(to, 0, moved)
    return next.map((p, i) => ({ ...p, sortOrder: i }))
  }

  function clearDragState() {
    dragIdRef.current = null
    dropTargetIdRef.current = null
    setDraggingId(null)
    setDropTargetId(null)
  }

  function onHandleDragStart(e: React.DragEvent<HTMLDivElement>, id: number) {
    if (sortMode !== 'manual') return
    didDropRef.current = false
    dragIdRef.current = id
    dropTargetIdRef.current = null
    setDraggingId(id)
    setDropTargetId(null)
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(id))
    const row = e.currentTarget.closest('[data-proposal-row]')
    if (row instanceof HTMLElement) {
      e.dataTransfer.setDragImage(row, 32, 24)
    }
  }

  function onRowDragOver(e: React.DragEvent, overId: number) {
    if (sortMode !== 'manual') return
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    const fromId = dragIdRef.current
    if (fromId == null || fromId === overId) return
    dropTargetIdRef.current = overId
    setDropTargetId(overId)
  }

  async function applyReorder(fromId: number, toId: number) {
    if (fromId === toId) return
    let nextList: AdminProposal[] | null = null
    setList((prev) => {
      nextList = reorderList(prev, fromId, toId)
      return nextList
    })
    if (nextList) {
      await persistOrder(nextList)
    }
  }

  async function onRowDrop(e: React.DragEvent, overId: number) {
    if (sortMode !== 'manual') return
    e.preventDefault()
    e.stopPropagation()
    didDropRef.current = true
    const raw = e.dataTransfer.getData('text/plain')
    const fromId = raw ? Number(raw) : dragIdRef.current
    if (fromId != null) {
      await applyReorder(fromId, overId)
    }
    clearDragState()
  }

  async function onHandleDragEnd() {
    if (sortMode === 'manual' && !didDropRef.current) {
      const fromId = dragIdRef.current
      const toId = dropTargetIdRef.current
      if (fromId != null && toId != null) {
        await applyReorder(fromId, toId)
      }
    }
    didDropRef.current = false
    clearDragState()
  }

  async function toggleVoting(p: AdminProposal, active: boolean) {
    if (!community) return
    const previousStatus = p.status
    setTogglingId(p.id)
    setList((prev) =>
      prev.map((item) =>
        item.id === p.id ? { ...item, status: active ? 'ACTIVE' : 'ENDED' } : item,
      ),
    )
    try {
      await adminApi.toggleProposalVoting(community.id, p.id, active)
      toast.success(active ? '已啟動投票' : '已終止投票')
      await reload()
    } catch (err) {
      setList((prev) =>
        prev.map((item) => (item.id === p.id ? { ...item, status: previousStatus } : item)),
      )
      toast.error(err instanceof Error ? err.message : '操作失敗')
    } finally {
      setTogglingId(null)
    }
  }

  async function remove(id: number) {
    if (!community || !confirm('確定刪除此提案？若已有投票紀錄將一併刪除，且無法復原。')) return
    try {
      await adminApi.deleteProposal(community.id, id)
      toast.success('提案已刪除')
      reload()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '刪除失敗')
    }
  }

  async function resetVotes(p: AdminProposal) {
    if (!community) return
    if (!confirm(`確定清除「${p.title}」的所有投票紀錄？住戶可重新投票。`)) return
    try {
      await adminApi.resetProposalVotes(community.id, p.id)
      toast.success('已清除投票紀錄')
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '操作失敗')
    }
  }

  async function persistOrder(next: AdminProposal[]) {
    if (!community || sortMode !== 'manual') return
    try {
      await adminApi.reorderProposals(
        community.id,
        next.map((p) => p.id),
      )
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '排序儲存失敗')
      reload()
    }
  }

  function ProposalActions({ p }: { p: AdminProposal }) {
    return (
      <div className="flex flex-wrap items-center justify-end gap-1.5">
        <Button variant="ghost" size="sm" onClick={() => resetVotes(p)} aria-label="清除投票">
          <RotateCcw className="size-4" aria-hidden="true" />
        </Button>
        <Link to={`/admin/proposals/${p.id}`}>
          <Button variant="ghost" size="sm" aria-label="檢視詳情">
            <Eye className="size-4" aria-hidden="true" />
          </Button>
        </Link>
        <Link to={`/admin/results/${p.id}`}>
          <Button variant="ghost" size="sm" aria-label="查看結果">
            <BarChart3 className="size-4" aria-hidden="true" />
          </Button>
        </Link>
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
      </div>
    )
  }

  return (
    <div className="mx-auto w-full max-w-5xl">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-foreground">提案管理</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            切換投票狀態、新增提案會自動排在最後；手動排序模式下可拖曳左側握把調整順序
          </p>
        </div>
        <Link to="/admin/proposals/new">
          <Button>
            <Plus className="size-4" aria-hidden="true" />
            新增提案
          </Button>
        </Link>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-2">
        <span className="text-sm text-muted-foreground">排序：</span>
        {(
          [
            ['manual', '手動排序'],
            ['type', '依類型'],
            ['status', '依狀態'],
            ['time', '依時間'],
          ] as const
        ).map(([value, label]) => (
          <Button
            key={value}
            size="sm"
            variant={sortMode === value ? 'default' : 'outline'}
            onClick={() => setSortMode(value)}
          >
            {label}
          </Button>
        ))}
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
                    {sortMode === 'manual' && <TableHead className="w-8" />}
                    <TableHead>提案</TableHead>
                    <TableHead>類型</TableHead>
                    <TableHead>狀態</TableHead>
                    <TableHead>投票</TableHead>
                    <TableHead className="sticky right-0 bg-card text-right shadow-[-8px_0_12px_-8px_rgba(0,0,0,0.15)]">
                      操作
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {displayed.map((p) => (
                    <TableRow
                      key={p.id}
                      data-proposal-row
                      onDragOver={(e) => onRowDragOver(e, p.id)}
                      onDrop={(e) => onRowDrop(e, p.id)}
                      className={cn(
                        draggingId === p.id && 'opacity-60',
                        dropTargetId === p.id &&
                          draggingId !== p.id &&
                          'bg-primary/5 ring-1 ring-inset ring-primary/30',
                      )}
                    >
                      {sortMode === 'manual' && (
                        <TableCell className="w-8 text-muted-foreground">
                          <div
                            draggable
                            onDragStart={(e) => onHandleDragStart(e, p.id)}
                            onDragEnd={onHandleDragEnd}
                            className="inline-flex cursor-grab touch-none select-none active:cursor-grabbing"
                            aria-label="拖曳排序"
                            title="拖曳以調整順序"
                          >
                            <GripVertical className="size-4" aria-hidden="true" />
                          </div>
                        </TableCell>
                      )}
                      <TableCell
                        onDragOver={(e) => onRowDragOver(e, p.id)}
                        onDrop={(e) => onRowDrop(e, p.id)}
                      >
                        <p className="text-xs text-muted-foreground">{p.proposalNumber}</p>
                        <p className="max-w-xs font-medium leading-snug text-foreground">{p.title}</p>
                        <p className="mt-0.5 text-xs text-muted-foreground">
                          {formatDateTime(p.startTime)} ~ {formatDateTime(p.endTime)}
                        </p>
                      </TableCell>
                      <TableCell>
                        <TypeBadge type={p.type as ProposalType} />
                      </TableCell>
                      <TableCell>
                        <StatusBadge status={p.status} />
                      </TableCell>
                      <TableCell
                        className="select-none"
                        onPointerDown={(e) => e.stopPropagation()}
                      >
                        <div className="flex items-center gap-2">
                          <Switch
                            checked={p.status === 'ACTIVE'}
                            disabled={togglingId === p.id}
                            onCheckedChange={(checked) => toggleVoting(p, checked)}
                            aria-label={p.status === 'ACTIVE' ? '終止投票' : '啟動投票'}
                          />
                          <span className="text-xs text-muted-foreground">
                            {p.status === 'ACTIVE' ? '投票中' : '未啟動'}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell className="sticky right-0 bg-card shadow-[-8px_0_12px_-8px_rgba(0,0,0,0.15)]">
                        <ProposalActions p={p} />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              {displayed.length === 0 && (
                <p className="py-10 text-center text-sm text-muted-foreground">
                  尚無提案，請點「新增提案」建立第一筆
                </p>
              )}
            </CardContent>
          </Card>

          <div className="mt-6 flex flex-col gap-3 md:hidden">
            {displayed.map((p) => (
              <Card key={p.id}>
                <CardContent className="p-4">
                  <div className="flex flex-wrap items-center gap-2">
                    <TypeBadge type={p.type as ProposalType} />
                    <StatusBadge status={p.status} />
                  </div>
                  <p className="mt-2 text-xs text-muted-foreground">{p.proposalNumber}</p>
                  <p className="font-medium text-foreground">{p.title}</p>
                  <div className="mt-3 flex items-center gap-2">
                    <Switch
                      checked={p.status === 'ACTIVE'}
                      disabled={togglingId === p.id}
                      onCheckedChange={(checked) => toggleVoting(p, checked)}
                      aria-label={p.status === 'ACTIVE' ? '終止投票' : '啟動投票'}
                    />
                    <span className="text-xs text-muted-foreground">
                      {p.status === 'ACTIVE' ? '投票中' : '未啟動'}
                    </span>
                  </div>
                  <div className="mt-3">
                    <ProposalActions p={p} />
                  </div>
                </CardContent>
              </Card>
            ))}
            {displayed.length === 0 && (
              <p className="py-10 text-center text-sm text-muted-foreground">
                尚無提案，請點「新增提案」建立第一筆
              </p>
            )}
          </div>
        </>
      )}
    </div>
  )
}
