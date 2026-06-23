import type { ProposalStatus, ProposalType, ThresholdBase } from './types'

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

export function thresholdBaseLabel(base: ThresholdBase): string {
  return base === 'ATTENDED' ? '出席人數' : '全社區'
}

export function thresholdLabel(numerator: number, denominator: number): string {
  return `${numerator}/${denominator}`
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

export const DEFAULT_VOTE_OPTIONS = [
  { label: '同意', description: '', passOption: true },
  { label: '反對', description: '', passOption: false },
  { label: '棄權', description: '', passOption: false },
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

export function formatOwnershipRatio(ratio: number | null | undefined): string {
  if (ratio == null) return '—'
  return `${Number(ratio).toLocaleString('zh-TW', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  })}%`
}
