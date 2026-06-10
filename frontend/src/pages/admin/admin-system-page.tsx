import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import {
  Building2,
  KeyRound,
  Loader2,
  LogIn,
  Plus,
  ShieldCheck,
  Trash2,
  UserPlus,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { adminApi } from '@/lib/admin-api'
import { useAdminAuth } from '@/context/admin-auth-context'
import type { AdminAccount, Community } from '@/lib/admin-types'

export function AdminSystemPage() {
  const navigate = useNavigate()
  const { me, selectCommunity } = useAdminAuth()
  const [communities, setCommunities] = useState<Community[]>([])
  const [accounts, setAccounts] = useState<AdminAccount[]>([])
  const [loading, setLoading] = useState(true)

  const [communityForm, setCommunityForm] = useState({
    name: '',
    totalHouseholds: '',
    address: '',
  })
  const [creatingCommunity, setCreatingCommunity] = useState(false)
  const [showCommunityForm, setShowCommunityForm] = useState(false)

  const [accountForm, setAccountForm] = useState({
    username: '',
    password: '',
    displayName: '',
    communityId: '',
  })
  const [creatingAccount, setCreatingAccount] = useState(false)
  const [showAccountForm, setShowAccountForm] = useState(false)

  const reload = useCallback(async () => {
    const [communityList, accountList] = await Promise.all([
      adminApi.listSystemCommunities(),
      adminApi.listAdminAccounts(),
    ])
    setCommunities(communityList)
    setAccounts(accountList)
  }, [])

  useEffect(() => {
    reload()
      .catch((err) => toast.error(err instanceof Error ? err.message : '載入失敗'))
      .finally(() => setLoading(false))
  }, [reload])

  function enterCommunity(c: Community) {
    selectCommunity(c)
    navigate('/admin')
  }

  async function createCommunity(e: React.FormEvent) {
    e.preventDefault()
    const households = Number(communityForm.totalHouseholds)
    if (!communityForm.name.trim()) {
      toast.error('請輸入社區名稱')
      return
    }
    if (!Number.isInteger(households) || households <= 0) {
      toast.error('總戶數必須為正整數')
      return
    }
    setCreatingCommunity(true)
    try {
      await adminApi.createCommunity({
        name: communityForm.name.trim(),
        totalHouseholds: households,
        address: communityForm.address.trim() || undefined,
      })
      toast.success('社區已建立')
      setCommunityForm({ name: '', totalHouseholds: '', address: '' })
      setShowCommunityForm(false)
      await reload()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '建立失敗')
    } finally {
      setCreatingCommunity(false)
    }
  }

  async function createAccount(e: React.FormEvent) {
    e.preventDefault()
    if (!accountForm.communityId) {
      toast.error('請選擇管理的社區')
      return
    }
    setCreatingAccount(true)
    try {
      await adminApi.createAdminAccount({
        username: accountForm.username.trim(),
        password: accountForm.password,
        displayName: accountForm.displayName.trim() || undefined,
        role: 'COMMUNITY_ADMIN',
        communityId: Number(accountForm.communityId),
      })
      toast.success('管理帳號已建立')
      setAccountForm({ username: '', password: '', displayName: '', communityId: '' })
      setShowAccountForm(false)
      await reload()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '建立失敗')
    } finally {
      setCreatingAccount(false)
    }
  }

  async function removeAccount(account: AdminAccount) {
    const ok = window.confirm(`確定要刪除管理帳號「${account.username}」嗎？`)
    if (!ok) return
    try {
      await adminApi.deleteAdminAccount(account.id)
      toast.success('帳號已刪除')
      await reload()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '刪除失敗')
    }
  }

  async function resetPassword(account: AdminAccount) {
    const password = window.prompt(`請輸入「${account.username}」的新密碼（至少 6 字元）`)
    if (!password) return
    if (password.length < 6) {
      toast.error('密碼長度需至少 6 字元')
      return
    }
    try {
      await adminApi.resetAdminPassword(account.id, password)
      toast.success('密碼已重設')
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '重設失敗')
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center gap-2 py-16 text-sm text-muted-foreground">
        <Loader2 className="size-4 animate-spin" aria-hidden="true" />
        載入中…
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-bold">系統管理</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          建立社區與社區管理帳號，各社區資料彼此獨立、由各自的管理帳號維護。
        </p>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-start justify-between gap-4">
          <div>
            <CardTitle className="flex items-center gap-2 text-base">
              <Building2 className="size-4 text-primary" aria-hidden="true" />
              社區管理
            </CardTitle>
            <CardDescription>共 {communities.length} 個社區</CardDescription>
          </div>
          <Button size="sm" onClick={() => setShowCommunityForm((v) => !v)}>
            <Plus className="size-4" aria-hidden="true" />
            新增社區
          </Button>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {showCommunityForm && (
            <form
              onSubmit={createCommunity}
              className="grid gap-3 rounded-lg border border-border p-4 sm:grid-cols-2"
            >
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="community-name">社區名稱 *</Label>
                <Input
                  id="community-name"
                  value={communityForm.name}
                  onChange={(e) => setCommunityForm((f) => ({ ...f, name: e.target.value }))}
                  placeholder="例：幸福華廈"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="community-households">總戶數 *</Label>
                <Input
                  id="community-households"
                  type="number"
                  min={1}
                  value={communityForm.totalHouseholds}
                  onChange={(e) =>
                    setCommunityForm((f) => ({ ...f, totalHouseholds: e.target.value }))
                  }
                  placeholder="例：155"
                />
              </div>
              <div className="flex flex-col gap-1.5 sm:col-span-2">
                <Label htmlFor="community-address">地址</Label>
                <Input
                  id="community-address"
                  value={communityForm.address}
                  onChange={(e) => setCommunityForm((f) => ({ ...f, address: e.target.value }))}
                  placeholder="社區地址（選填）"
                />
              </div>
              <div className="sm:col-span-2">
                <Button type="submit" disabled={creatingCommunity}>
                  {creatingCommunity && (
                    <Loader2 className="size-4 animate-spin" aria-hidden="true" />
                  )}
                  建立社區
                </Button>
              </div>
            </form>
          )}

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>社區名稱</TableHead>
                <TableHead>總戶數</TableHead>
                <TableHead>地址</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {communities.map((c) => (
                <TableRow key={c.id}>
                  <TableCell className="font-medium">{c.name}</TableCell>
                  <TableCell>{c.totalHouseholds}</TableCell>
                  <TableCell className="text-muted-foreground">{c.address || '—'}</TableCell>
                  <TableCell className="text-right">
                    <Button variant="outline" size="sm" onClick={() => enterCommunity(c)}>
                      <LogIn className="size-3.5" aria-hidden="true" />
                      管理此社區
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {communities.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} className="py-8 text-center text-muted-foreground">
                    尚未建立任何社區
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-start justify-between gap-4">
          <div>
            <CardTitle className="flex items-center gap-2 text-base">
              <ShieldCheck className="size-4 text-primary" aria-hidden="true" />
              管理帳號
            </CardTitle>
            <CardDescription>
              每個社區可建立專屬管理帳號，登入後僅能管理自己的社區。
            </CardDescription>
          </div>
          <Button size="sm" onClick={() => setShowAccountForm((v) => !v)}>
            <UserPlus className="size-4" aria-hidden="true" />
            新增帳號
          </Button>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {showAccountForm && (
            <form
              onSubmit={createAccount}
              className="grid gap-3 rounded-lg border border-border p-4 sm:grid-cols-2"
            >
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="account-username">帳號 *</Label>
                <Input
                  id="account-username"
                  value={accountForm.username}
                  onChange={(e) => setAccountForm((f) => ({ ...f, username: e.target.value }))}
                  placeholder="3-50 字元，限英數字與 _ . -"
                  autoComplete="off"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="account-password">密碼 *</Label>
                <Input
                  id="account-password"
                  type="password"
                  value={accountForm.password}
                  onChange={(e) => setAccountForm((f) => ({ ...f, password: e.target.value }))}
                  placeholder="至少 6 字元"
                  autoComplete="new-password"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="account-display-name">顯示名稱</Label>
                <Input
                  id="account-display-name"
                  value={accountForm.displayName}
                  onChange={(e) =>
                    setAccountForm((f) => ({ ...f, displayName: e.target.value }))
                  }
                  placeholder="例：王主委（選填）"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="account-community">管理的社區 *</Label>
                <select
                  id="account-community"
                  value={accountForm.communityId}
                  onChange={(e) =>
                    setAccountForm((f) => ({ ...f, communityId: e.target.value }))
                  }
                  className="h-9 rounded-md border border-input bg-transparent px-3 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
                >
                  <option value="">請選擇社區</option>
                  {communities.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="sm:col-span-2">
                <Button type="submit" disabled={creatingAccount}>
                  {creatingAccount && (
                    <Loader2 className="size-4 animate-spin" aria-hidden="true" />
                  )}
                  建立帳號
                </Button>
              </div>
            </form>
          )}

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>帳號</TableHead>
                <TableHead>顯示名稱</TableHead>
                <TableHead>角色</TableHead>
                <TableHead>所屬社區</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {accounts.map((account) => (
                <TableRow key={account.id}>
                  <TableCell className="font-medium">{account.username}</TableCell>
                  <TableCell>{account.displayName || '—'}</TableCell>
                  <TableCell>
                    {account.role === 'SUPER_ADMIN' ? (
                      <Badge className="border-primary/30 bg-primary/10 text-primary">
                        超級管理員
                      </Badge>
                    ) : (
                      <Badge className="bg-secondary text-secondary-foreground">
                        社區管理員
                      </Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {account.communityName || '全部社區'}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => resetPassword(account)}
                        title="重設密碼"
                      >
                        <KeyRound className="size-3.5" aria-hidden="true" />
                        重設密碼
                      </Button>
                      {account.id !== me?.id && (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-destructive hover:text-destructive"
                          onClick={() => removeAccount(account)}
                          title="刪除帳號"
                        >
                          <Trash2 className="size-3.5" aria-hidden="true" />
                          刪除
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  )
}
