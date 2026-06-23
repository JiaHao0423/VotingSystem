import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import {
  Search,
  Plus,
  QrCode,
  CheckCircle2,
  Circle,
  Users,
  Upload,
  Pencil,
  Printer,
  Trash2,
} from 'lucide-react'
import { toast } from 'sonner'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { adminApi } from '@/lib/admin-api'
import { useAdminAuth } from '@/context/admin-auth-context'
import { OwnerQrCard } from '@/components/owner-qr-card'
import { Switch } from '@/components/ui/switch'
import { buildingTypeLabel } from '@/lib/labels'
import type { AdminOwner, AdminUnit, UpdateOwnerBody } from '@/lib/admin-types'

function findUnitByShortName(units: AdminUnit[], input: string): AdminUnit | undefined {
  const trimmed = input.trim()
  if (!trimmed) return undefined
  const lower = trimmed.toLowerCase()
  return units.find((u) => u.shortName.toLowerCase() === lower)
}

function formatArea(area: number | null | undefined): string {
  if (area == null) return '—'
  return `${area} 坪`
}

function formatRatio(ratio: number | null | undefined): string {
  if (ratio == null) return '—'
  return `${Number(ratio).toLocaleString('zh-TW', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  })}%`
}

function ownerToForm(owner: AdminOwner): UpdateOwnerBody {
  return {
    name: owner.name,
    phone: owner.phone ?? '',
    attended: owner.attended,
    unitShortName: owner.unitShortName,
    fullAddress: owner.fullAddress,
    buildingType: owner.buildingType,
    floor: owner.floor,
    unitNo: owner.unitNo,
    shopNo: owner.shopNo,
    area: owner.area,
    ownershipRatio: owner.ownershipRatio,
  }
}

function parseOptionalNumber(value: string): number | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  const n = Number(trimmed)
  return Number.isFinite(n) ? n : null
}

function isParseableUnitShortName(input: string): boolean {
  const trimmed = input.trim()
  return /^\d+[AB]\d+$/i.test(trimmed) || /^店\d+$/i.test(trimmed)
}

