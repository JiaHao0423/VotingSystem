import type { ProposalStatus, ProposalType, VoteChoice } from './types'

export function statusLabel(status: ProposalStatus): string {
  const map: Record<ProposalStatus, string> = {
    DRAFT: '草稿',
    SCHEDULED: '待開始',
    ACTIVE: '進行中',
    ENDED: '已結束',
  }
  return map[status]
}

export function typeLabel(type: ProposalType): string {
  return type === 'EXTRAORDINARY' ? '臨時提案' : '一般提案'
}

export function choiceLabel(choice: VoteChoice): string {
  const map: Record<VoteChoice, string> = {
    AGREE: '同意',
    DISAGREE: '反對',
    ABSTAIN: '棄權',
  }
  return map[choice]
}

export function formatDateTime(iso: string | null): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString('zh-TW', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

export function pct(ratio: number): string {
  return `${(ratio * 100).toFixed(1)}%`
}

export const VOTE_OPTIONS: { choice: VoteChoice; label: string }[] = [
  { choice: 'AGREE', label: '同意' },
  { choice: 'DISAGREE', label: '反對' },
  { choice: 'ABSTAIN', label: '棄權' },
]

export const SYSTEM_NAME = '社區電子投票系統'

export function buildingTypeLabel(type: string): string {
  const map: Record<string, string> = {
    A: 'A 棟',
    B: 'B 棟',
    SHOP: '店面',
  }
  return map[type] ?? type
}
