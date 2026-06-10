import { Link } from 'react-router-dom'
import { Building2, QrCode, ClipboardList, ArrowRight } from 'lucide-react'
import { SiteHeader } from '@/components/site-header'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { SYSTEM_NAME } from '@/lib/labels'

const flow = [
  { icon: QrCode, label: '掃描 QR Code', href: '/vote' },
  { icon: ClipboardList, label: '查看提案並投票', href: '/proposals' },
]

export function HomePage() {
  return (
    <div className="min-h-screen bg-background">
      <SiteHeader subtitle="區權會提案投票工具" />
      <main className="mx-auto w-full max-w-md px-4 py-8">
        <div className="flex flex-col items-center text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <Building2 className="size-8" aria-hidden="true" />
          </div>
          <h1 className="mt-4 text-2xl font-bold text-foreground">{SYSTEM_NAME}</h1>
          <p className="mt-2 text-pretty text-sm leading-relaxed text-muted-foreground">
            高效、透明、公正且符合法規的區分所有權人會議電子投票平台
          </p>
        </div>

        <Card className="mt-6">
          <CardContent className="p-4">
            <p className="text-sm font-medium text-foreground">投票流程</p>
            <ol className="mt-3 flex flex-col gap-3">
              {flow.map((step, i) => (
                <li key={step.label} className="flex items-center gap-3">
                  <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                    {i + 1}
                  </span>
                  <step.icon className="size-4 text-muted-foreground" aria-hidden="true" />
                  <span className="text-sm text-foreground">{step.label}</span>
                </li>
              ))}
            </ol>
            <Link to="/vote" className="mt-4 block">
              <Button className="w-full">
                開始投票
                <ArrowRight className="size-4" aria-hidden="true" />
              </Button>
            </Link>
          </CardContent>
        </Card>

        <p className="mt-6 text-center text-xs text-muted-foreground">
          請掃描管理員核發的永久 QR Code 完成報到後進行投票
        </p>
        <p className="mt-2 text-center text-sm">
          <Link to="/admin/login" className="text-primary hover:underline">
            管理後台登入 →
          </Link>
        </p>
      </main>
    </div>
  )
}