export function AdminOwnersPage() {
  const { community } = useAdminAuth()
  const [owners, setOwners] = useState<AdminOwner[]>([])
  const [units, setUnits] = useState<AdminUnit[]>([])
  const [query, setQuery] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [newUnitShortName, setNewUnitShortName] = useState('')
  const [newName, setNewName] = useState('')
  const [newPhone, setNewPhone] = useState('')
  const [newFullAddress, setNewFullAddress] = useState('')
  const [newArea, setNewArea] = useState('')
  const [newOwnershipRatio, setNewOwnershipRatio] = useState('')
  const [detailOwner, setDetailOwner] = useState<AdminOwner | null>(null)
  const [editingOwner, setEditingOwner] = useState<AdminOwner | null>(null)
  const [editForm, setEditForm] = useState<UpdateOwnerBody | null>(null)
  const [qrModal, setQrModal] = useState<{
    name: string
    unit: string
    ownershipRatio: number | null
    qrUrl: string
  } | null>(null)
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())
  const [batchDeleting, setBatchDeleting] = useState(false)
  const [batchAttending, setBatchAttending] = useState(false)

  const matchedUnit = useMemo(
    () => findUnitByShortName(units, newUnitShortName),
    [units, newUnitShortName],
  )
  const needsNewUnit = Boolean(newUnitShortName.trim() && !matchedUnit)
  const canParseShortName = isParseableUnitShortName(newUnitShortName)

  const reload = useCallback(async () => {
    if (!community) return
    const [o, u] = await Promise.all([
      adminApi.listOwners(community.id),
      adminApi.listUnits(community.id),
    ])
    setOwners(o)
    setUnits(u)
  }, [community])

  useEffect(() => {
    reload()
  }, [reload])

  const filtered = owners.filter(
    (o) =>
      o.name.includes(query) ||
      o.unitShortName.includes(query) ||
      o.fullAddress.includes(query) ||
      (o.phone ?? '').includes(query) ||
      String(o.id).includes(query),
  )
  const attended = owners.filter((o) => o.attended).length
  const selectedCount = selectedIds.size
  const allFilteredSelected =
    filtered.length > 0 && filtered.every((o) => selectedIds.has(o.id))
  const someFilteredSelected =
    filtered.some((o) => selectedIds.has(o.id)) && !allFilteredSelected

  useEffect(() => {
    setSelectedIds((prev) => {
      const valid = new Set(owners.map((o) => o.id))
      const next = new Set<number>()
      prev.forEach((id) => {
        if (valid.has(id)) next.add(id)
      })
      return next.size === prev.size ? prev : next
    })
  }, [owners])

  function toggleSelect(ownerId: number) {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(ownerId)) next.delete(ownerId)
      else next.add(ownerId)
      return next
    })
  }

  function toggleSelectAllFiltered() {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (allFilteredSelected) {
        filtered.forEach((o) => next.delete(o.id))
      } else {
        filtered.forEach((o) => next.add(o.id))
      }
      return next
    })
  }

  function clearSelection() {
    setSelectedIds(new Set())
  }

  async function createOwner() {
    if (!community || !newUnitShortName.trim() || !newName.trim()) {
      toast.error('請輸入戶別簡稱與姓名')
      return
    }
    if (matchedUnit?.hasOwner) {
      toast.error('此戶別已有所有權人，請先編輯現有資料')
      return
    }
    if (needsNewUnit) {
      if (!canParseShortName) {
        toast.error('戶別簡稱格式不正確，請使用如 12B9、4A7、店1')
        return
      }
      const area = parseOptionalNumber(newArea)
      const ownershipRatio = parseOptionalNumber(newOwnershipRatio)
      if (!newFullAddress.trim()) {
        toast.error('請填寫完整門牌')
        return
      }
      if (area == null || area <= 0) {
        toast.error('請填寫有效的坪數')
        return
      }
      if (ownershipRatio == null || ownershipRatio <= 0) {
        toast.error('請填寫有效的區分所有權比例')
        return
      }
    }
    try {
      const area = parseOptionalNumber(newArea)
      const ownershipRatio = parseOptionalNumber(newOwnershipRatio)
      const created = await adminApi.createOwner(community.id, {
        unitShortName: newUnitShortName.trim(),
        name: newName.trim(),
        phone: newPhone.trim() || undefined,
        ...(needsNewUnit
          ? {
              fullAddress: newFullAddress.trim(),
              area: area ?? undefined,
              ownershipRatio: ownershipRatio ?? undefined,
            }
          : {}),
      })
      setQrModal({
        name: created.owner.name,
        unit: created.owner.unitShortName,
        ownershipRatio: created.owner.ownershipRatio,
        qrUrl: created.qrUrl,
      })
      toast.success('所有權人已新增，請列印 QR Code')
      setShowForm(false)
      setNewUnitShortName('')
      setNewName('')
      setNewPhone('')
      setNewFullAddress('')
      setNewArea('')
      setNewOwnershipRatio('')
      reload()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '新增失敗')
    }
  }

  function openEdit(owner: AdminOwner) {
    setEditingOwner(owner)
    setEditForm(ownerToForm(owner))
  }

  async function saveEdit() {
    if (!community || !editingOwner || !editForm) return
    if (!editForm.name.trim() || !editForm.unitShortName.trim() || !editForm.fullAddress.trim()) {
      toast.error('請填寫姓名、戶別簡稱與完整門牌')
      return
    }
    if (editForm.area == null || editForm.area <= 0) {
      toast.error('請填寫有效的坪數')
      return
    }
    if (editForm.ownershipRatio == null || editForm.ownershipRatio <= 0) {
      toast.error('請填寫有效的區分所有權比例')
      return
    }
    try {
      await adminApi.updateOwner(community.id, editingOwner.id, {
        ...editForm,
        name: editForm.name.trim(),
        phone: editForm.phone?.trim() || undefined,
        unitShortName: editForm.unitShortName.trim(),
        fullAddress: editForm.fullAddress.trim(),
      })
      toast.success('所有權人資料已更新')
      setEditingOwner(null)
      setEditForm(null)
      reload()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '更新失敗')
    }
  }

  async function toggleAttendance(owner: AdminOwner, attended: boolean) {
    if (!community) return
    try {
      await adminApi.updateOwnerAttendance(community.id, owner.id, attended)
      setOwners((prev) => prev.map((o) => (o.id === owner.id ? { ...o, attended } : o)))
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '更新出席狀態失敗')
      reload()
    }
  }

  async function batchSetAttendance(attended: boolean) {
    if (!community || selectedCount === 0) return
    const ids = [...selectedIds]
    setBatchAttending(true)
    try {
      const res = await adminApi.batchUpdateAttendance(community.id, ids, attended)
      toast.success(`已更新 ${res.updatedCount} 位所有權人的出席狀態`)
      clearSelection()
      reload()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '批次更新失敗')
    } finally {
      setBatchAttending(false)
    }
  }

  async function removeSelectedOwners() {
    if (!community || selectedCount === 0) return
    const ids = [...selectedIds]
    const ok = window.confirm(
      `確定要刪除已選的 ${ids.length} 位所有權人？\n相關 QR Code 與投票紀錄將一併刪除，戶別資料仍會保留。`,
    )
    if (!ok) return
    setBatchDeleting(true)
    try {
      const res = await adminApi.deleteOwners(community.id, ids)
      toast.success(`已刪除 ${res.deletedCount} 位所有權人`)
      if (detailOwner && selectedIds.has(detailOwner.id)) setDetailOwner(null)
      if (editingOwner && selectedIds.has(editingOwner.id)) {
        setEditingOwner(null)
        setEditForm(null)
      }
      clearSelection()
      reload()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '批次刪除失敗')
    } finally {
      setBatchDeleting(false)
    }
  }

  async function removeOwner(owner: AdminOwner) {
    if (!community) return
    const ok = window.confirm(
      `確定要刪除 ${owner.name}（${owner.unitShortName}）？\n相關 QR Code 與投票紀錄將一併刪除，戶別資料仍會保留。`,
    )
    if (!ok) return
    try {
      await adminApi.deleteOwner(community.id, owner.id)
      toast.success('所有權人已刪除')
      if (detailOwner?.id === owner.id) setDetailOwner(null)
      if (editingOwner?.id === owner.id) {
        setEditingOwner(null)
        setEditForm(null)
      }
      reload()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '刪除失敗')
    }
  }

  async function showQr(owner: AdminOwner) {
    if (!community) return
    try {
      const qr = await adminApi.getQrCode(community.id, owner.id)
      setQrModal({
        name: qr.ownerName,
        unit: qr.unitShortName,
        ownershipRatio: owner.ownershipRatio,
        qrUrl: qr.qrUrl,
      })
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '取得 QR 失敗')
    }
  }

  const summary = [
    { label: '所有權人總數', value: owners.length, icon: Users },
    { label: '已出席', value: attended, icon: CheckCircle2 },
    { label: '未出席', value: owners.length - attended, icon: Circle },
    {
      label: '區分所有權總計',
      value: `${owners.reduce((sum, o) => sum + Number(o.area ?? 0), 0).toLocaleString()} 坪`,
      icon: Users,
    },
  ]

  const isShop = editForm?.buildingType === 'SHOP'

  return (
    <div className="mx-auto w-full max-w-6xl">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-foreground">所有權人管理</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            建立所有權人時會產生永久 QR Code，列印後供住戶掃描報到登入
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link to="/admin/owners/print-qr">
            <Button variant="outline">
              <Printer className="size-4" aria-hidden="true" />
              列印全部 QR Code
            </Button>
          </Link>
          <Link to="/admin/units/import">
            <Button variant="outline">
              <Upload className="size-4" aria-hidden="true" />
              Excel 匯入
            </Button>
          </Link>
          <Button onClick={() => setShowForm((v) => !v)}>
            <Plus className="size-4" aria-hidden="true" />
            新增所有權人
          </Button>
        </div>
      </div>

      {showForm && (
        <Card className="mt-4">
          <CardContent className="flex flex-col gap-4 pt-6">
            <div className="grid gap-3 sm:grid-cols-3">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="new-unit">戶別簡稱</Label>
                <Input
                  id="new-unit"
                  placeholder="例如 4A7、12B9、店1"
                  value={newUnitShortName}
                  onChange={(e) => setNewUnitShortName(e.target.value)}
                />
                <p className="text-xs text-muted-foreground">
                  若戶別尚未建立，填寫下方戶別資料後將一併建立
                </p>
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="new-name">姓名</Label>
                <Input
                  id="new-name"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="new-phone">手機（選填）</Label>
                <Input
                  id="new-phone"
                  placeholder="09xxxxxxxx"
                  value={newPhone}
                  onChange={(e) => setNewPhone(e.target.value)}
                />
              </div>
            </div>
            {needsNewUnit && (
              <div className="grid gap-3 sm:grid-cols-3">
                <div className="flex flex-col gap-1.5 sm:col-span-3">
                  <Label htmlFor="new-address">完整門牌</Label>
                  <Input
                    id="new-address"
                    placeholder="例如 台中市北屯區… 12F-9"
                    value={newFullAddress}
                    onChange={(e) => setNewFullAddress(e.target.value)}
                  />
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="new-area">坪數</Label>
                  <Input
                    id="new-area"
                    type="number"
                    step="0.01"
                    value={newArea}
                    onChange={(e) => setNewArea(e.target.value)}
                  />
                </div>
                <div className="flex flex-col gap-1.5 sm:col-span-2">
                  <Label htmlFor="new-ratio">區分所有權比例 (%)</Label>
                  <Input
                    id="new-ratio"
                    type="number"
                    step="0.0001"
                    value={newOwnershipRatio}
                    onChange={(e) => setNewOwnershipRatio(e.target.value)}
                  />
                </div>
              </div>
            )}
            {newUnitShortName.trim() && (
              <div className="rounded-lg border border-border bg-muted/40 px-3 py-2 text-sm">
                {matchedUnit ? (
                  <p className="text-muted-foreground">
                    已找到戶別：{matchedUnit.fullAddress} ·{' '}
                    {buildingTypeLabel(matchedUnit.buildingType)} · {formatArea(matchedUnit.area)} · 比例{' '}
                    {formatRatio(matchedUnit.ownershipRatio)}
                    {matchedUnit.hasOwner && (
                      <span className="ml-2 text-destructive">（此戶別已有所有權人）</span>
                    )}
                  </p>
                ) : canParseShortName ? (
                  <p className="text-primary">
                    此戶別尚未建立，填寫上方戶別資料後將一併建立戶別與所有權人
                  </p>
                ) : (
                  <p className="text-destructive">
                    無法解析戶別簡稱，請使用如 12B9、4A7、店1
                  </p>
                )}
              </div>
            )}
            <div className="flex justify-end">
              <Button onClick={createOwner}>建立</Button>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {summary.map((s) => (
          <Card key={s.label}>
            <CardContent className="flex items-center gap-3 pt-6">
              <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <s.icon className="size-5" aria-hidden="true" />
              </div>
              <div>
                <p className="text-lg font-black text-foreground">{s.value}</p>
                <p className="text-xs text-muted-foreground">{s.label}</p>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="mt-6 flex flex-wrap items-center gap-3">
        <div className="relative max-w-sm flex-1">
          <Search
            className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
            aria-hidden="true"
          />
          <Input
            placeholder="搜尋姓名、戶別、門牌或手機"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="pl-9"
          />
        </div>
        {selectedCount > 0 && (
          <div className="flex flex-wrap items-center gap-2 rounded-lg border border-primary/30 bg-primary/5 px-3 py-2">
            <span className="text-sm font-medium text-foreground">已選 {selectedCount} 位</span>
            <Button
              variant="outline"
              size="sm"
              disabled={batchAttending}
              onClick={() => batchSetAttendance(true)}
            >
              設為已出席
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={batchAttending}
              onClick={() => batchSetAttendance(false)}
            >
              設為未出席
            </Button>
            <Button
              variant="destructive"
              size="sm"
              disabled={batchDeleting}
              onClick={removeSelectedOwners}
            >
              <Trash2 className="size-4" aria-hidden="true" />
              {batchDeleting ? '刪除中…' : '刪除所選'}
            </Button>
            <Button variant="ghost" size="sm" onClick={clearSelection}>
              取消選取
            </Button>
          </div>
        )}
      </div>

      <div className="mt-4 flex flex-col gap-3 md:hidden">
        {filtered.map((o) => (
          <Card key={o.id}>
            <CardContent className="p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="font-medium text-foreground">{o.name}</p>
                  <p className="text-sm text-muted-foreground">{o.unitShortName}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{o.phone || '—'}</p>
                </div>
                {o.attended ? (
                  <Badge className="shrink-0 border-chart-3/30 bg-chart-3/10 text-chart-3">
                    已出席
                  </Badge>
                ) : (
                  <Badge className="shrink-0 bg-muted text-muted-foreground">未出席</Badge>
                )}
              </div>
              <div className="mt-3 flex items-center justify-between">
                <span className="text-xs text-muted-foreground">出席狀態</span>
                <Switch
                  checked={o.attended}
                  onCheckedChange={(checked) => toggleAttendance(o, checked)}
                  aria-label={`${o.name} 出席狀態`}
                />
              </div>
              <div className="mt-3 flex flex-wrap gap-2 text-xs text-muted-foreground">
                <span>坪數 {o.area != null ? o.area : '—'}</span>
                <span>比例 {formatRatio(o.ownershipRatio)}</span>
              </div>
              <div className="mt-3 flex flex-wrap gap-1.5">
                <Button variant="outline" size="sm" onClick={() => setDetailOwner(o)}>
                  詳情
                </Button>
                <Button variant="outline" size="sm" onClick={() => openEdit(o)}>
                  <Pencil className="size-3.5" aria-hidden="true" />
                  編輯
                </Button>
                <Button variant="outline" size="sm" onClick={() => showQr(o)}>
                  <QrCode className="size-3.5" aria-hidden="true" />
                  QR
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  className="text-destructive hover:text-destructive"
                  onClick={() => removeOwner(o)}
                >
                  <Trash2 className="size-3.5" aria-hidden="true" />
                  刪除
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
        {filtered.length === 0 && (
          <p className="py-10 text-center text-sm text-muted-foreground">查無符合條件的所有權人</p>
        )}
      </div>

      <Card className="mt-4 hidden md:block">
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-10">
                  <input
                    type="checkbox"
                    checked={allFilteredSelected}
                    ref={(el) => {
                      if (el) el.indeterminate = someFilteredSelected
                    }}
                    onChange={toggleSelectAllFiltered}
                    aria-label="全選目前列表"
                    className="size-4 accent-primary"
                  />
                </TableHead>
                <TableHead>姓名</TableHead>
                <TableHead>戶別</TableHead>
                <TableHead className="text-right">坪數</TableHead>
                <TableHead className="text-right">所有權比例</TableHead>
                <TableHead>手機</TableHead>
                <TableHead className="text-center">出席</TableHead>
                <TableHead className="sticky right-0 bg-card text-right shadow-[-8px_0_12px_-8px_rgba(0,0,0,0.15)]">
                  操作
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((o) => (
                <TableRow
                  key={o.id}
                  className={`cursor-pointer hover:bg-muted/50 ${selectedIds.has(o.id) ? 'bg-primary/5' : ''}`}
                  onClick={() => setDetailOwner(o)}
                >
                  <TableCell onClick={(e) => e.stopPropagation()}>
                    <input
                      type="checkbox"
                      checked={selectedIds.has(o.id)}
                      onChange={() => toggleSelect(o.id)}
                      aria-label={`選取 ${o.name}`}
                      className="size-4 accent-primary"
                    />
                  </TableCell>
                  <TableCell className="font-medium">{o.name}</TableCell>
                  <TableCell className="text-muted-foreground">{o.unitShortName}</TableCell>
                  <TableCell className="text-right text-muted-foreground">
                    {o.area != null ? o.area : '—'}
                  </TableCell>
                  <TableCell className="text-right text-sm tabular-nums text-muted-foreground">
                    {formatRatio(o.ownershipRatio)}
                  </TableCell>
                  <TableCell className="text-muted-foreground">{o.phone || '—'}</TableCell>
                  <TableCell className="text-center" onClick={(e) => e.stopPropagation()}>
                    <Switch
                      checked={o.attended}
                      onCheckedChange={(checked) => toggleAttendance(o, checked)}
                      aria-label={`${o.name} 出席狀態`}
                    />
                  </TableCell>
                  <TableCell
                    className="sticky right-0 bg-card text-right shadow-[-8px_0_12px_-8px_rgba(0,0,0,0.15)]"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <div className="flex flex-wrap justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => openEdit(o)}
                        aria-label="編輯"
                      >
                        <Pencil className="size-4" aria-hidden="true" />
                      </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => showQr(o)}
                          aria-label="報到 QR Code"
                        >
                          <QrCode className="size-4" aria-hidden="true" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => removeOwner(o)}
                          aria-label="刪除"
                        >
                          <Trash2 className="size-4 text-destructive" aria-hidden="true" />
                        </Button>
                      </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {filtered.length === 0 && (
            <p className="py-10 text-center text-sm text-muted-foreground">查無符合條件的所有權人</p>
          )}
        </CardContent>
      </Card>

      {detailOwner && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          onClick={() => setDetailOwner(null)}
        >
          <Card className="w-full max-w-lg" onClick={(e) => e.stopPropagation()}>
            <CardContent className="flex flex-col gap-4 pt-6">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-lg font-bold">{detailOwner.name}</h2>
                  <p className="text-sm text-muted-foreground">編號 {detailOwner.id}</p>
                </div>
                <Badge className="border-border bg-muted text-muted-foreground">
                  {detailOwner.attended ? '已出席' : '未出席'}
                </Badge>
              </div>
              <dl className="grid gap-3 text-sm sm:grid-cols-2">
                <DetailItem label="戶別簡稱" value={detailOwner.unitShortName} />
                <DetailItem label="棟別" value={buildingTypeLabel(detailOwner.buildingType)} />
                <DetailItem label="完整門牌" value={detailOwner.fullAddress} className="sm:col-span-2" />
                {detailOwner.buildingType === 'SHOP' ? (
                  <DetailItem label="店面序號" value={detailOwner.shopNo != null ? String(detailOwner.shopNo) : '—'} />
                ) : (
                  <>
                    <DetailItem label="樓層" value={detailOwner.floor != null ? String(detailOwner.floor) : '—'} />
                    <DetailItem label="戶號" value={detailOwner.unitNo != null ? String(detailOwner.unitNo) : '—'} />
                  </>
                )}
                <DetailItem label="坪數" value={formatArea(detailOwner.area)} />
                <DetailItem
                  label="區分所有權比例"
                  value={formatRatio(detailOwner.ownershipRatio)}
                  valueClassName="tabular-nums"
                />
                <DetailItem label="手機" value={detailOwner.phone || '—'} />
              </dl>
              <div className="flex justify-end gap-2">
                <Button
                  variant="outline"
                  className="text-destructive hover:text-destructive"
                  onClick={() => {
                    removeOwner(detailOwner)
                  }}
                >
                  <Trash2 className="size-4" aria-hidden="true" />
                  刪除
                </Button>
                <Button
                  variant="outline"
                  onClick={() => {
                    openEdit(detailOwner)
                    setDetailOwner(null)
                  }}
                >
                  編輯
                </Button>
                <Button onClick={() => setDetailOwner(null)}>關閉</Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {editingOwner && editForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <Card className="max-h-[90vh] w-full max-w-2xl overflow-y-auto">
            <CardContent className="flex flex-col gap-4 pt-6">
              <h2 className="text-lg font-bold">編輯所有權人</h2>
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="姓名" required>
                  <Input
                    value={editForm.name}
                    onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                  />
                </Field>
                <Field label="手機">
                  <Input
                    placeholder="09xxxxxxxx"
                    value={editForm.phone ?? ''}
                    onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
                  />
                </Field>
                <Field label="戶別簡稱" required>
                  <Input
                    value={editForm.unitShortName}
                    onChange={(e) => setEditForm({ ...editForm, unitShortName: e.target.value })}
                  />
                </Field>
                <Field label="棟別" required>
                  <select
                    className="h-9 w-full rounded-lg border border-input bg-transparent px-3 text-sm"
                    value={editForm.buildingType}
                    onChange={(e) =>
                      setEditForm({ ...editForm, buildingType: e.target.value })
                    }
                  >
                    <option value="A">A 棟</option>
                    <option value="B">B 棟</option>
                    <option value="SHOP">店面</option>
                  </select>
                </Field>
                <Field label="完整門牌" required className="sm:col-span-2">
                  <Input
                    value={editForm.fullAddress}
                    onChange={(e) => setEditForm({ ...editForm, fullAddress: e.target.value })}
                  />
                </Field>
                {isShop ? (
                  <Field label="店面序號" required>
                    <Input
                      type="number"
                      value={editForm.shopNo ?? ''}
                      onChange={(e) =>
                        setEditForm({ ...editForm, shopNo: parseOptionalNumber(e.target.value) })
                      }
                    />
                  </Field>
                ) : (
                  <>
                    <Field label="樓層" required>
                      <Input
                        type="number"
                        value={editForm.floor ?? ''}
                        onChange={(e) =>
                          setEditForm({ ...editForm, floor: parseOptionalNumber(e.target.value) })
                        }
                      />
                    </Field>
                    <Field label="戶號" required>
                      <Input
                        type="number"
                        value={editForm.unitNo ?? ''}
                        onChange={(e) =>
                          setEditForm({ ...editForm, unitNo: parseOptionalNumber(e.target.value) })
                        }
                      />
                    </Field>
                  </>
                )}
                <Field label="坪數" required>
                  <Input
                    type="number"
                    step="0.01"
                    value={editForm.area ?? ''}
                    onChange={(e) =>
                      setEditForm({ ...editForm, area: parseOptionalNumber(e.target.value) })
                    }
                  />
                </Field>
                <Field label="區分所有權比例" required>
                  <Input
                    type="number"
                    step="0.0001"
                    value={editForm.ownershipRatio ?? ''}
                    onChange={(e) =>
                      setEditForm({
                        ...editForm,
                        ownershipRatio: parseOptionalNumber(e.target.value),
                      })
                    }
                  />
                </Field>
              </div>
              <div className="flex justify-end gap-2">
                <Button
                  variant="outline"
                  onClick={() => {
                    setEditingOwner(null)
                    setEditForm(null)
                  }}
                >
                  取消
                </Button>
                <Button onClick={saveEdit}>儲存</Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {qrModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 print:bg-white">
          <Card className="w-full max-w-sm print:border-0 print:shadow-none">
            <CardContent className="flex flex-col gap-4 pt-6">
              <h2 className="text-center text-lg font-bold print:hidden">報到 QR Code</h2>
              <OwnerQrCard
                ownerName={qrModal.name}
                unitShortName={qrModal.unit}
                ownershipRatio={qrModal.ownershipRatio}
                qrUrl={qrModal.qrUrl}
              />
              <Button onClick={() => setQrModal(null)} className="mt-2 print:hidden">
                關閉
              </Button>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}

function DetailItem({
  label,
  value,
  className,
  valueClassName,
}: {
  label: string
  value: string
  className?: string
  valueClassName?: string
}) {
  return (
    <div className={className}>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className={`mt-0.5 font-medium text-foreground ${valueClassName ?? ''}`}>{value}</dd>
    </div>
  )
}

function Field({
  label,
  children,
  required,
  className,
}: {
  label: string
  children: ReactNode
  required?: boolean
  className?: string
}) {
  return (
    <div className={`flex flex-col gap-1.5 ${className ?? ''}`}>
      <Label>
        {label}
        {required && <span className="text-destructive"> *</span>}
      </Label>
      {children}
    </div>
  )
}
