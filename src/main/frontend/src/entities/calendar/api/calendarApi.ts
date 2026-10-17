import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, ForbiddenError, NotFoundError } from '@/shared/lib'
import type { CalendarApi } from '../model/calendarApiContext'
import type {
  CalendarEvent, CalendarOccurrence, CreatedSubscriptionLink, ExternalFeed, ImportReport, RecurringEventSeries, SubscriptionLink,
} from '../model/types'
import { toSyncError } from './calendarSyncErrors'

function handleError(error: unknown): never {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status === 429) throw new RateLimitError()
    if (status === 403) throw new ForbiddenError()
    if (status === 404) throw new NotFoundError()
    if (status !== undefined) throw new ServerError()
  }
  throw new NetworkError()
}

/** A large file takes longer to read than the usual 15 s. */
const IMPORT_TIMEOUT_MS = 120_000

/** A blob response type turns an error body into a Blob too: put the JSON back so that `error_code` can be read. */
async function readBlobBody(error: unknown): Promise<unknown> {
  if (isAxiosError(error) && error.response?.data instanceof Blob) {
    try {
      error.response.data = JSON.parse(await error.response.data.text()) as unknown
    } catch {
      error.response.data = {}
    }
  }
  return error
}

const base = (spaceId: string) => `/spaces/${spaceId}/calendar`

