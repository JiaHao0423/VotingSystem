import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { QRCodeSVG } from 'qrcode.react'
import { ArrowLeft, Loader2, Printer } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { QrPrintLabels } from '@/components/qr-print-labels'
import { adminApi } from '@/lib/admin-api'
import { useAdminAuth } from '@/context/admin-auth-context'
import type { OwnerQrPrintItem } from '@/lib/admin-types'

const PER_PAGE = 8

function chunk<T>(items: T[], size: number): T[][] {
  const pages: T[][] = []
  for (let i = 0; i < items.length; i += size) {
    pages.push(items.slice(i, i + size))
  }
  return pages
}

function QrPrintCell({ item }: { item: OwnerQrPrintItem }) {
  return (
    <div className="qr-print-cell">
      <div className="qr-print-block">
        <QRCodeSVG value={item.qrUrl} size={110} level="M" includeMargin className="qr-print-code" />
        <QrPrintLabels
          ownerName={item.ownerName}
          unitShortName={item.unitShortName}
          ownershipRatio={item.ownershipRatio}
        />
      </div>
    </div>
  )
}

export function AdminOwnersPrintQrPage() {
  const { community } = useAdminAuth()
  const [items, setItems] = useState<OwnerQrPrintItem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!community) return
    adminApi
      .listOwnerQrPrintItems(community.id)
      .then(setItems)
      .catch((err: Error) => toast.error(err.message || '無法載入 QR 資料'))
      .finally(() => setLoading(false))
  }, [community])

  const pages = useMemo(() => chunk(items, PER_PAGE), [items])

  function handlePrint() {
    window.print()
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center gap-2 text-sm text-muted-foreground">
        <Loader2 className="size-4 animate-spin" />
        載入 QR Code 中…
      </div>
    )
  }

  return (
    <div className="qr-bulk-print-root min-h-screen bg-background">
      <div className="qr-print-toolbar mx-auto flex max-w-4xl items-center justify-between gap-3 px-4 py-4">
        <Link to="/admin/owners">
          <Button variant="outline" size="sm">
            <ArrowLeft className="size-4" aria-hidden="true" />
            返回所有權人管理
          </Button>
        </Link>
        <div className="text-sm text-muted-foreground">
          共 {items.length} 位 · {pages.length} 頁（每頁 {PER_PAGE} 個）
        </div>
        <Button onClick={handlePrint} disabled={items.length === 0}>
          <Printer className="size-4" aria-hidden="true" />
          列印全部
        </Button>
      </div>

      {items.length === 0 ? (
        <p className="py-20 text-center text-sm text-muted-foreground">尚無所有權人資料可列印</p>
      ) : (
        <div className="qr-bulk-print-pages mx-auto flex flex-col items-center gap-8 px-4 pb-10">
          {pages.map((pageItems, pageIndex) => (
            <section key={pageIndex} className="qr-print-sheet">
              {pageItems.map((item) => (
                <QrPrintCell key={item.ownerId} item={item} />
              ))}
            </section>
          ))}
        </div>
      )}
    </div>
  )
}
