import type { ProposalResult } from './types'
import type {
  AdminAccount,
  AdminMe,
  AdminOwner,
  AdminProposal,
  AdminResultDetail,
  AdminUnit,
  AuthCodeRegenerated,
  Community,
  CreateAdminAccountBody,
  CreateCommunityBody,
  CreateProposalBody,
  OwnerCreated,
  OwnerQrPrintItem,
  QrCodeInfo,
  UnitImportResult,
  UpdateOwnerBody,
} from './admin-types'

type Creds = { username: string; password: string }

let creds: Creds | null = null

export function setAdminCredentials(c: Creds | null) {
  creds = c
}

export function getAdminCredentials() {
  return creds
}

function authHeader(): string {
  if (!creds) throw new Error('尚未登入管理後台')
  return 'Basic ' + btoa(`${creds.username}:${creds.password}`)
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.ok) {
    if (res.status === 204) return undefined as T
    return res.json() as Promise<T>
  }
  let message = res.statusText
  try {
    const body = await res.json()
    message = body.message ?? body.error ?? message
  } catch {
    // ignore
  }
  throw new Error(message || `HTTP ${res.status}`)
}

async function adminFetch(path: string, init?: RequestInit) {
  return fetch(path, {
    ...init,
    headers: {
      Authorization: authHeader(),
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })
}

export const adminApi = {
  async verifyLogin(username: string, password: string): Promise<AdminMe> {
    const prev = creds
    creds = { username, password }
    try {
      return await this.getMe()
    } catch (e) {
      creds = prev
      throw e
    }
  },

  getMe(): Promise<AdminMe> {
    return adminFetch('/api/admin/me').then((r) => handleResponse<AdminMe>(r))
  },

  // ---- 超級管理員：社區與帳號管理 ----

  listSystemCommunities(): Promise<Community[]> {
    return adminFetch('/api/admin/system/communities').then((r) =>
      handleResponse<Community[]>(r),
    )
  },

  createCommunity(body: CreateCommunityBody): Promise<Community> {
    return adminFetch('/api/admin/system/communities', {
      method: 'POST',
      body: JSON.stringify(body),
    }).then((r) => handleResponse<Community>(r))
  },

  listAdminAccounts(): Promise<AdminAccount[]> {
    return adminFetch('/api/admin/system/admins').then((r) =>
      handleResponse<AdminAccount[]>(r),
    )
  },

  createAdminAccount(body: CreateAdminAccountBody): Promise<AdminAccount> {
    return adminFetch('/api/admin/system/admins', {
      method: 'POST',
      body: JSON.stringify(body),
    }).then((r) => handleResponse<AdminAccount>(r))
  },

  deleteAdminAccount(adminId: number): Promise<void> {
    return adminFetch(`/api/admin/system/admins/${adminId}`, {
      method: 'DELETE',
    }).then((r) => handleResponse<void>(r))
  },

  resetAdminPassword(adminId: number, password: string): Promise<void> {
    return adminFetch(`/api/admin/system/admins/${adminId}/password`, {
      method: 'PUT',
      body: JSON.stringify({ password }),
    }).then((r) => handleResponse<void>(r))
  },

  listProposals(communityId: number): Promise<AdminProposal[]> {
    return adminFetch(`/api/admin/communities/${communityId}/proposals`).then((r) =>
      handleResponse<AdminProposal[]>(r),
    )
  },

  getProposal(communityId: number, proposalId: number): Promise<AdminProposal> {
    return adminFetch(`/api/admin/communities/${communityId}/proposals/${proposalId}`).then((r) =>
      handleResponse<AdminProposal>(r),
    )
  },

  createProposal(communityId: number, body: CreateProposalBody): Promise<AdminProposal> {
    return adminFetch(`/api/admin/communities/${communityId}/proposals`, {
      method: 'POST',
      body: JSON.stringify(body),
    }).then((r) => handleResponse<AdminProposal>(r))
  },

  updateProposal(
    communityId: number,
    proposalId: number,
    body: CreateProposalBody,
  ): Promise<AdminProposal> {
    return adminFetch(`/api/admin/communities/${communityId}/proposals/${proposalId}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }).then((r) => handleResponse<AdminProposal>(r))
  },

  deleteProposal(communityId: number, proposalId: number): Promise<void> {
    return adminFetch(`/api/admin/communities/${communityId}/proposals/${proposalId}`, {
      method: 'DELETE',
    }).then((r) => handleResponse<void>(r))
  },

  startProposal(communityId: number, proposalId: number): Promise<AdminProposal> {
    return adminFetch(`/api/admin/communities/${communityId}/proposals/${proposalId}/start`, {
      method: 'POST',
      body: '{}',
    }).then((r) => handleResponse<AdminProposal>(r))
  },

  stopProposal(communityId: number, proposalId: number): Promise<AdminProposal> {
    return adminFetch(`/api/admin/communities/${communityId}/proposals/${proposalId}/stop`, {
      method: 'POST',
      body: '{}',
    }).then((r) => handleResponse<AdminProposal>(r))
  },

  getProposalResult(communityId: number, proposalId: number): Promise<ProposalResult> {
    return adminFetch(`/api/admin/communities/${communityId}/proposals/${proposalId}/results`).then(
      (r) => handleResponse<ProposalResult>(r),
    )
  },

  getProposalResultDetail(communityId: number, proposalId: number): Promise<AdminResultDetail> {
    return adminFetch(
      `/api/admin/communities/${communityId}/proposals/${proposalId}/results/detail`,
    ).then((r) => handleResponse<AdminResultDetail>(r))
  },

  listOwners(communityId: number): Promise<AdminOwner[]> {
    return adminFetch(`/api/admin/communities/${communityId}/owners`).then((r) =>
      handleResponse<AdminOwner[]>(r),
    )
  },

  listOwnerQrPrintItems(communityId: number): Promise<OwnerQrPrintItem[]> {
    return adminFetch(`/api/admin/communities/${communityId}/owners/qr/print-all`).then((r) =>
      handleResponse<OwnerQrPrintItem[]>(r),
    )
  },

  createOwner(
    communityId: number,
    body: {
      unitId?: number
      unitShortName?: string
      name: string
      phone?: string
      fullAddress?: string
      area?: number
      ownershipRatio?: number
    },
  ): Promise<OwnerCreated> {
    return adminFetch(`/api/admin/communities/${communityId}/owners`, {
      method: 'POST',
      body: JSON.stringify(body),
    }).then((r) => handleResponse<OwnerCreated>(r))
  },

  updateOwner(
    communityId: number,
    ownerId: number,
    body: UpdateOwnerBody,
  ): Promise<AdminOwner> {
    return adminFetch(`/api/admin/communities/${communityId}/owners/${ownerId}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }).then((r) => handleResponse<AdminOwner>(r))
  },

  regenerateAuthCode(communityId: number, ownerId: number): Promise<AuthCodeRegenerated> {
    return adminFetch(`/api/admin/communities/${communityId}/owners/${ownerId}/regenerate-code`, {
      method: 'POST',
      body: '{}',
    }).then((r) => handleResponse<AuthCodeRegenerated>(r))
  },

  getQrCode(communityId: number, ownerId: number): Promise<QrCodeInfo> {
    return adminFetch(`/api/admin/communities/${communityId}/owners/${ownerId}/qr`).then((r) =>
      handleResponse<QrCodeInfo>(r),
    )
  },

  listUnits(communityId: number): Promise<AdminUnit[]> {
    return adminFetch(`/api/admin/communities/${communityId}/units`).then((r) =>
      handleResponse<AdminUnit[]>(r),
    )
  },

  async downloadUnitImportTemplate(communityId: number): Promise<void> {
    const res = await fetch(`/api/admin/communities/${communityId}/units/import/template`, {
      headers: { Authorization: authHeader() },
    })
    if (!res.ok) throw new Error('無法下載範本')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'unit-import-template.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  },

  deleteOwner(communityId: number, ownerId: number): Promise<void> {
    return adminFetch(`/api/admin/communities/${communityId}/owners/${ownerId}`, {
      method: 'DELETE',
    }).then((r) => handleResponse<void>(r))
  },

  deleteOwners(communityId: number, ownerIds: number[]): Promise<{ deletedCount: number }> {
    return adminFetch(`/api/admin/communities/${communityId}/owners/batch-delete`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ownerIds }),
    }).then((r) => handleResponse<{ deletedCount: number }>(r))
  },

  importUnits(
    communityId: number,
    file: File,
    options?: {
      dryRun?: boolean
      createOwners?: boolean
      duplicatePolicy?: import('./admin-types').ImportDuplicatePolicy
    },
  ): Promise<UnitImportResult> {
    const form = new FormData()
    form.append('file', file)
    const params = new URLSearchParams()
    if (options?.dryRun) params.set('dryRun', 'true')
    if (options?.createOwners === false) params.set('createOwners', 'false')
    if (options?.duplicatePolicy) params.set('duplicatePolicy', options.duplicatePolicy)
    const qs = params.toString()
    return fetch(
      `/api/admin/communities/${communityId}/units/import${qs ? `?${qs}` : ''}`,
      {
        method: 'POST',
        headers: { Authorization: authHeader() },
        body: form,
      },
    ).then((r) => handleResponse<UnitImportResult>(r))
  },
}
