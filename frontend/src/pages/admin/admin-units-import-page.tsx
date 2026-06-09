import { useRef, useState } from 'react'
import { Download, Upload, FileSpreadsheet, AlertCircle, FileCheck, X } from 'lucide-react'
import { toast } from 'sonner'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { adminApi } from '@/lib/admin-api'
import { useAdminAuth } from '@/context/admin-auth-context'
import type { ImportDuplicatePolicy, UnitImportResult } from '@/lib/admin-types'

function statusBadgeClass(status: string): string {
  switch (status) {
    case 'CREATED':
      return 'bg-chart-3/10 text-chart-3'
    case 'UPDATED':
      return 'bg-primary/10 text-primary'
    case 'SKIPPED':
      return 'bg-muted text-muted-foreground'
    default:
      return 'bg-chart-5/10 text-chart-5'
  }
}

export function AdminUnitsImportPage() {
  const { community } = useAdminAuth()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [createOwners, setCreateOwners] = useState(true)
  const [duplicatePolicy, setDuplicatePolicy] = useState<ImportDuplicatePolicy>('SKIP')
  const [loading, setLoading] = useState(false)
  const [loadingMode, setLoadingMode] = useState<'preview' | 'import' | null>(null)
  const [result, setResult] = useState<UnitImportResult | null>(null)

  function handleFileSelect(selected: File | null) {
    setFile(selected)
    setResult(null)
  }

  async function downloadTemplate() {
    if (!community) return
    try {
      await adminApi.downloadUnitImportTemplate(community.id)
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '下載失敗')
    }
  }

  async function runImport(preview: boolean) {
    if (!community || !file) {
      toast.error('請先選擇 Excel 檔案')
      return
    }
    if (!preview) {
      const ok = window.confirm(
        '確定要正式匯入並寫入資料庫嗎？建議先按「預覽匯入」確認內容無誤。',
      )
      if (!ok) return
    }
    setLoading(true)
    setLoadingMode(preview ? 'preview' : 'import')
    try {
      const res = await adminApi.importUnits(community.id, file, {
        dryRun: preview,
        createOwners,
        duplicatePolicy,
      })
      setResult(res)
      toast.success(preview ? '預覽完成' : '匯入完成')
    } catch (err) {
      toast.error(err instanceof Error ? err.message : '匯入失敗')
    } finally {
      setLoading(false)
      setLoadingMode(null)
    }
  }

  return (
    <div className="mx-auto w-full max-w-5xl">
      <h1 className="text-2xl font-bold text-foreground">Excel 戶別批次匯入</h1>
      <p className="mt-1 text-sm text-muted-foreground">
        支援區權會名冊格式（戶別、姓名、門牌號碼或詳細門牌、區分所有權人比例），店面 S1 會自動轉為店1
      </p>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <FileSpreadsheet className="size-4 text-primary" aria-hidden="true" />
            匯入步驟
          </CardTitle>
          <CardDescription>建議先下載範本、填寫後以「預覽」確認，再正式匯入</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <ol className="list-decimal space-y-1 pl-5 text-sm text-muted-foreground">
            <li>可直接上傳「名冊及區權比」Excel（欄位：戶別、姓名、門牌號碼、區分所有權人比例）</li>
            <li>區分所有權比例支援「94.60 / 13241.21」格式，系統會自動換算為百分比（小數點後兩位）</li>
            <li>店面戶別 S1、S2 會自動轉為店1、店2；住宅如 2A1、12B9 可直接匯入</li>
            <li>建議先預覽，確認無誤後再正式匯入並建立所有權人與 QR Code</li>
          </ol>

          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={downloadTemplate}>
              <Download className="size-4" aria-hidden="true" />
              下載範本
            </Button>
          </div>

          <div className="flex flex-col gap-4 rounded-lg border border-border p-4">
            <div>
              <p className="text-sm font-medium">步驟 1：選擇 Excel 檔案</p>
              <input
                ref={fileInputRef}
                type="file"
                accept=".xlsx,.xls"
                className="sr-only"
                onChange={(e) => handleFileSelect(e.target.files?.[0] ?? null)}
              />
              <div
                role="button"
                tabIndex={0}
                onClick={() => fileInputRef.current?.click()}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault()
                    fileInputRef.current?.click()
                  }
                }}
                className="mt-2 flex cursor-pointer flex-col items-center gap-3 rounded-xl border-2 border-dashed border-primary/40 bg-primary/5 px-6 py-8 transition-colors hover:border-primary hover:bg-primary/10"
              >
                <FileSpreadsheet className="size-10 text-primary" aria-hidden="true" />
                <div className="text-center">
                  <p className="font-medium text-foreground">點此選擇 Excel 檔案</p>
                  <p className="mt-1 text-sm text-muted-foreground">支援 .xlsx、.xls 格式</p>
                </div>
                <Button
                  type="button"
                  size="lg"
                  onClick={(e) => {
                    e.stopPropagation()
                    fileInputRef.current?.click()
                  }}
                >
                  <Upload className="size-4" aria-hidden="true" />
                  選擇檔案
                </Button>
              </div>
              {file && (
                <div className="mt-3 flex items-center gap-2 rounded-lg border border-chart-3/30 bg-chart-3/5 px-3 py-2">
                  <FileCheck className="size-4 shrink-0 text-chart-3" aria-hidden="true" />
                  <span className="min-w-0 flex-1 truncate text-sm font-medium">{file.name}</span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    aria-label="移除檔案"
                    onClick={() => {
                      handleFileSelect(null)
                      if (fileInputRef.current) fileInputRef.current.value = ''
                    }}
                  >
                    <X className="size-4" aria-hidden="true" />
                  </Button>
                </div>
              )}
            </div>

            <div>
              <p className="text-sm font-medium">步驟 2：匯入選項</p>
              <label className="mt-2 flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={createOwners}
                  onChange={(e) => setCreateOwners(e.target.checked)}
                  className="size-4 accent-primary"
                />
                同時建立或更新所有權人（需填寫姓名欄）
              </label>

              <div className="mt-3 flex flex-col gap-2">
                <span className="text-sm font-medium">遇到已存在戶別時</span>
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="radio"
                    name="duplicatePolicy"
                    checked={duplicatePolicy === 'SKIP'}
                    onChange={() => setDuplicatePolicy('SKIP')}
                    className="accent-primary"
                  />
                  保留原資料（略過）
                </label>
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="radio"
                    name="duplicatePolicy"
                    checked={duplicatePolicy === 'REPLACE'}
                    onChange={() => setDuplicatePolicy('REPLACE')}
                    className="accent-primary"
                  />
                  以 Excel 取代（更新門牌、坪數、比例與所有權人）
                </label>
              </div>
            </div>

            <div>
              <p className="text-sm font-medium">步驟 3：執行匯入</p>
              <div className="mt-2 flex flex-wrap gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="lg"
                  disabled={loading || !file}
                  onClick={() => runImport(true)}
                >
                  <FileSpreadsheet className="size-4" aria-hidden="true" />
                  {loadingMode === 'preview' ? '預覽中…' : '預覽匯入'}
                </Button>
                <Button
                  type="button"
                  size="lg"
                  disabled={loading || !file}
                  onClick={() => runImport(false)}
                >
                  <Upload className="size-4" aria-hidden="true" />
                  {loadingMode === 'import' ? '匯入中…' : '正式匯入'}
                </Button>
              </div>
              <p className="mt-2 text-xs text-muted-foreground">
                「預覽匯入」不會寫入資料庫；確認無誤後再按「正式匯入」。
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2 rounded-lg bg-secondary/80 p-3 text-xs text-muted-foreground">
            <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            <p>
              預覽時若選擇「取代」，狀態會顯示 UPDATED 供確認。正式匯入後可至所有權人管理列印 QR Code。
            </p>
          </div>
        </CardContent>
      </Card>

      {result && (
        <Card className="mt-6">
          <CardHeader>
            <CardTitle className="text-base">{result.dryRun ? '預覽結果' : '匯入結果'}</CardTitle>
            <CardDescription>
              共 {result.totalRows} 列 · 新增 {result.createdUnits} 戶 · 更新 {result.updatedUnits}{' '}
              戶 · 略過 {result.skippedUnits} 戶 · 所有權人 {result.createdOwners} 人 · 錯誤{' '}
              {result.errorCount} 筆
            </CardDescription>
          </CardHeader>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>列號</TableHead>
                  <TableHead>戶別</TableHead>
                  <TableHead>狀態</TableHead>
                  <TableHead>說明</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {result.rows.map((row) => (
                  <TableRow key={`${row.rowNumber}-${row.shortName}`}>
                    <TableCell>{row.rowNumber}</TableCell>
                    <TableCell className="font-medium">{row.shortName || '—'}</TableCell>
                    <TableCell>
                      <Badge className={statusBadgeClass(row.status)}>{row.status}</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{row.message}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
