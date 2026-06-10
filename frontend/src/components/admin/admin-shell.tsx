import { Link, Navigate, Outlet, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  FileText,
  BarChart3,
  Users,
  Upload,
  ShieldCheck,
  Building2,
  ArrowLeft,
  ArrowLeftRight,
  Settings,
  LogOut,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { useAdminAuth } from '@/context/admin-auth-context'
import { SYSTEM_NAME } from '@/lib/labels'

const communityNavItems = [
  { href: '/admin', label: '管理儀表板', icon: LayoutDashboard, exact: true },
  { href: '/admin/proposals', label: '提案管理', icon: FileText },
  { href: '/admin/results', label: '投票結果詳情', icon: BarChart3 },
  { href: '/admin/owners', label: '所有權人管理', icon: Users },
  { href: '/admin/units/import', label: '戶別匯入', icon: Upload },
]

export function AdminShell() {
  const pathname = useLocation().pathname
  const { me, community, isSuperAdmin, logout } = useAdminAuth()

  // 超級管理員尚未選擇社區時，先導向系統管理頁
  if (isSuperAdmin && !community && pathname !== '/admin/system') {
    return <Navigate to="/admin/system" replace />
  }

  const navItems = [
    ...(community ? communityNavItems : []),
    ...(isSuperAdmin
      ? [{ href: '/admin/system', label: '系統管理', icon: Settings, exact: true }]
      : []),
  ]

  const headerName = community?.name ?? SYSTEM_NAME
  const adminName = me?.displayName || me?.username || '管理員'

  return (
    <div className="flex min-h-screen bg-background">
      <aside
        className="hidden w-64 shrink-0 flex-col md:flex"
        style={{ background: 'var(--sidebar)', color: 'var(--sidebar-foreground)' }}
      >
        <div
          className="flex items-center gap-2 px-5 py-4"
          style={{ borderBottom: '1px solid var(--sidebar-border)' }}
        >
          <div
            className="flex size-9 items-center justify-center rounded-md"
            style={{ background: 'var(--sidebar-primary)', color: 'var(--sidebar-primary-foreground)' }}
          >
            <Building2 className="size-5" aria-hidden="true" />
          </div>
          <div className="min-w-0">
            <p className="truncate text-sm font-bold">{headerName}</p>
            <p className="text-xs opacity-60">後台管理系統</p>
          </div>
        </div>

        {isSuperAdmin && community && (
          <Link
            to="/admin/system"
            className="mx-3 mt-3 flex items-center gap-2 rounded-md border border-dashed px-3 py-2 text-xs opacity-70 hover:opacity-100"
            style={{ borderColor: 'var(--sidebar-border)' }}
          >
            <ArrowLeftRight className="size-3.5" aria-hidden="true" />
            切換社區 / 系統管理
          </Link>
        )}

        <nav className="flex flex-1 flex-col gap-1 p-3" aria-label="後台導覽">
          {navItems.map((item) => {
            const active = item.exact ? pathname === item.href : pathname.startsWith(item.href)
            return (
              <Link
                key={item.href}
                to={item.href}
                className={cn(
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                  active
                    ? 'bg-[var(--sidebar-accent)] text-[var(--sidebar-accent-foreground)]'
                    : 'opacity-70 hover:bg-[var(--sidebar-accent)]/50 hover:opacity-100',
                )}
              >
                <item.icon className="size-4" aria-hidden="true" />
                {item.label}
              </Link>
            )
          })}
        </nav>

        <div className="p-3" style={{ borderTop: '1px solid var(--sidebar-border)' }}>
          <div className="flex items-center gap-3 rounded-md px-3 py-2">
            <div
              className="flex size-8 items-center justify-center rounded-full text-xs font-bold"
              style={{ background: 'var(--sidebar-accent)' }}
            >
              {adminName.slice(0, 1)}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium">{adminName}</p>
              <p className="flex items-center gap-1 text-xs opacity-60">
                <ShieldCheck className="size-3" aria-hidden="true" />
                {isSuperAdmin ? '超級管理員' : '社區管理員'}
              </p>
            </div>
          </div>
          <Link
            to="/"
            className="mt-1 flex items-center gap-2 rounded-md px-3 py-2 text-xs opacity-60 hover:opacity-100"
          >
            <ArrowLeft className="size-3.5" aria-hidden="true" /> 返回住戶端
          </Link>
          <button
            type="button"
            onClick={logout}
            className="mt-1 flex w-full items-center gap-2 rounded-md px-3 py-2 text-xs opacity-60 hover:opacity-100"
          >
            <LogOut className="size-3.5" aria-hidden="true" /> 登出
          </button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <div
          className="flex items-center justify-between px-4 py-3 md:hidden"
          style={{ background: 'var(--sidebar)', color: 'var(--sidebar-foreground)' }}
        >
          <div className="flex items-center gap-2">
            <Building2 className="size-5" aria-hidden="true" />
            <span className="text-sm font-bold">{headerName}</span>
          </div>
          <Link to="/" className="text-xs opacity-70">
            住戶端
          </Link>
        </div>
        <nav
          className="flex gap-1 overflow-x-auto border-b border-border bg-card px-3 py-2 md:hidden"
          aria-label="後台導覽"
        >
          {navItems.map((item) => {
            const active = item.exact ? pathname === item.href : pathname.startsWith(item.href)
            return (
              <Link
                key={item.href}
                to={item.href}
                className={cn(
                  'flex shrink-0 items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-medium',
                  active ? 'bg-primary text-primary-foreground' : 'text-muted-foreground',
                )}
              >
                <item.icon className="size-3.5" aria-hidden="true" />
                {item.label}
              </Link>
            )
          })}
        </nav>
        <main className="flex-1 px-4 py-6 md:px-8 md:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
