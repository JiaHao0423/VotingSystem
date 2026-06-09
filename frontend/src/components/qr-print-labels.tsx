export function formatOwnershipRatio(ratio: number | null | undefined): string {
  if (ratio == null) return '—'
  return `${Number(ratio).toLocaleString('zh-TW', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  })}%`
}

export interface QrPrintLabelsProps {
  ownerName: string
  unitShortName: string
  ownershipRatio?: number | null
}

export function QrPrintLabels({ ownerName, unitShortName, ownershipRatio }: QrPrintLabelsProps) {
  return (
    <div className="qr-print-labels space-y-0.5 text-sm leading-snug">
      <p className="qr-print-line">
        <span className="qr-print-field">所有權人:</span>
        <span className="qr-print-value">{ownerName}</span>
      </p>
      <p className="qr-print-line">
        <span className="qr-print-field">戶別:</span>
        <span className="qr-print-value">{unitShortName}</span>
      </p>
      <p className="qr-print-line">
        <span className="qr-print-field">所有權比例:</span>
        <span className="qr-print-value tabular-nums">{formatOwnershipRatio(ownershipRatio)}</span>
      </p>
    </div>
  )
}
