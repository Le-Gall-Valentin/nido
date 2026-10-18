import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AxiosError, AxiosHeaders } from 'axios'
import { client } from '@/shared/api'
import { calendarApi } from './calendarApi'
import { CalendarSyncError } from './calendarSyncErrors'

vi.mock('@/shared/api', () => ({
  client: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))

const base = '/spaces/s1/calendar'

function failure(status: number, data: unknown) {
  return new AxiosError('x', 'ERR', undefined, undefined,
    { status, data, statusText: '', headers: {}, config: { headers: new AxiosHeaders() } })
}

describe('calendar sync api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('downloads the export as a file named by the server', async () => {
    const blob = new Blob(['BEGIN:VCALENDAR'])
    vi.mocked(client.get).mockResolvedValue({
      data: blob, headers: { 'content-disposition': 'attachment; filename="nido-chez-nous-2026-10-13.ics"; filename*=UTF-8\'\'nido-chez-nous-2026-10-13.ics' },
    })

    const file = await calendarApi.exportCalendar('s1', ['EVENT', 'TASK'], true)

    expect(client.get).toHaveBeenCalledWith(`${base}/export.ics`,
      { params: { sources: 'EVENT,TASK', onlyMine: true }, responseType: 'blob' })
    expect(file).toEqual({ blob, filename: 'nido-chez-nous-2026-10-13.ics' })
  })

  it('sends a file to import as its own text/calendar body, and gives it time', async () => {
    const file = new Blob(['BEGIN:VCALENDAR'])
    vi.mocked(client.post).mockResolvedValue({ data: { written: false } })

    await calendarApi.analyseImport('s1', file)
    await calendarApi.importFile('s1', file)

    expect(client.post).toHaveBeenCalledWith(`${base}/import/analysis`, file,
      { headers: { 'Content-Type': 'text/calendar' }, timeout: 120_000 })
    expect(client.post).toHaveBeenCalledWith(`${base}/import`, file,
      { headers: { 'Content-Type': 'text/calendar' }, timeout: 120_000 })
  })

  it('creates, lists and revokes subscription addresses', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: { url: 'https://n/ical/t.ics' } })
    vi.mocked(client.get).mockResolvedValue({ data: [] })
    vi.mocked(client.delete).mockResolvedValue({})

    await calendarApi.createSubscriptionLink('s1', { label: 'iPhone', sources: ['EVENT'], onlyMine: false })
    await calendarApi.listSubscriptionLinks('s1')
    await calendarApi.revokeSubscriptionLink('s1', 'l1')

    expect(client.post).toHaveBeenCalledWith(`${base}/subscription-links`, { label: 'iPhone', sources: ['EVENT'], onlyMine: false })
    expect(client.get).toHaveBeenCalledWith(`${base}/subscription-links`)
    expect(client.delete).toHaveBeenCalledWith(`${base}/subscription-links/l1`)
  })

  it('follows, edits, refreshes and removes a calendar', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: {} })
    vi.mocked(client.patch).mockResolvedValue({ data: {} })
    vi.mocked(client.delete).mockResolvedValue({})

    await calendarApi.followExternalFeed('s1', { url: 'webcal://x/a.ics', name: null, color: 'event-cyan' })
    await calendarApi.updateExternalFeed('s1', 'f1', { name: 'École', color: null })
    await calendarApi.refreshExternalFeed('s1', 'f1')
    await calendarApi.removeExternalFeed('s1', 'f1')

    expect(client.post).toHaveBeenCalledWith(`${base}/external-feeds`, { url: 'webcal://x/a.ics', name: null, color: 'event-cyan' })
    expect(client.patch).toHaveBeenCalledWith(`${base}/external-feeds/f1`, { name: 'École', color: null })
    expect(client.post).toHaveBeenCalledWith(`${base}/external-feeds/f1/refresh`)
    expect(client.delete).toHaveBeenCalledWith(`${base}/external-feeds/f1`)
  })

  it('says why a calendar could not be followed', async () => {
    vi.mocked(client.post).mockRejectedValue(failure(400, { error_code: 'feed_unavailable', feed_error: 'HTTP_404' }))

    await expect(calendarApi.followExternalFeed('s1', { url: 'https://x/a.ics', name: null, color: null }))
      .rejects.toMatchObject({ reason: 'feed_unavailable', feedError: 'HTTP_404' })
  })

  it('names every other failure', async () => {
    vi.mocked(client.post).mockRejectedValueOnce(failure(400, { error_code: 'not_ics' }))
    await expect(calendarApi.analyseImport('s1', new Blob())).rejects.toBeInstanceOf(CalendarSyncError)
    vi.mocked(client.post).mockRejectedValueOnce(failure(413, { error_code: 'file_too_large' }))
    await expect(calendarApi.analyseImport('s1', new Blob())).rejects.toMatchObject({ reason: 'file_too_large' })
    // Refused before it reached nido — a reverse proxy's own limit, often 1 MB: not the 5 MB nido sets.
    vi.mocked(client.post).mockRejectedValueOnce(failure(413, '<html>413 Request Entity Too Large</html>'))
    await expect(calendarApi.analyseImport('s1', new Blob())).rejects.toMatchObject({ reason: 'upload_too_large' })
    vi.mocked(client.post).mockRejectedValueOnce(new Error('offline'))
    await expect(calendarApi.analyseImport('s1', new Blob())).rejects.toMatchObject({ reason: 'network' })
  })

  it('reads the error code of a refused export, whose body arrives as a blob', async () => {
    vi.mocked(client.get).mockRejectedValue(failure(400, new Blob(['{"error_code":"invalid_sources"}'])))
    await expect(calendarApi.exportCalendar('s1', [], false)).rejects.toMatchObject({ reason: 'invalid_sources' })

    vi.mocked(client.get).mockRejectedValue(failure(500, new Blob(['not json'])))
    await expect(calendarApi.exportCalendar('s1', [], false)).rejects.toMatchObject({ reason: 'server' })
  })

  it('names a rate limit, a refusal and a refresh asked too soon', async () => {
    vi.mocked(client.post).mockRejectedValueOnce(failure(429, {}))
    await expect(calendarApi.analyseImport('s1', new Blob())).rejects.toMatchObject({ reason: 'rate_limited' })
    vi.mocked(client.post).mockRejectedValueOnce(failure(403, {}))
    await expect(calendarApi.analyseImport('s1', new Blob())).rejects.toMatchObject({ reason: 'forbidden' })
    vi.mocked(client.post).mockRejectedValueOnce(failure(429, { error_code: 'refresh_too_soon' }))
    await expect(calendarApi.refreshExternalFeed('s1', 'f1')).rejects.toMatchObject({ reason: 'refresh_too_soon' })
  })
})
