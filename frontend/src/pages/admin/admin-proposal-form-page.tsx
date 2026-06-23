import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, GripVertical, Plus, Save, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Switch } from '@/components/ui/switch'
import { adminApi } from '@/lib/admin-api'
import { useAdminAuth } from '@/context/admin-auth-context'
import { DEFAULT_VOTE_OPTIONS } from '@/lib/labels'
import { cn } from '@/lib/utils'
import type { ProposalType, ThresholdBase } from '@/lib/types'
import type { VoteOptionInput } from '@/lib/admin-types'

type VoteOptionRow = VoteOptionInput & { id: string }

function newOptionId(): string {
  return crypto.randomUUID()
}

function withOptionIds(options: VoteOptionInput[]): VoteOptionRow[] {
  return options.map((opt) => ({ ...opt, id: newOptionId() }))
}

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
  const [voteOptions, setVoteOptions] = useState<VoteOptionRow[]>(() => withOptionIds(DEFAULT_VOTE_OPTIONS))
  const [passNumerator, setPassNumerator] = useState(1)
  const [passDenominator, setPassDenominator] = useState(2)
  const [thresholdBase, setThresholdBase] = useState<ThresholdBase>('ATTENDED')
  const [allowRevote, setAllowRevote] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [draggingOptionId, setDraggingOptionId] = useState<string | null>(null)
  const [dropTargetOptionId, setDropTargetOptionId] = useState<string | null>(null)
  const dragOptionIdRef = useRef<string | null>(null)
  const dropTargetOptionIdRef = useRef<string | null>(null)
  const didDropRef = useRef(false)

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
        setVoteOptions(
          withOptionIds(
            p.voteOptions.map((o) => ({
              label: o.label,
              description: o.description ?? '',
              passOption: o.passOption,
            })),
          ),
        )
        setPassNumerator(p.passThresholdNumerator)
        setPassDenominator(p.passThresholdDenominator)
        setThresholdBase(p.thresholdBase)
        setAllowRevote(p.allowRevote)
      })
      .catch((err: Error) => {
        toast.error(err.message)
        navigate('/admin/proposals')
      })
      .finally(() => setLoading(false))
  }, [mode, community, id, navigate])

  function updateOption(index: number, patch: Partial<VoteOptionInput>) {
    setVoteOptions((prev) => prev.map((opt, i) => (i === index ? { ...opt, ...patch } : opt)))
  }

  function addOption() {
    setVoteOptions((prev) => [
      ...prev,
      { id: newOptionId(), label: '', description: '', passOption: false },
    ])
  }

  function removeOption(index: number) {
    setVoteOptions((prev) => prev.filter((_, i) => i !== index))
  }

  function resetDefaultOptions() {
    setVoteOptions(withOptionIds(DEFAULT_VOTE_OPTIONS))
  }

  function moveOption(fromId: string, toId: string) {
    if (fromId === toId) return
    setVoteOptions((prev) => {
      const next = [...prev]
      const from = next.findIndex((o) => o.id === fromId)
      const to = next.findIndex((o) => o.id === toId)
      if (from < 0 || to < 0) return prev
      const [moved] = next.splice(from, 1)
      next.splice(to, 0, moved)
      return next
    })
  }

  function clearOptionDragState() {
    dragOptionIdRef.current = null
    dropTargetOptionIdRef.current = null
    setDraggingOptionId(null)
    setDropTargetOptionId(null)
  }

  function onHandleDragStart(e: React.DragEvent<HTMLDivElement>, optionId: string) {
    didDropRef.current = false
    dragOptionIdRef.current = optionId
    dropTargetOptionIdRef.current = null
    setDraggingOptionId(optionId)
    setDropTargetOptionId(null)
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', optionId)
    const row = e.currentTarget.closest('[data-option-row]')
    if (row instanceof HTMLElement) {
      e.dataTransfer.setDragImage(row, 32, 32)
    }
  }

  function onRowDragOver(e: React.DragEvent<HTMLDivElement>, overId: string) {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    const fromId = dragOptionIdRef.current
    if (!fromId || fromId === overId) return
    dropTargetOptionIdRef.current = overId
    setDropTargetOptionId(overId)
  }

  function onRowDrop(e: React.DragEvent<HTMLDivElement>, overId: string) {
    e.preventDefault()
    e.stopPropagation()
    didDropRef.current = true
    const fromId = e.dataTransfer.getData('text/plain') || dragOptionIdRef.current
    if (fromId) {
      moveOption(fromId, overId)
    }
    clearOptionDragState()
  }

  function onHandleDragEnd() {
    if (!didDropRef.current) {
      const fromId = dragOptionIdRef.current
      const toId = dropTargetOptionIdRef.current
      if (fromId && toId) {
        moveOption(fromId, toId)
      }
    }
    didDropRef.current = false
    clearOptionDragState()
  }

  async function save() {
    if (!community) return
    if (!title.trim() || !proposalNumber.trim() || !content.trim()) {
      toast.error('請填寫提案編號、標題與內容')
      return
    }
    if (voteOptions.length === 0 || voteOptions.some((o) => !o.label.trim())) {
      toast.error('請填寫所有投票選項名稱')
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
      voteOptions: voteOptions.map((o) => ({
        label: o.label.trim(),
        description: o.description?.trim() || null,
        passOption: o.passOption,
      })),
      passThresholdNumerator: passNumerator,
      passThresholdDenominator: passDenominator,
      thresholdBase,
      allowRevote,
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
      <p className="mt-1 text-sm text-muted-foreground">可自訂投票選項、通過門檻與重新投票設定</p>

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
              <Input id="title" value={title} onChange={(e) => setTitle(e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="content">提案內容</Label>
              <Textarea
                id="content"
                rows={5}
                value={content}
                onChange={(e) => setContent(e.target.value)}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between gap-3">
            <CardTitle className="text-base">投票選項</CardTitle>
            <div className="flex gap-2">
              <Button type="button" variant="outline" size="sm" onClick={resetDefaultOptions}>
                恢復預設
              </Button>
              <Button type="button" variant="outline" size="sm" onClick={addOption}>
                <Plus className="size-4" aria-hidden="true" />
                新增選項
              </Button>
            </div>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <p className="text-xs text-muted-foreground">拖曳左側握把可調整選項順序</p>
            {voteOptions.map((opt, index) => (
              <div
                key={opt.id}
                data-option-row
                onDragOver={(e) => onRowDragOver(e, opt.id)}
                onDrop={(e) => onRowDrop(e, opt.id)}
                className={cn(
                  'rounded-lg border border-border p-3 transition-shadow',
                  draggingOptionId === opt.id && 'opacity-60',
                  dropTargetOptionId === opt.id &&
                    draggingOptionId !== opt.id &&
                    'border-primary ring-2 ring-primary/30',
                )}
              >
                <div className="flex gap-3">
                  <div
                    draggable
                    onDragStart={(e) => onHandleDragStart(e, opt.id)}
                    onDragEnd={onHandleDragEnd}
                    className="flex shrink-0 cursor-grab touch-none select-none items-center self-stretch text-muted-foreground active:cursor-grabbing"
                    aria-label={`拖曳排序第 ${index + 1} 項`}
                    title="拖曳以調整順序"
                  >
                    <GripVertical className="size-5" aria-hidden="true" />
                  </div>
                  <div
                    className="min-w-0 flex-1"
                    onDragOver={(e) => onRowDragOver(e, opt.id)}
                    onDrop={(e) => onRowDrop(e, opt.id)}
                  >
                    <div className="grid gap-3 sm:grid-cols-2">
                      <div className="flex flex-col gap-1.5">
                        <Label>選項名稱</Label>
                        <Input
                          value={opt.label}
                          onChange={(e) => updateOption(index, { label: e.target.value })}
                          placeholder="例如：同意方案 A"
                        />
                      </div>
                      <div className="flex flex-col gap-1.5">
                        <Label>說明（選填）</Label>
                        <Input
                          value={opt.description ?? ''}
                          onChange={(e) => updateOption(index, { description: e.target.value })}
                          placeholder="例如：方案 A 內容"
                        />
                      </div>
                    </div>
                    <div className="mt-3 flex items-center justify-between">
                      <label className="flex items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={opt.passOption}
                          onChange={(e) => updateOption(index, { passOption: e.target.checked })}
                        />
                        計入通過票數
                      </label>
                      {voteOptions.length > 1 && (
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          className="text-destructive"
                          onClick={() => removeOption(index)}
                        >
                          <Trash2 className="size-4" aria-hidden="true" />
                          移除
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))}
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

            <div className="grid gap-4 sm:grid-cols-3">
              <div className="flex flex-col gap-1.5">
                <Label>通過門檻（分子）</Label>
                <Input
                  type="number"
                  min={1}
                  value={passNumerator}
                  onChange={(e) => setPassNumerator(Number(e.target.value))}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label>通過門檻（分母）</Label>
                <Input
                  type="number"
                  min={1}
                  value={passDenominator}
                  onChange={(e) => setPassDenominator(Number(e.target.value))}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label>門檻基準</Label>
                <select
                  className="h-9 rounded-lg border border-input bg-transparent px-3 text-sm"
                  value={thresholdBase}
                  onChange={(e) => setThresholdBase(e.target.value as ThresholdBase)}
                >
                  <option value="ATTENDED">出席人數／權數</option>
                  <option value="COMMUNITY">全社區戶數／權數</option>
                </select>
              </div>
            </div>
            <p className="text-xs text-muted-foreground">
              通過條件：同意票（計入通過票數的選項）之人數比例與權數比例均須達 {passNumerator}/{passDenominator}
            </p>

            <div className="flex items-center justify-between rounded-lg border border-border p-3">
              <div>
                <p className="text-sm font-medium">允許重新投票</p>
                <p className="text-xs text-muted-foreground">投票進行中，住戶可更改已投選項</p>
              </div>
              <Switch checked={allowRevote} onCheckedChange={setAllowRevote} aria-label="允許重新投票" />
            </div>

            <div className="flex items-center justify-between rounded-lg border border-border p-3">
              <div>
                <p className="text-sm font-medium">前端顯示</p>
                <p className="text-xs text-muted-foreground">啟動投票時會自動設為顯示</p>
              </div>
              <Switch checked={visible} onCheckedChange={setVisible} aria-label="前端顯示" />
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
