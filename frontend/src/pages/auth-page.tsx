import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { QrCode, ShieldCheck, Info, Loader2, UserCheck } from 'lucide-react'
import { toast } from 'sonner'
import { SiteHeader } from '@/components/site-header'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { api } from '@/lib/api'
import { useAuth } from '@/context/auth-context'
import { buildingTypeLabel } from '@/lib/labels'
import type { QrPreview } from '@/lib/types'

export function AuthPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { session, setSession } = useAuth()
  const [preview, setPreview] = useState<QrPreview | null>(null)
  const [previewError, setPreviewError] = useState('')
  const [loadingPreview, setLoadingPreview] = useState(false)
  const [confirming, setConfirming] = useState(false)

  const qrToken = searchParams.get('t')

  useEffect(() => {
    if (session) {
      navigate('/proposals', { replace: true })
    }
  }, [session, navigate])

  useEffect(() => {
    if (!qrToken) return
    setLoadingPreview(true)
    setPreviewError('')
    api
      .previewQr(qrToken)
      .then(setPreview)
      .catch((err: Error) => setPreviewError(err.message || '無效的 QR Code'))
      .finally(() => setLoadingPreview(false))
  }, [qrToken])

  function confirmLogin() {
    if (!qrToken) return
    setConfirming(true)
    api
      .verifyQr(qrToken)
      .then((me) => {
        setSession(me)
        toast.success(me.message || '報到成功')
        navigate('/proposals', { replace: true })
      })
      .catch((err: Error) => toast.error(err.message || '登入失敗'))
      .finally(() => setConfirming(false))
  }

  if (qrToken && loadingPreview) {
    return (
      <div className="flex min-h-screen items-center justify-center gap-2 text-sm text-muted-foreground">
        <Loader2 className="size-4 animate-spin" />
        讀取住戶資料中…
      </div>
    )
  }

  if (qrToken && preview) {
    return (
      <div className="min-h-screen bg-background">
        <SiteHeader subtitle="QR Code 報到登入" />
        <main className="mx-auto w-full max-w-md px-4 py-8">
          <div className="flex flex-col items-center text-center">
            <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10 text-primary">
              <UserCheck className="size-8" aria-hidden="true" />
            </div>
            <h1 className="mt-4 text-2xl font-bold text-foreground">確認身份</h1>
            <p className="mt-1 text-pretty text-sm leading-relaxed text-muted-foreground">
              請確認以下資料為您本人，確認後將完成報到並進入投票。
            </p>
          </div>

          <Card className="mt-6">
            <CardHeader>
              <CardTitle className="text-base">{preview.ownerName}</CardTitle>
              <CardDescription>所有權人報到資訊</CardDescription>
            </CardHeader>
            <CardContent>
              <dl className="flex flex-col gap-3 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">戶別</dt>
                  <dd className="font-medium text-foreground">{preview.unitShortName}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">棟別</dt>
                  <dd className="font-medium text-foreground">
                    {buildingTypeLabel(preview.buildingType)}
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="shrink-0 text-muted-foreground">門牌</dt>
                  <dd className="text-right font-medium text-foreground">{preview.fullAddress}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">坪數</dt>
                  <dd className="font-medium text-foreground">
                    {preview.area != null ? `${preview.area} 坪` : '—'}
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">區分所有權比例</dt>
                  <dd className="font-medium tabular-nums text-foreground">
                    {preview.ownershipRatio != null
                      ? `${Number(preview.ownershipRatio).toLocaleString('zh-TW', {
                          minimumFractionDigits: 2,
                          maximumFractionDigits: 4,
                        })}%`
                      : '—'}
                  </dd>
                </div>
              </dl>
              <Button
                onClick={confirmLogin}
                disabled={confirming}
                className="mt-6 w-full"
              >
                {confirming && <Loader2 className="size-4 animate-spin" aria-hidden="true" />}
                {confirming ? '登入中…' : '確認報到並進入投票'}
              </Button>
            </CardContent>
          </Card>
        </main>
      </div>
    )
  }

  if (qrToken && previewError) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 px-4 text-center">
        <p className="text-sm text-destructive">{previewError}</p>
        <Link to="/vote" className="text-sm text-primary hover:underline">
          返回登入頁
        </Link>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      <SiteHeader subtitle="QR Code 報到登入" />
      <main className="mx-auto w-full max-w-md px-4 py-8">
        <div className="flex flex-col items-center text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <QrCode className="size-8" aria-hidden="true" />
          </div>
          <h1 className="mt-4 text-2xl font-bold text-foreground">掃描報到</h1>
          <p className="mt-1 text-pretty text-sm leading-relaxed text-muted-foreground">
            請使用管理員核發的永久 QR Code 掃描登入。掃描後將顯示您的住戶資料供確認，即可進入投票。
          </p>
        </div>

        <Card className="mt-6">
          <CardContent className="flex flex-col items-center gap-3 pt-6 text-center">
            <QrCode className="size-12 text-muted-foreground/50" aria-hidden="true" />
            <p className="text-sm text-muted-foreground">
              請以手機相機或 QR 掃描 App 掃描報到單上的 QR Code
            </p>
          </CardContent>
        </Card>

        <div className="mt-4 flex items-start gap-2 rounded-lg border border-border bg-secondary/60 p-3 text-xs leading-relaxed text-muted-foreground">
          <Info className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" />
          <p>
            每位所有權人持專屬永久 QR Code 報到，系統將自動記錄出席並防止重複投票。
            您的個人資料與投票紀錄將以加密方式儲存與傳輸。
          </p>
        </div>

        <Separator className="my-6" />

        <div className="flex items-center justify-between rounded-lg border border-dashed border-border p-4">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <ShieldCheck className="size-4 text-primary" aria-hidden="true" />
            已完成報到？
          </div>
          <Link to="/proposals" className="text-sm font-medium text-primary hover:underline">
            直接進入
          </Link>
        </div>
      </main>
    </div>
  )
}
