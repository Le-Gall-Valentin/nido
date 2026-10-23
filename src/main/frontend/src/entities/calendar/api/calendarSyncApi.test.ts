import { describe, it, expect, expectTypeOf, vi, beforeEach } from 'vitest'
import { AxiosError, AxiosHeaders } from 'axios'
import { client } from '@/shared/api'
import { calendarSyncApi } from './calendarSyncApi'
import { CalendarSyncError } from './calendarSyncErrors'
import type { EventColor } from '../lib/eventPalette'
import type { ExternalFeedInput, ExternalFeedUpdate } from '../model/types'

vi.mock('@/shared/api', () => ({
  client: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))

const base = '/spaces/s1/calendar'

function failure(status: number, data: unknown) {
  return new AxiosError('x', 'ERR', undefined, undefined,
    { status, data, statusText: '', headers: {}, config: { headers: new AxiosHeaders() } })
}

/** Every sync call: each must name why it failed, never throw the shared errors the event calls do. */
const everyCall: [string, () => Promise<unknown>][] = [
  ['exportCalendar', () => calendarSyncApi.exportCalendar('s1', ['EVENT'], false)],
  ['listSubscriptionLinks', () => calendarSyncApi.listSubscriptionLinks('s1')],
  ['createSubscriptionLink', () => calendarSyncApi.createSubscriptionLink('s1', { label: 'iPhone', sources: ['EVENT'], onlyMine: false })],
  ['revokeSubscriptionLink', () => calendarSyncApi.revokeSubscriptionLink('s1', 'l1')],
  ['analyseImport', () => calendarSyncApi.analyseImport('s1', new Blob())],
  ['importFile', () => calendarSyncApi.importFile('s1', new Blob())],
  ['listExternalFeeds', () => calendarSyncApi.listExternalFeeds('s1')],
  ['followExternalFeed', () => calendarSyncApi.followExternalFeed('s1', { url: 'https://x/a.ics', name: null, color: null })],
  ['updateExternalFeed', () => calendarSyncApi.updateExternalFeed('s1', 'f1', { name: 'École', color: null })],
  ['removeExternalFeed', () => calendarSyncApi.removeExternalFeed('s1', 'f1')],
  ['refreshExternalFeed', () => calendarSyncApi.refreshExternalFeed('s1', 'f1')],
]

function everyVerbRejects(error: unknown) {
  for (const verb of ['get', 'post', 'patch', 'put', 'delete'] as const) vi.mocked(client[verb]).mockRejectedValue(error)
}

describe('calendar sync api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('downloads the export as a file named by the server', async () => {
    const blob = new Blob(['BEGIN:VCALENDAR'])
    vi.mocked(client.get).mockResolvedValue({
      data: blob, headers: { 'content-disposition': 'attachment; filename="nido-chez-nous-2026-10-13.ics"; filename*=UTF-8\'\'nido-chez-nous-2026-10-13.ics' },
    })

    const file = await calendarSyncApi.exportCalendar('s1', ['EVENT', 'TASK'], true)

    expect(client.get).toHaveBeenCalledWith(`${base}/export.ics`,
      { params: { sources: 'EVENT,TASK', onlyMine: true }, responseType: 'blob' })
    expect(file).toEqual({ blob, filename: 'nido-chez-nous-2026-10-13.ics' })
  })

  it('sends a file to import as its own text/calendar body, and gives it time', async () => {
    const file = new Blob(['BEGIN:VCALENDAR'])
    vi.mocked(client.post).mockResolvedValue({ data: { written: false } })

    await calendarSyncApi.analyseImport('s1', file)
    await calendarSyncApi.importFile('s1', file)

    expect(client.post).toHaveBeenCalledWith(`${base}/import/analysis`, file,
      { headers: { 'Content-Type': 'text/calendar' }, timeout: 120_000 })
    expect(client.post).toHaveBeenCalledWith(`${base}/import`, file,
      { headers: { 'Content-Type': 'text/calendar' }, timeout: 120_000 })
  })

  it('creates, lists and revokes subscription addresses', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: { url: 'https://n/ical/t.ics' } })
    vi.mocked(client.get).mockResolvedValue({ data: [] })
    vi.mocked(client.delete).mockResolvedValue({})

    await calendarSyncApi.createSubscriptionLink('s1', { label: 'iPhone', sources: ['EVENT'], onlyMine: false })
    await calendarSyncApi.listSubscriptionLinks('s1')
    await calendarSyncApi.revokeSubscriptionLink('s1', 'l1')

    expect(client.post).toHaveBeenCalledWith(`${base}/subscription-links`, { label: 'iPhone', sources: ['EVENT'], onlyMine: false })
    expect(client.get).toHaveBeenCalledWith(`${base}/subscription-links`)
    expect(client.delete).toHaveBeenCalledWith(`${base}/subscription-links/l1`)
  })

  it('follows, edits, refreshes and removes a calendar', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: {} })
    vi.mocked(client.patch).mockResolvedValue({ data: {} })
    vi.mocked(client.delete).mockResolvedValue({})

    await calendarSyncApi.followExternalFeed('s1', { url: 'webcal://x/a.ics', name: null, color: 'event-cyan' })
    await calendarSyncApi.updateExternalFeed('s1', 'f1', { name: 'École', color: null })
    await calendarSyncApi.refreshExternalFeed('s1', 'f1')
    await calendarSyncApi.removeExternalFeed('s1', 'f1')

    expect(client.post).toHaveBeenCalledWith(`${base}/external-feeds`, { url: 'webcal://x/a.ics', name: null, color: 'event-cyan' })
    expect(client.patch).toHaveBeenCalledWith(`${base}/external-feeds/f1`, { name: 'École', color: null })
    expect(client.post).toHaveBeenCalledWith(`${base}/external-feeds/f1/refresh`)
    expect(client.delete).toHaveBeenCalledWith(`${base}/external-feeds/f1`)
  })

  it('sends only a colour of the palette, or none: the server refuses any other', () => {
    expectTypeOf<ExternalFeedInput['color']>().toEqualTypeOf<EventColor | null>()
    expectTypeOf<ExternalFeedUpdate['color']>().toEqualTypeOf<EventColor | null>()
  })

  it('says why a calendar could not be followed', async () => {
    vi.mocked(client.post).mockRejectedValue(failure(400, { error_code: 'feed_unavailable', feed_error: 'HTTP_404' }))

    await expect(calendarSyncApi.followExternalFeed('s1', { url: 'https://x/a.ics', name: null, color: null }))
      .rejects.toMatchObject({ reason: 'feed_unavailable', feedError: 'HTTP_404' })
  })

  it('names every other failure', async () => {
    vi.mocked(client.post).mockRejectedValueOnce(failure(400, { error_code: 'not_ics' }))
    await expect(calendarSyncApi.analyseImport('s1', new Blob())).rejects.toMatchObject({ reason: 'not_ics' })
    vi.mocked(client.post).mockRejectedValueOnce(failure(413, { error_code: 'file_too_large' }))
    await expect(calendarSyncApi.analyseImport('s1', new Blob())).rejects.toMatchObject({ reason: 'file_too_large' })
    // Refused before it reached nido — a reverse proxy's own limit, often 1 MB: not the 5 MB nido sets.
    vi.mocked(client.post).mockRejectedValueOnce(failure(413, '<html>413 Request Entity Too Large</html>'))
    await expect(calendarSyncApi.analyseImport('s1', new Blob())).rejects.toMatchObject({ reason: 'upload_too_large' })
    vi.mocked(client.post).mockRejectedValueOnce(new Error('offline'))
    await expect(calendarSyncApi.analyseImport('s1', new Blob())).rejects.toMatchObject({ reason: 'network' })
  })

  it('reads the error code of a refused export, whose body arrives as a blob', async () => {
    vi.mocked(client.get).mockRejectedValue(failure(400, new Blob(['{"error_code":"invalid_sources"}'])))
    await expect(calendarSyncApi.exportCalendar('s1', [], false)).rejects.toMatchObject({ reason: 'invalid_sources' })

    vi.mocked(client.get).mockRejectedValue(failure(500, new Blob(['not json'])))
    await expect(calendarSyncApi.exportCalendar('s1', [], false)).rejects.toMatchObject({ reason: 'server' })
  })

  it('names a rate limit, a refusal and a refresh asked too soon', async () => {
    vi.mocked(client.post).mockRejectedValueOnce(failure(429, {}))
    await expect(calendarSyncApi.analyseImport('s1', new Blob())).rejects.toMatchObject({ reason: 'rate_limited' })
    vi.mocked(client.post).mockRejectedValueOnce(failure(403, {}))
    await expect(calendarSyncApi.analyseImport('s1', new Blob())).rejects.toMatchObject({ reason: 'forbidden' })
    vi.mocked(client.post).mockRejectedValueOnce(failure(429, { error_code: 'refresh_too_soon' }))
    await expect(calendarSyncApi.refreshExternalFeed('s1', 'f1')).rejects.toMatchObject({ reason: 'refresh_too_soon' })
  })

  it.each(everyCall)('names a rate limit, a refusal and a lost connection on %s', async (_name, call) => {
    for (const [error, reason] of [[failure(429, {}), 'rate_limited'], [failure(403, {}), 'forbidden'], [new Error('offline'), 'network']] as const) {
      everyVerbRejects(error)
      const failed = await call().then(() => null, (thrown: unknown) => thrown)
      expect(failed).toBeInstanceOf(CalendarSyncError)
      expect(failed).toMatchObject({ reason })
    }
  })

  it.each(everyCall)('words a code it does not know on %s as the server failing', async (_name, call) => {
    everyVerbRejects(failure(500, { error_code: 'data_integrity' }))
    await expect(call()).rejects.toMatchObject({ reason: 'server' })
  })

  it('says an address or a calendar removed meanwhile is gone, and nothing else', async () => {
    vi.mocked(client.delete).mockRejectedValueOnce(failure(404, { error_code: 'subscription_link_not_found' }))
    await expect(calendarSyncApi.revokeSubscriptionLink('s1', 'l1'))
      .rejects.toMatchObject({ reason: 'subscription_link_not_found', gone: true })
    vi.mocked(client.patch).mockRejectedValueOnce(failure(404, { error_code: 'external_feed_not_found' }))
    await expect(calendarSyncApi.updateExternalFeed('s1', 'f1', { name: 'École', color: null }))
      .rejects.toMatchObject({ reason: 'external_feed_not_found', gone: true })
    vi.mocked(client.post).mockRejectedValueOnce(failure(429, { error_code: 'refresh_too_soon' }))
    await expect(calendarSyncApi.refreshExternalFeed('s1', 'f1')).rejects.toMatchObject({ gone: false })
  })
})
