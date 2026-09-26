import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, NotFoundError, RateLimitError } from '@/shared/lib'
import { dashboardApi } from './dashboardApi'
import type { Dashboard } from '../model/types'

vi.mock('@/shared/api', () => ({ client: { get: vi.fn() } }))

const DASHBOARD: Dashboard = { date: '2026-09-26', spaceType: 'SHARED', canWrite: true, attention: [], cards: {} }

describe('dashboardApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('reads the dashboard of a space', async () => {
    vi.mocked(client.get).mockResolvedValue({ data: DASHBOARD })

    const result = await dashboardApi.getDashboard('space-1')

    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/dashboard')
    expect(result).toEqual(DASHBOARD)
  })

  it('translates a 404 — not a member — into NotFoundError', async () => {
    vi.mocked(client.get).mockRejectedValueOnce(new AxiosError('Not found', undefined, undefined, undefined, { status: 404 } as never))
    await expect(dashboardApi.getDashboard('space-1')).rejects.toBeInstanceOf(NotFoundError)
  })

  it('translates a 429 into RateLimitError', async () => {
    vi.mocked(client.get).mockRejectedValueOnce(new AxiosError('Too many', undefined, undefined, undefined, { status: 429 } as never))
    await expect(dashboardApi.getDashboard('space-1')).rejects.toBeInstanceOf(RateLimitError)
  })

  it('translates a missing response into NetworkError', async () => {
    vi.mocked(client.get).mockRejectedValueOnce(new Error('offline'))
    await expect(dashboardApi.getDashboard('space-1')).rejects.toBeInstanceOf(NetworkError)
  })
})
