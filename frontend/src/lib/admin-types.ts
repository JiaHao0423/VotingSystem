import type { ProposalResult, ProposalSummary, ProposalType, VoteChoice } from './types'

export interface Community {
  id: number
  name: string
  totalHouseholds: number
  totalArea: number
  address: string
  createdAt: string
}

export type AdminRole = 'SUPER_ADMIN' | 'COMMUNITY_ADMIN'

export interface AdminMe {
  id: number
  username: string
  displayName: string | null
  role: AdminRole
  community: Community | null
}

export interface AdminAccount {
  id: number
  username: string
  displayName: string | null
  role: AdminRole
  communityId: number | null
  communityName: string | null
  createdAt: string
}

export interface CreateCommunityBody {
  name: string
  totalHouseholds: number
  totalArea?: number | null
  address?: string
  meetingName?: string
}

export interface UpdateCommunityBody {
  name: string
  totalHouseholds: number
  totalArea?: number | null
  address?: string
}

export interface CreateAdminAccountBody {
  username: string
  password: string
  displayName?: string
  role: AdminRole
  communityId?: number | null
}

export interface UpdateAdminAccountBody {
  username: string
  displayName?: string
  communityId?: number | null
}

export interface AdminOwner {
  id: number
  unitId: number
  unitShortName: string
  fullAddress: string
  buildingType: string
  floor: number | null
  unitNo: number | null
  shopNo: number | null
  area: number | null
  ownershipRatio: number | null
  name: string
  phone: string
  attended: boolean
  createdAt: string
}

export interface UpdateOwnerBody {
  name: string
  phone?: string
  attended: boolean
  unitShortName: string
  fullAddress: string
  buildingType: string
  floor?: number | null
  unitNo?: number | null
  shopNo?: number | null
  area?: number | null
  ownershipRatio?: number | null
}

export interface OwnerCreated {
  owner: AdminOwner
  authCode: string
  qrToken: string
  qrUrl: string
  message: string
}

export interface OwnerQrPrintItem {
  ownerId: number
  ownerName: string
  unitShortName: string
  ownershipRatio: number | null
  qrUrl: string
}

export interface QrCodeInfo {
  ownerId: number
  unitShortName: string
  ownerName: string
  fullAddress: string
  buildingType: string
  qrToken: string
  qrUrl: string
}

export interface AuthCodeRegenerated {
  authCode: string
  message: string
}

export interface AdminUnit {
  id: number
  communityId: number
  shortName: string
  fullAddress: string
  buildingType: string
  floor: number | null
  unitNo: number | null
  shopNo: number | null
  area: number
  ownershipRatio: number
  hasOwner: boolean
  createdAt: string
}

export interface UnitImportRowResult {
  rowNumber: number
  shortName: string
  status: 'CREATED' | 'SKIPPED' | 'ERROR' | string
  message: string
}

export type ImportDuplicatePolicy = 'SKIP' | 'REPLACE'

export interface UnitImportResult {
  totalRows: number
  createdUnits: number
  updatedUnits: number
  skippedUnits: number
  createdOwners: number
  errorCount: number
  dryRun: boolean
  rows: UnitImportRowResult[]
}

export interface CreateProposalBody {
  proposalNumber: string
  title: string
  content: string
  type: ProposalType
  startTime?: string | null
  endTime?: string | null
  visible?: boolean
  sortOrder?: number
}

export type AdminProposal = Omit<ProposalSummary, 'hasVoted'>

export interface AdminResultDetail {
  summary: ProposalResult
  voters: {
    ownerId: number
    ownerName: string
    unitShortName: string
    choice: VoteChoice
    choiceLabel: string
    votedAt: string
  }[]
}
