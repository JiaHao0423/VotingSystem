import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  Loader2,
  ShieldCheck,
} from 'lucide-react'
import { toast } from 'sonner'
import { SiteHeader } from '@/components/site-header'
import { TypeBadge } from '@/components/status-badge'
import { VoterInfoCard } from '@/components/voter-info-card'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { cn } from '@/lib/utils'
import { api } from '@/lib/api'
import { useAuth } from '@/context/auth-context'
import { usePolling } from '@/hooks/use-polling'
import type { ProposalSummary } from '@/lib/types'

export function VotePage() {
  const { id } = useParams<{ id: string }>()
  const proposalId = Number(id)
  const navigate = useNavigate()
  const { session } = useAuth()
  const [proposal, setProposal] = useState<ProposalSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [selected, setSelected] = useState('')
  const [step, setStep] = useState<'vote' | 'confirm'>('vote')
  const [submitting, setSubmitting] = useState(false)
  const [isRevote, setIsRevote] = useState(false)

  useEffect(() => {
    if (!proposalId) return
    api
      .getProposal(proposalId)
      .then((p) => {
        if (p.hasVoted && !p.allowRevote) {
          toast.info('您已對此提案投票')
          navigate(`/proposals/${p.id}/result`, { replace: true })
          return
        }
        if (p.status !== 'ACTIVE') {
          toast.error('此提案目前無法投票')
          navigate('/proposals', { replace: true })
          return
        }
        setIsRevote(p.hasVoted && p.allowRevote)
        setProposal(p)
      })
      .catch((err: Error) => {
        toast.error(err.message || '無法載入提案')
        navigate('/proposals', { replace: true })
      })
      .finally(() => setLoading(false))
  }, [proposalId, navigate])

  const pollProposal = useCallback(async () => {
    if (!proposalId || step === 'confirm' || submitting) return
    try {
      const p = await api.getProposal(proposalId)
      if (p.hasVoted && !p.allowRevote) {
        toast.info('您已對此提案投票')
        navigate(`/proposals/${p.id}/result`, { replace: true })
        return
      }
      if (p.status !== 'ACTIVE') {
        toast.info('此提案已結束或暫停投票')
        navigate('/proposals', { replace: true })
        return
      }
      setIsRevote(p.hasVoted && p.allowRevote)
      setProposal(p)
    } catch {
      // ignore transient errors during polling
    }
  }, [proposalId, navigate, step, submitting])

  usePolling(pollProposal, 5000, !loading && !!proposal)

  function selectedLabel() {
    if (!proposal || !selected) return ''
    return proposal.voteOptions.find((o) => o.key === selected)?.label ?? selected
  }

  function proceed() {
    if (!selected) {
      toast.error('請先選擇一個選項')
      return
    }
    setStep('confirm')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function submit() {
    if (!selected || !proposal) return
    setSubmitting(true)
    api
      .submitVote(proposal.id, selected)
      .then(() => {
        navigate(`/proposals/${proposal.id}/result?voted=1`)
      })
      .catch((err: Error) => toast.error(err.message || '投票失敗'))
      .finally(() => setSubmitting(false))
  }

  if (loading || !proposal || !session) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-muted-foreground">
        載入中…
      </div>
    )
  }

  const voteOptions = [...proposal.voteOptions].sort((a, b) => a.sortOrder - b.sortOrder)

  return (
    <div className="min-h-screen bg-background pb-28">
      <SiteHeader subtitle={step === 'vote' ? '投票' : '確認投票'} />
      <main className="mx-auto w-full max-w-md px-4 py-5">
        <Link
          to="/proposals"
          className="mb-2 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          返回提案列表
        </Link>

        <div className="mb-4">
          <VoterInfoCard session={session} />
        </div>

        <div className="mb-4 flex items-center gap-2 text-xs font-medium">
          <span className={cn('flex items-center gap-1.5', step === 'vote' ? 'text-primary' : 'text-chart-3')}>
            <span
              className={cn(
                'flex size-5 items-center justify-center rounded-full text-[11px]',
                step === 'vote' ? 'bg-primary text-primary-foreground' : 'bg-chart-3 text-white',
              )}
            >
              {step === 'confirm' ? <CheckCircle2 className="size-3.5" /> : '1'}
            </span>
            選擇
          </span>
          <span className="h-px flex-1 bg-border" />
          <span
            className={cn(
              'flex items-center gap-1.5',
              step === 'confirm' ? 'text-primary' : 'text-muted-foreground',
            )}
          >
            <span
              className={cn(
                'flex size-5 items-center justify-center rounded-full text-[11px]',
                step === 'confirm' ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground',
              )}
            >
              2
            </span>
            確認
          </span>
        </div>

        <Card>
          <CardContent className="p-4">
            <TypeBadge type={proposal.type} />
            <p className="mt-2 text-xs font-medium text-muted-foreground">{proposal.proposalNumber}</p>
            <h1 className="mt-0.5 text-pretty text-lg font-bold leading-snug text-foreground">{proposal.title}</h1>
            <Separator className="my-3" />
            <p className="text-sm leading-relaxed text-muted-foreground">{proposal.content}</p>
          </CardContent>
        </Card>

        {step === 'vote' ? (
          <section className="mt-5" aria-label="投票選項">
            <h2 className="mb-2 text-sm font-bold text-foreground">
              {isRevote ? '請重新選擇您的決定' : '請選擇您的決定'}
            </h2>
            <div className="flex flex-col gap-2.5">
              {voteOptions.map((opt) => (
                <Label
                  key={opt.key}
                  className={cn(
                    'flex cursor-pointer flex-col gap-1 rounded-lg border bg-card p-4 transition-colors',
                    selected === opt.key
                      ? 'border-primary bg-primary/5 ring-1 ring-primary'
                      : 'border-border hover:bg-secondary',
                  )}
                >
                  <div className="flex items-center gap-3">
                    <input
                      type="radio"
                      name="vote"
                      value={opt.key}
                      checked={selected === opt.key}
                      onChange={() => setSelected(opt.key)}
                      className="size-4 accent-primary"
                    />
                    <span className="text-base font-medium text-foreground">{opt.label}</span>
                  </div>
                  {opt.description && (
                    <p className="pl-7 text-sm text-muted-foreground">{opt.description}</p>
                  )}
                </Label>
              ))}
            </div>
            <p className="mt-3 flex items-center gap-1.5 text-xs text-muted-foreground">
              <AlertCircle className="size-3.5" aria-hidden="true" />
              {proposal.allowRevote
                ? '本提案為單選，投票進行中可重新更改選擇。'
                : '本提案為單選，送出後將無法更改。'}
            </p>
          </section>
        ) : (
          <section className="mt-5" aria-label="確認投票">
            <Card className="border-primary/30 bg-primary/5">
              <CardContent className="p-4">
                <p className="text-sm text-muted-foreground">您即將投下的選擇</p>
                <p className="mt-1 text-2xl font-black text-primary">{selectedLabel()}</p>
                <Separator className="my-3" />
                <dl className="flex flex-col gap-1.5 text-sm">
                  <div className="flex justify-between">
                    <dt className="text-muted-foreground">投票人</dt>
                    <dd className="font-medium text-foreground">
                      {session.name}（{session.unitShortName}）
                    </dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-muted-foreground">區分所有權比例</dt>
                    <dd className="font-medium text-foreground">
                      {Number(session.ownershipRatio ?? 0).toFixed(2)}%
                    </dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-muted-foreground">表決權數（坪）</dt>
                    <dd className="font-medium text-foreground">{Number(session.area ?? 0).toLocaleString()}</dd>
                  </div>
                </dl>
              </CardContent>
            </Card>
            <p className="mt-3 flex items-start gap-1.5 text-xs leading-relaxed text-muted-foreground">
              <ShieldCheck className="mt-0.5 size-3.5 shrink-0 text-primary" aria-hidden="true" />
              {proposal.allowRevote
                ? '請再次確認您的選擇。送出後系統將記錄投票時間與內容。'
                : '請再次確認您的選擇。送出後系統將記錄投票時間與內容，且無法重複投票。'}
            </p>
          </section>
        )}
      </main>

      <div className="fixed inset-x-0 bottom-0 border-t border-border bg-card/95 backdrop-blur">
        <div className="mx-auto flex w-full max-w-md gap-3 px-4 py-3">
          {step === 'vote' ? (
            <Button onClick={proceed} className="w-full" size="lg">
              下一步：確認選擇
            </Button>
          ) : (
            <>
              <Button variant="outline" onClick={() => setStep('vote')} className="flex-1" size="lg">
                返回修改
              </Button>
              <Button onClick={submit} disabled={submitting} className="flex-1" size="lg">
                {submitting && <Loader2 className="size-4 animate-spin" aria-hidden="true" />}
                {submitting ? '送出中…' : isRevote ? '確認更改' : '確認送出'}
              </Button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