export const calendarApi: CalendarApi = {
  async listOccurrences(spaceId, from, to) {
    try {
      const res = await client.get<CalendarOccurrence[]>(`${base(spaceId)}/occurrences`, { params: { from, to } })
      return res.data
    } catch (error) { handleError(error) }
  },

  async createEvent(spaceId, input) {
    try {
      const res = await client.post<CalendarEvent>(`${base(spaceId)}/events`, input)
      return res.data
    } catch (error) { handleError(error) }
  },

  async updateEvent(spaceId, eventId, input) {
    try {
      const res = await client.patch<CalendarEvent>(`${base(spaceId)}/events/${eventId}`, input)
      return res.data
    } catch (error) { handleError(error) }
  },

  async deleteEvent(spaceId, eventId) {
    try {
      await client.delete(`${base(spaceId)}/events/${eventId}`)
    } catch (error) { handleError(error) }
  },

  async joinEvent(spaceId, eventId) {
    try {
      await client.post(`${base(spaceId)}/events/${eventId}/participants/me`)
    } catch (error) { handleError(error) }
  },

  async leaveEvent(spaceId, eventId) {
    try {
      await client.delete(`${base(spaceId)}/events/${eventId}/participants/me`)
    } catch (error) { handleError(error) }
  },

  async copyEvent(spaceId, eventId, destinationSpaceId) {
    try {
      const res = await client.post<CalendarEvent>(`${base(spaceId)}/events/${eventId}/copy`, { destinationSpaceId })
      return res.data
    } catch (error) { handleError(error) }
  },

  async moveEvent(spaceId, eventId, destinationSpaceId) {
    try {
      const res = await client.post<CalendarEvent>(`${base(spaceId)}/events/${eventId}/move`, { destinationSpaceId })
      return res.data
    } catch (error) { handleError(error) }
  },

  async listRecurringEventSeries(spaceId) {
    try {
      const res = await client.get<RecurringEventSeries[]>(`${base(spaceId)}/recurring-event-series`)
      return res.data
    } catch (error) { handleError(error) }
  },

  async createRecurringEventSeries(spaceId, input) {
    try {
      const res = await client.post<RecurringEventSeries>(`${base(spaceId)}/recurring-event-series`, input)
      return res.data
    } catch (error) { handleError(error) }
  },

  async updateRecurringEventSeries(spaceId, seriesId, input) {
    try {
      const res = await client.patch<RecurringEventSeries>(`${base(spaceId)}/recurring-event-series/${seriesId}`, input)
      return res.data
    } catch (error) { handleError(error) }
  },

  async deleteRecurringEventSeries(spaceId, seriesId) {
    try {
      await client.delete(`${base(spaceId)}/recurring-event-series/${seriesId}`)
    } catch (error) { handleError(error) }
  },

  async detachOccurrence(spaceId, seriesId, date, input) {
    try {
      // PUT, not PATCH: this upserts the (series, slot) pair, so replaying it is harmless.
      const res = await client.put<CalendarEvent>(
        `${base(spaceId)}/recurring-event-series/${seriesId}/occurrences/${date}`, input)
      return res.data
    } catch (error) { handleError(error) }
  },

  async excludeOccurrence(spaceId, seriesId, date) {
    try {
      await client.delete(`${base(spaceId)}/recurring-event-series/${seriesId}/occurrences/${date}`)
    } catch (error) { handleError(error) }
  },

  async copyOccurrence(spaceId, seriesId, date, destinationSpaceId) {
    try {
      const res = await client.post<CalendarEvent>(
        `${base(spaceId)}/recurring-event-series/${seriesId}/occurrences/${date}/copy`, { destinationSpaceId })
      return res.data
    } catch (error) { handleError(error) }
  },

  async moveOccurrence(spaceId, seriesId, date, destinationSpaceId) {
    try {
      const res = await client.post<CalendarEvent>(
        `${base(spaceId)}/recurring-event-series/${seriesId}/occurrences/${date}/move`, { destinationSpaceId })
      return res.data
    } catch (error) { handleError(error) }
  },

  async exportCalendar(spaceId, sources, onlyMine) {
    try {
      const res = await client.get<Blob>(`${base(spaceId)}/export.ics`,
        { params: { sources: sources.join(','), onlyMine }, responseType: 'blob' })
      const disposition = String(res.headers['content-disposition'] ?? '')
      const filename = /filename="([^"]+)"/.exec(disposition)?.[1] ?? 'nido.ics'
      return { blob: res.data, filename }
    } catch (error) { throw toSyncError(await readBlobBody(error)) }
  },

  async listSubscriptionLinks(spaceId) {
    try {
      const res = await client.get<SubscriptionLink[]>(`${base(spaceId)}/subscription-links`)
      return res.data
    } catch (error) { throw toSyncError(error) }
  },

  async createSubscriptionLink(spaceId, input) {
    try {
      const res = await client.post<CreatedSubscriptionLink>(`${base(spaceId)}/subscription-links`, input)
      return res.data
    } catch (error) { throw toSyncError(error) }
  },

  async revokeSubscriptionLink(spaceId, linkId) {
    try {
      await client.delete(`${base(spaceId)}/subscription-links/${linkId}`)
    } catch (error) { throw toSyncError(error) }
  },

  // The file is the request's own body, typed text/calendar: a type that makes a browser ask the server first, the
  // second guard against forged requests client.ts relies on. A large file takes longer than the usual 15 s.
  async analyseImport(spaceId, file) {
    try {
      const res = await client.post<ImportReport>(`${base(spaceId)}/import/analysis`, file,
        { headers: { 'Content-Type': 'text/calendar' }, timeout: IMPORT_TIMEOUT_MS })
      return res.data
    } catch (error) { throw toSyncError(error) }
  },

  async importFile(spaceId, file) {
    try {
      const res = await client.post<ImportReport>(`${base(spaceId)}/import`, file,
        { headers: { 'Content-Type': 'text/calendar' }, timeout: IMPORT_TIMEOUT_MS })
      return res.data
    } catch (error) { throw toSyncError(error) }
  },

  async listExternalFeeds(spaceId) {
    try {
      const res = await client.get<ExternalFeed[]>(`${base(spaceId)}/external-feeds`)
      return res.data
    } catch (error) { throw toSyncError(error) }
  },

  async followExternalFeed(spaceId, input) {
    try {
      const res = await client.post<ExternalFeed>(`${base(spaceId)}/external-feeds`, input)
      return res.data
    } catch (error) { throw toSyncError(error) }
  },

  async updateExternalFeed(spaceId, feedId, input) {
    try {
      const res = await client.patch<ExternalFeed>(`${base(spaceId)}/external-feeds/${feedId}`, input)
      return res.data
    } catch (error) { throw toSyncError(error) }
  },

  async removeExternalFeed(spaceId, feedId) {
    try {
      await client.delete(`${base(spaceId)}/external-feeds/${feedId}`)
    } catch (error) { throw toSyncError(error) }
  },

  async refreshExternalFeed(spaceId, feedId) {
    try {
      const res = await client.post<ExternalFeed>(`${base(spaceId)}/external-feeds/${feedId}/refresh`)
      return res.data
    } catch (error) { throw toSyncError(error) }
  },
}
