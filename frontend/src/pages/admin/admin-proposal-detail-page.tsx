import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, BarChart3, Pencil, Play, Square, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { StatusBadge, TypeBadge } from '@/components/status-badge'
import { adminApi } from '@/lib/admin-api'
import { useAdminAuth } from '@/context/admin-auth-context'
import { formatDateTime } from '@/lib/labels'
import type { AdminProposal } from '@/lib/admin-types'

export function AdminProposalDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { community } = useAdminAuth()
  const [proposal, setProposal] = useState<AdminProposal | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!community || !id) return
    adminApi
      .getProposal(community.id, Number(id))
      .then(setProposal)
      .catch((err: Error) => {
        toast.error(err.message)
        navigate('/admin/proposals')
      })
      .finally(() => setLoading(false))
  }, [community, id, navigate])

  async function start() {
    if (!community || !proposal) return
    try {
      const updated = await adminApi.startProposal(community.id, proposal.id)
      setProposal(updated)
      toast.success('已啟動投票')
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '操作失敗')
    }
  }

  async function stop() {
    if (!community || !proposal) return
    try {
      const updated = await adminApi.stopProposal(community.id, proposal.id)
      setProposal(updated)
      toast.success('已終止投票')
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '操作失敗')
    }
  }

  async function remove() {
    if (!community || !proposal) return
    const ok = window.confirm('確定刪除此提案？若已有投票紀錄將一併刪除，且無法復原。')
    if (!ok) return
    try {
      await adminApi.deleteProposal(community.id, proposal.id)
      toast.success('提案已刪除')
      navigate('/admin/proposals')
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '刪除失敗')
    }
  }

  if (loading || !proposal) {
    return <p className="text-sm text-muted-foreground">載入中…</p>
  }

  return (
    <div className="mx-auto w-full max-w-3xl">
      <Link
        to="/admin/proposals"
        className="mb-2 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" aria-hidden="true" />
        返回提案管理
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-muted-foreground">{proposal.proposalNumber}</p>
          <h1 className="text-2xl font-bold text-foreground">{proposal.title}</h1>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <TypeBadge type={proposal.type} />
            <StatusBadge status={proposal.status} />
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          {proposal.status === 'ACTIVE' ? (
            <Button variant="outline" onClick={stop}>
              <Square className="size-4" aria-hidden="true" />
              終止投票
            </Button>
          ) : (
            proposal.status !== 'ENDED' && (
              <Button variant="outline" onClick={start}>
                <Play className="size-4" aria-hidden="true" />
                啟動投票
              </Button>
            )
          )}
          <Link to={`/admin/results/${proposal.id}`}>
            <Button variant="outline">
              <BarChart3 className="size-4" aria-hidden="true" />
              查看結果
            </Button>
          </Link>
          <Link to={`/admin/proposals/${proposal.id}/edit`}>
            <Button>
              <Pencil className="size-4" aria-hidden="true" />
              編輯
            </Button>
          </Link>
          <Button
            variant="outline"
            className="text-destructive hover:text-destructive"
            onClick={remove}
          >
            <Trash2 className="size-4" aria-hidden="true" />
            刪除
          </Button>
        </div>
      </div>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="text-base">提案內容</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="whitespace-pre-wrap text-sm leading-relaxed text-foreground">
            {proposal.content}
          </p>
        </CardContent>
      </Card>

      <Card className="mt-4">
        <CardHeader>
          <CardTitle className="text-base">投票設定</CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid gap-3 text-sm sm:grid-cols-2">
            <div className="flex justify-between gap-4 sm:flex-col sm:justify-start sm:gap-1">
              <dt className="text-muted-foreground">投票開始時間</dt>
              <dd className="font-medium text-foreground">{formatDateTime(proposal.startTime)}</dd>
            </div>
            <div className="flex justify-between gap-4 sm:flex-col sm:justify-start sm:gap-1">
              <dt className="text-muted-foreground">投票結束時間</dt>
              <dd className="font-medium text-foreground">{formatDateTime(proposal.endTime)}</dd>
            </div>
            <div className="flex justify-between gap-4 sm:flex-col sm:justify-start sm:gap-1">
              <dt className="text-muted-foreground">住戶端顯示</dt>
              <dd className="font-medium text-foreground">{proposal.visible ? '顯示' : '隱藏'}</dd>
            </div>
            <div className="flex justify-between gap-4 sm:flex-col sm:justify-start sm:gap-1">
              <dt className="text-muted-foreground">排序</dt>
              <dd className="font-medium text-foreground">{proposal.sortOrder}</dd>
            </div>
            <div className="flex justify-between gap-4 sm:flex-col sm:justify-start sm:gap-1">
              <dt className="text-muted-foreground">建立時間</dt>
              <dd className="font-medium text-foreground">{formatDateTime(proposal.createdAt)}</dd>
            </div>
          </dl>
        </CardContent>
      </Card>
    </div>
  )
}
