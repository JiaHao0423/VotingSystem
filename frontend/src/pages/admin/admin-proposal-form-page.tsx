import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Save } from 'lucide-react'
import { toast } from 'sonner'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { adminApi } from '@/lib/admin-api'
import { useAdminAuth } from '@/context/admin-auth-context'
import type { ProposalType } from '@/lib/types'

function toLocalDatetime(iso: string | null): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function fromLocalDatetime(value: string): string | null {
  if (!value) return null
  return new Date(value).toISOString()
}

export function AdminProposalFormPage({ mode }: { mode: 'new' | 'edit' }) {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { community } = useAdminAuth()
  const [loading, setLoading] = useState(mode === 'edit')
  const [proposalNumber, setProposalNumber] = useState('')
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [type, setType] = useState<ProposalType>('GENERAL')
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [visible, setVisible] = useState(false)
  const [sortOrder, setSortOrder] = useState(0)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (mode !== 'edit' || !community || !id) return
    adminApi
      .getProposal(community.id, Number(id))
      .then((p) => {
        setProposalNumber(p.proposalNumber)
        setTitle(p.title)
        setContent(p.content)
        setType(p.type)
        setStartTime(toLocalDatetime(p.startTime))
        setEndTime(toLocalDatetime(p.endTime))
        setVisible(p.visible)
        setSortOrder(p.sortOrder)
      })
      .catch((err: Error) => {
        toast.error(err.message)
        navigate('/admin/proposals')
      })
      .finally(() => setLoading(false))
  }, [mode, community, id, navigate])

  async function save() {
    if (!community) return
    if (!title.trim() || !proposalNumber.trim() || !content.trim()) {
      toast.error('請填寫提案編號、標題與內容')
      return
    }
    setSubmitting(true)
    const body = {
      proposalNumber: proposalNumber.trim(),
      title: title.trim(),
      content: content.trim(),
      type,
      startTime: fromLocalDatetime(startTime),
      endTime: fromLocalDatetime(endTime),
      visible,
      sortOrder,
    }
    try {
      if (mode === 'new') {
        await adminApi.createProposal(community.id, body)
        toast.success('提案已新增')
      } else {
        await adminApi.updateProposal(community.id, Number(id), body)
        toast.success('提案已更新')
      }
      navigate('/admin/proposals')
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '儲存失敗')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
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
      <h1 className="text-2xl font-bold text-foreground">
        {mode === 'new' ? '新增提案' : '編輯提案'}
      </h1>
      <p className="mt-1 text-sm text-muted-foreground">投票選項固定為：同意、反對、棄權</p>

      <div className="mt-6 flex flex-col gap-4">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">基本資訊</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div className="grid gap-4 sm:grid-cols-3">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="number">提案編號</Label>
                <Input
                  id="number"
                  placeholder="P-001"
                  value={proposalNumber}
                  onChange={(e) => setProposalNumber(e.target.value)}
                />
              </div>
              <div className="flex flex-col gap-1.5 sm:col-span-2">
                <Label htmlFor="type">提案類型</Label>
                <select
                  id="type"
                  className="h-9 rounded-lg border border-input bg-transparent px-3 text-sm"
                  value={type}
                  onChange={(e) => setType(e.target.value as ProposalType)}
                >
                  <option value="GENERAL">一般提案</option>
                  <option value="EXTRAORDINARY">臨時提案</option>
                </select>
              </div>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="title">提案標題</Label>
              <Input
                id="title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="請輸入提案標題"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="content">提案內容</Label>
              <Textarea
                id="content"
                rows={5}
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder="請詳細說明提案內容"
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">投票設定</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="start">投票開始時間</Label>
                <Input
                  id="start"
                  type="datetime-local"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="end">投票結束時間</Label>
                <Input
                  id="end"
                  type="datetime-local"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                />
              </div>
            </div>
            <div className="flex items-center justify-between rounded-lg border border-border p-3">
              <div>
                <p className="text-sm font-medium">前端顯示</p>
                <p className="text-xs text-muted-foreground">啟動投票時會自動設為顯示</p>
              </div>
              <input
                type="checkbox"
                checked={visible}
                onChange={(e) => setVisible(e.target.checked)}
                className="size-4 accent-primary"
                aria-label="前端顯示"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="sort">排序</Label>
              <Input
                id="sort"
                type="number"
                value={sortOrder}
                onChange={(e) => setSortOrder(Number(e.target.value))}
              />
            </div>
          </CardContent>
        </Card>

        <div className="flex justify-end gap-3">
          <Link to="/admin/proposals">
            <Button variant="outline">取消</Button>
          </Link>
          <Button onClick={save} disabled={submitting}>
            <Save className="size-4" aria-hidden="true" />
            {submitting ? '儲存中…' : mode === 'new' ? '建立提案' : '儲存變更'}
          </Button>
        </div>
      </div>
    </div>
  )
}
