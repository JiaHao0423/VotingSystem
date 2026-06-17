export type ProposalStatus = 'DRAFT' | 'SCHEDULED' | 'ACTIVE' | 'ENDED'
export type ProposalType = 'GENERAL' | 'EXTRAORDINARY'
export type VoteChoice = 'AGREE' | 'DISAGREE' | 'ABSTAIN'
export type BuildingType = 'A' | 'B' | 'SHOP'

export interface QrPreview {
  ownerName: string
  unitShortName: string
  fullAddress: string
  buildingType: BuildingType
  area: number | null
  ownershipRatio: number | null
}

export interface VoterSession {
  ownerId: number
  unitId: number
  communityId: number
  unitShortName: string
  fullAddress: string
  buildingType: BuildingType
  name: string
  area: number
  ownershipRatio: number
  attended: boolean
  message: string
}

export interface ProposalSummary {
  id: number
  meetingId: number
  proposalNumber: string
  title: string
  content: string
  type: ProposalType
  status: ProposalStatus
  startTime: string | null
  endTime: string | null
  visible: boolean
  sortOrder: number
  hasVoted: boolean
  createdAt: string
}

export interface VoteOptionResult {
  choice: VoteChoice
  label: string
  votes: number
  weight: number
  voteRatio: number
  weightRatio: number
}

export interface ProposalResult {
  id: number
  proposalNumber: string
  title: string
  content: string
  type: ProposalType
  status: ProposalStatus
  options: VoteOptionResult[]
  totalVotedHouseholds: number
  totalVotedWeight: number
  totalCommunityHouseholds: number
  totalCommunityWeight: number
  agreeHouseholdRatio: number
  agreeWeightRatio: number
  passed: boolean
  votedAt: string | null
}

export interface UnitOptionItem {
  id: number
  shortName: string
  unitNo: number | null
  hasOwner: boolean
}

export interface FloorOption {
  floor: number
  units: UnitOptionItem[]
}

export interface BuildingOption {
  buildingType: BuildingType
  label: string
  floors: FloorOption[]
  units: UnitOptionItem[]
}

export interface UnitOptionsResponse {
  communityId: number
  communityName: string
  buildings: BuildingOption[]
}

export interface ApiError {
  message?: string
  error?: string
}
