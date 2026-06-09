import { QRCodeSVG } from 'qrcode.react'
import { Printer } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { QrPrintLabels } from '@/components/qr-print-labels'

export interface OwnerQrCardProps {
  ownerName: string
  unitShortName: string
  ownershipRatio?: number | null
  qrUrl: string
  showPrint?: boolean
}

export function OwnerQrCard({
  ownerName,
  unitShortName,
  ownershipRatio,
  qrUrl,
  showPrint = true,
}: OwnerQrCardProps) {
  function handlePrint() {
    window.print()
  }

  return (
    <div className="owner-qr-card flex flex-col items-center gap-4">
      <div className="qr-print-block rounded-xl border border-border bg-white p-4 print:border-0 print:p-0">
        <QRCodeSVG value={qrUrl} size={200} level="M" includeMargin className="qr-print-code" />
        <QrPrintLabels
          ownerName={ownerName}
          unitShortName={unitShortName}
          ownershipRatio={ownershipRatio}
        />
      </div>
      <p className="max-w-xs text-xs text-muted-foreground print:hidden">
        此 QR Code 永久有效，請列印後供住戶報到掃描，即可登入投票。
      </p>
      {showPrint && (
        <Button variant="outline" onClick={handlePrint} className="print:hidden">
          <Printer className="size-4" aria-hidden="true" />
          列印 QR Code
        </Button>
      )}
    </div>
  )
}
