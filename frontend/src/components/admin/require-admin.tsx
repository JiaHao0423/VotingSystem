import { Navigate, useLocation } from 'react-router-dom'
import { useAdminAuth } from '@/context/admin-auth-context'
import type { ReactNode } from 'react'

export function RequireAdmin({ children }: { children: ReactNode }) {
  const { isAuthenticated, loading } = useAdminAuth()
  const location = useLocation()

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-muted-foreground">
        載入中…
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/admin/login" replace state={{ from: location.pathname }} />
  }

  return children
}
