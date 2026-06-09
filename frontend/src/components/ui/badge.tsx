import { cn } from '@/lib/utils'
import type { HTMLAttributes } from 'react'

export function Badge({ className, ...props }: HTMLAttributes<HTMLSpanElement>) {
  return (
    <span
      className={cn(
        'inline-flex h-5 items-center rounded-full border px-2 text-xs font-medium',
        className,
      )}
      {...props}
    />
  )
}
