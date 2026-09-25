import { describe, it, expect, vi, beforeEach } from 'vitest'
import { client } from '@/shared/api'
import { calendarApi } from './calendarApi'
import { NotFoundError, ForbiddenError, RateLimitError, ServerError, NetworkError } from '@/shared/lib'
import type { EventInput, RecurringEventSeriesInput } from '../model/types'

vi.mock('@/shared/api', () => ({
  client: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))

const input: EventInput = {
  title: 'Piano', description: null, location: null, allDay: false,
  startDate: '2026-01-13', startTime: '18:00', endDate: '2026-01-13', endTime: '19:00',
  color: null, participantIds: [],
}

const seriesInput: RecurringEventSeriesInput = {
  title: 'Piano', description: null, location: null, allDay: false, startTime: '18:00', endTime: '19:00',
  durationDays: 0, color: null, intervalType: 'WEEKLY', intervalCount: 1, anchorDate: '2026-01-06',
  endDate: null, participantIds: [],
}

const series = '/spaces/s1/calendar/recurring-event-series'

/** Every write, the verb and route it goes to, and what it sends. */
const writes: [string, () => Promise<unknown>, 'post' | 'patch' | 'put' | 'delete', unknown[]][] = [
  ['createEvent', () => calendarApi.createEvent('s1', input), 'post', ['/spaces/s1/calendar/events', input]],
  ['updateEvent', () => calendarApi.updateEvent('s1', 'e1', input), 'patch', ['/spaces/s1/calendar/events/e1', input]],
  ['deleteEvent', () => calendarApi.deleteEvent('s1', 'e1'), 'delete', ['/spaces/s1/calendar/events/e1']],
  ['moveEvent', () => calendarApi.moveEvent('s1', 'e1', 's2'), 'post', ['/spaces/s1/calendar/events/e1/move', { destinationSpaceId: 's2' }]],
  ['createRecurringEventSeries', () => calendarApi.createRecurringEventSeries('s1', seriesInput), 'post', [series, seriesInput]],
  ['updateRecurringEventSeries', () => calendarApi.updateRecurringEventSeries('s1', 'r1', seriesInput), 'patch', [`${series}/r1`, seriesInput]],
  ['deleteRecurringEventSeries', () => calendarApi.deleteRecurringEventSeries('s1', 'r1'), 'delete', [`${series}/r1`]],
]

/** Every call, failing: each must turn the failure into the app's own error rather than let axios' through. */
const everyCall: [string, () => Promise<unknown>][] = [
  ['listOccurrences', () => calendarApi.listOccurrences('s1', '2026-01-01', '2026-01-31')],
  ...writes.map(([name, call]): [string, () => Promise<unknown>] => [name, call]),
  ['joinEvent', () => calendarApi.joinEvent('s1', 'e1')],
  ['leaveEvent', () => calendarApi.leaveEvent('s1', 'e1')],
  ['copyEvent', () => calendarApi.copyEvent('s1', 'e1', 's2')],
  ['listRecurringEventSeries', () => calendarApi.listRecurringEventSeries('s1')],
  ['detachOccurrence', () => calendarApi.detachOccurrence('s1', 'r1', '2026-01-13', input)],
  ['excludeOccurrence', () => calendarApi.excludeOccurrence('s1', 'r1', '2026-01-13')],
  ['copyOccurrence', () => calendarApi.copyOccurrence('s1', 'r1', '2026-01-13', 's2')],
  ['moveOccurrence', () => calendarApi.moveOccurrence('s1', 'r1', '2026-01-13', 's2')],
]

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

  it('reads the series of a space from their own route', async () => {
    vi.mocked(client.get).mockResolvedValue({ data: [] })
    await calendarApi.listRecurringEventSeries('s1')
    expect(client.get).toHaveBeenCalledWith(series)
  })

  it.each(writes)('sends %s to its own route', async (_name, call, verb, args) => {
    vi.mocked(client[verb]).mockResolvedValue({ data: {} })
    await call()
    expect(client[verb]).toHaveBeenCalledWith(...args)
  })

  it.each([
    [404, NotFoundError],
    [403, ForbiddenError],
    [429, RateLimitError],
    [500, ServerError],
  ])('translates a %s the way every other entity adapter does', async (status, expected) => {
    vi.mocked(client.get).mockRejectedValue({ isAxiosError: true, response: { status } })
    await expect(calendarApi.listOccurrences('s1', '2026-01-01', '2026-01-31')).rejects.toBeInstanceOf(expected)
  })

  it('reports a request that never got an answer as the network failing', async () => {
    vi.mocked(client.get).mockRejectedValue({ isAxiosError: true })
    await expect(calendarApi.listOccurrences('s1', '2026-01-01', '2026-01-31')).rejects.toBeInstanceOf(NetworkError)
  })

  it.each(everyCall)('translates a failure of %s', async (_name, call) => {
    for (const verb of ['get', 'post', 'patch', 'put', 'delete'] as const) {
      vi.mocked(client[verb]).mockRejectedValue({ isAxiosError: true, response: { status: 404 } })
    }
    await expect(call()).rejects.toBeInstanceOf(NotFoundError)
  })
})
