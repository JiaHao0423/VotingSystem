import type {
  ApiError,
  ProposalResult,
  ProposalSummary,
  QrPreview,
  UnitOptionsResponse,
  VoterSession,
  VoteChoice,
} from './types'

const JSON_HEADERS = { 'Content-Type': 'application/json' }

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.ok) {
    if (res.status === 204) return undefined as T
    return res.json() as Promise<T>
  }
  let message = res.statusText
  try {
    const body = (await res.json()) as ApiError
    message = body.message ?? body.error ?? message
  } catch {
    // ignore
  }
  throw new Error(message || `HTTP ${res.status}`)
}

function fetchApi(path: string, init?: RequestInit) {
  return fetch(path, {
    credentials: 'include',
    ...init,
    headers: {
      ...JSON_HEADERS,
      ...init?.headers,
    },
  })
}

export const api = {
  getCommunities(): Promise<{ id: number; name: string; address: string | null }[]> {
    return fetchApi('/api/communities').then((res) =>
      handleResponse<{ id: number; name: string; address: string | null }[]>(res),
    )
  },

  getUnitOptions(communityId: number): Promise<UnitOptionsResponse> {
    return fetchApi(`/api/units/options?communityId=${communityId}`).then((res) =>
      handleResponse<UnitOptionsResponse>(res),
    )
  },

  verifyAuth(
    communityId: number,
    unitShortName: string,
    authCode: string,
  ): Promise<VoterSession> {
    return fetchApi('/api/auth/verify', {
      method: 'POST',
      body: JSON.stringify({ communityId, unitShortName, authCode }),
    }).then((res) => handleResponse<VoterSession>(res))
  },

  previewQr(token: string): Promise<QrPreview> {
    return fetchApi(`/api/auth/qr/preview?token=${encodeURIComponent(token)}`).then((res) =>
      handleResponse<QrPreview>(res),
    )
  },

  verifyQr(token: string): Promise<VoterSession> {
    return fetchApi('/api/auth/qr', {
      method: 'POST',
      body: JSON.stringify({ token }),
    }).then((res) => handleResponse<VoterSession>(res))
  },

  getMe(): Promise<VoterSession> {
    return fetchApi('/api/auth/me').then((res) => handleResponse<VoterSession>(res))
  },

  logout(): Promise<void> {
    return fetchApi('/api/auth/logout', { method: 'POST' }).then((res) => handleResponse<void>(res))
  },

  listProposals(): Promise<ProposalSummary[]> {
    return fetchApi('/api/proposals').then((res) => handleResponse<ProposalSummary[]>(res))
  },

  getProposal(id: number): Promise<ProposalSummary> {
    return fetchApi(`/api/proposals/${id}`).then((res) => handleResponse<ProposalSummary>(res))
  },

  submitVote(proposalId: number, choice: VoteChoice): Promise<void> {
    return fetchApi(`/api/proposals/${proposalId}/votes`, {
      method: 'POST',
      body: JSON.stringify({ choice }),
    }).then((res) => handleResponse<void>(res))
  },

  getResults(proposalId: number): Promise<ProposalResult> {
    return fetchApi(`/api/proposals/${proposalId}/results`).then((res) =>
      handleResponse<ProposalResult>(res),
    )
  },
}
