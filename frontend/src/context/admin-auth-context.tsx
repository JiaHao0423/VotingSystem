import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { adminApi, getAdminCredentials, setAdminCredentials } from '@/lib/admin-api'
import type { Community } from '@/lib/admin-types'

const STORAGE_KEY = 'voting-admin-creds'

interface AdminAuthContextValue {
  community: Community | null
  loading: boolean
  isAuthenticated: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  refreshCommunity: () => Promise<void>
}

const AdminAuthContext = createContext<AdminAuthContextValue | null>(null)

function loadStoredCreds(): { username: string; password: string } | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as { username: string; password: string }
    if (parsed.username && parsed.password) return parsed
  } catch {
    // ignore
  }
  return null
}

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  const [community, setCommunity] = useState<Community | null>(null)
  const [loading, setLoading] = useState(true)
  const [authenticated, setAuthenticated] = useState(false)

  const refreshCommunity = useCallback(async () => {
    const c = await adminApi.getCommunity()
    setCommunity(c)
  }, [])

  const restore = useCallback(async () => {
    const stored = loadStoredCreds()
    if (!stored) {
      setAuthenticated(false)
      setCommunity(null)
      return
    }
    setAdminCredentials(stored)
    try {
      const c = await adminApi.getCommunity()
      await adminApi.listProposals(c.id)
      setCommunity(c)
      setAuthenticated(true)
    } catch {
      setAdminCredentials(null)
      sessionStorage.removeItem(STORAGE_KEY)
      setAuthenticated(false)
      setCommunity(null)
    }
  }, [])

  useEffect(() => {
    restore().finally(() => setLoading(false))
  }, [restore])

  const login = useCallback(async (username: string, password: string) => {
    const c = await adminApi.verifyLogin(username, password)
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ username, password }))
    setCommunity(c)
    setAuthenticated(true)
  }, [])

  const logout = useCallback(() => {
    setAdminCredentials(null)
    sessionStorage.removeItem(STORAGE_KEY)
    setAuthenticated(false)
    setCommunity(null)
  }, [])

  const value = useMemo(
    () => ({
      community,
      loading,
      isAuthenticated: authenticated && !!getAdminCredentials(),
      login,
      logout,
      refreshCommunity,
    }),
    [community, loading, authenticated, login, logout, refreshCommunity],
  )

  return <AdminAuthContext.Provider value={value}>{children}</AdminAuthContext.Provider>
}

export function useAdminAuth() {
  const ctx = useContext(AdminAuthContext)
  if (!ctx) throw new Error('useAdminAuth must be used within AdminAuthProvider')
  return ctx
}
