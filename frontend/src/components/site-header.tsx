import { Link } from 'react-router-dom'
import { Building2 } from 'lucide-react'
import { COMMUNITY_NAME } from '@/lib/labels'

export function SiteHeader({
  subtitle,
  communityName,
}: {
  subtitle?: string
  communityName?: string
}) {
  return (
    <header className="sticky top-0 z-30 border-b border-border bg-primary text-primary-foreground">
      <div className="mx-auto flex w-full max-w-3xl items-center gap-3 px-4 py-3">
        <Link
          to="/"
          className="flex size-9 shrink-0 items-center justify-center rounded-md bg-primary-foreground/10"
        >
          <Building2 className="size-5" aria-hidden="true" />
        </Link>
        <div className="min-w-0">
          <p className="truncate text-sm font-bold leading-tight">{communityName ?? COMMUNITY_NAME}</p>
          <p className="truncate text-xs text-primary-foreground/70">{subtitle ?? '區分所有權人會議'}</p>
        </div>
      </div>
    </header>
  )
}
