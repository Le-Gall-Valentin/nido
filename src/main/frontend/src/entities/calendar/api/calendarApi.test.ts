import { describe, it, expect, vi, beforeEach } from 'vitest'
import { client } from '@/shared/api'
import { calendarApi } from './calendarApi'
import { NotFoundError, ForbiddenError, RateLimitError } from '@/shared/lib'
import type { EventInput } from '../model/types'

vi.mock('@/shared/api', () => ({
  client: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))

const input: EventInput = {
  title: 'Piano', description: null, location: null, allDay: false,
  startDate: '2026-01-13', startTime: '18:00', endDate: '2026-01-13', endTime: '19:00',
  color: null, participantIds: [],
}

describe('calendarApi', () => {
  beforeEach(() => { vi.resetAllMocks() })

  it('asks the server for one window, not one request per source', async () => {
    vi.mocked(client.get).mockResolvedValue({ data: [] })
    await calendarApi.listOccurrences('s1', '2026-01-01', '2026-01-31')
    expect(client.get).toHaveBeenCalledOnce()
    expect(client.get).toHaveBeenCalledWith('/spaces/s1/calendar/occurrences', {
      params: { from: '2026-01-01', to: '2026-01-31' },
    })
  })

  it('puts an occurrence override on its slot, so replaying it is harmless', async () => {
    vi.mocked(client.put).mockResolvedValue({ data: {} })
    await calendarApi.detachOccurrence('s1', 'series-1', '2026-01-13', input)
    expect(client.put).toHaveBeenCalledWith(
      '/spaces/s1/calendar/recurring-event-series/series-1/occurrences/2026-01-13', input)
  })

  it('cancels one occurrence by deleting its slot, never the series', async () => {
    vi.mocked(client.delete).mockResolvedValue({ data: undefined })
    await calendarApi.excludeOccurrence('s1', 'series-1', '2026-01-13')
    expect(client.delete).toHaveBeenCalledWith(
      '/spaces/s1/calendar/recurring-event-series/series-1/occurrences/2026-01-13')
  })

  it('sends a transfer destination in the body, not the path', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: {} })
    await calendarApi.copyEvent('s1', 'e1', 's2')
    expect(client.post).toHaveBeenCalledWith('/spaces/s1/calendar/events/e1/copy', { destinationSpaceId: 's2' })
  })

  it('sends an occurrence still to come by its series and its day', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: {} })
    await calendarApi.copyOccurrence('s1', 'series-1', '2026-01-13', 's2')
    await calendarApi.moveOccurrence('s1', 'series-1', '2026-01-13', 's2')
    expect(client.post).toHaveBeenCalledWith(
      '/spaces/s1/calendar/recurring-event-series/series-1/occurrences/2026-01-13/copy', { destinationSpaceId: 's2' })
    expect(client.post).toHaveBeenCalledWith(
      '/spaces/s1/calendar/recurring-event-series/series-1/occurrences/2026-01-13/move', { destinationSpaceId: 's2' })
  })

  it('joins and leaves through the participants/me pair', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: undefined })
    vi.mocked(client.delete).mockResolvedValue({ data: undefined })
    await calendarApi.joinEvent('s1', 'e1')
    await calendarApi.leaveEvent('s1', 'e1')
    expect(client.post).toHaveBeenCalledWith('/spaces/s1/calendar/events/e1/participants/me')
    expect(client.delete).toHaveBeenCalledWith('/spaces/s1/calendar/events/e1/participants/me')
  })

  it.each([
    [404, NotFoundError],
    [403, ForbiddenError],
    [429, RateLimitError],
  ])('translates a %s the way every other entity adapter does', async (status, expected) => {
    vi.mocked(client.get).mockRejectedValue({ isAxiosError: true, response: { status } })
    await expect(calendarApi.listOccurrences('s1', '2026-01-01', '2026-01-31')).rejects.toBeInstanceOf(expected)
  })
})
