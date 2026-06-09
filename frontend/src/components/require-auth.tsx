import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '@/context/auth-context'
import type { ReactNode } from 'react'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { session, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-muted-foreground">
        載入中…
      </div>
    )
  }

  if (!session) {
    return <Navigate to="/vote" replace state={{ from: location.pathname }} />
  }

  return children
}
