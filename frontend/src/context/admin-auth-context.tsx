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
import type { AdminMe, Community } from '@/lib/admin-types'

const STORAGE_KEY = 'voting-admin-creds'
const ACTIVE_COMMUNITY_KEY = 'voting-admin-active-community'

interface AdminAuthContextValue {
  me: AdminMe | null
  community: Community | null
  loading: boolean
  isAuthenticated: boolean
  isSuperAdmin: boolean
  login: (username: string, password: string) => Promise<AdminMe>
  logout: () => void
  selectCommunity: (community: Community | null) => void
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

function loadStoredCommunity(): Community | null {
  try {
    const raw = sessionStorage.getItem(ACTIVE_COMMUNITY_KEY)
    if (!raw) return null
    return JSON.parse(raw) as Community
  } catch {
    return null
  }
}

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<AdminMe | null>(null)
  const [community, setCommunity] = useState<Community | null>(null)
  const [loading, setLoading] = useState(true)
  const [authenticated, setAuthenticated] = useState(false)

  const applyMe = useCallback((profile: AdminMe) => {
    setMe(profile)
    if (profile.community) {
      // 社區管理員固定綁定自己的社區
      setCommunity(profile.community)
    } else {
      // 超級管理員：還原先前選擇的社區
      setCommunity(loadStoredCommunity())
    }
    setAuthenticated(true)
  }, [])

  const restore = useCallback(async () => {
    const stored = loadStoredCreds()
    if (!stored) {
      setAuthenticated(false)
      setMe(null)
      setCommunity(null)
      return
    }
    setAdminCredentials(stored)
    try {
      const profile = await adminApi.getMe()
      applyMe(profile)
    } catch {
      setAdminCredentials(null)
      sessionStorage.removeItem(STORAGE_KEY)
      sessionStorage.removeItem(ACTIVE_COMMUNITY_KEY)
      setAuthenticated(false)
      setMe(null)
      setCommunity(null)
    }
  }, [applyMe])

  useEffect(() => {
    restore().finally(() => setLoading(false))
  }, [restore])

  const login = useCallback(
    async (username: string, password: string) => {
      const profile = await adminApi.verifyLogin(username, password)
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ username, password }))
      sessionStorage.removeItem(ACTIVE_COMMUNITY_KEY)
      applyMe(profile)
      return profile
    },
    [applyMe],
  )

  const logout = useCallback(() => {
    setAdminCredentials(null)
    sessionStorage.removeItem(STORAGE_KEY)
    sessionStorage.removeItem(ACTIVE_COMMUNITY_KEY)
    setAuthenticated(false)
    setMe(null)
    setCommunity(null)
  }, [])

  const selectCommunity = useCallback((next: Community | null) => {
    setCommunity(next)
    if (next) {
      sessionStorage.setItem(ACTIVE_COMMUNITY_KEY, JSON.stringify(next))
    } else {
      sessionStorage.removeItem(ACTIVE_COMMUNITY_KEY)
    }
  }, [])

  const value = useMemo(
    () => ({
      me,
      community,
      loading,
      isAuthenticated: authenticated && !!getAdminCredentials(),
      isSuperAdmin: me?.role === 'SUPER_ADMIN',
      login,
      logout,
      selectCommunity,
    }),
    [me, community, loading, authenticated, login, logout, selectCommunity],
  )

  return <AdminAuthContext.Provider value={value}>{children}</AdminAuthContext.Provider>
}

export function useAdminAuth() {
  const ctx = useContext(AdminAuthContext)
  if (!ctx) throw new Error('useAdminAuth must be used within AdminAuthProvider')
  return ctx
}
