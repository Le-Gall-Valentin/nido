import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import type { ICalendarSyncApi } from '../model/ICalendarSyncApi'
import type { CreatedSubscriptionLink, ExternalFeed, ImportReport, SubscriptionLink } from '../model/types'
import { toSyncError } from './calendarSyncErrors'

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

/**
 * What goes in and out of the calendar. Every failure is a CalendarSyncError naming its reason, which the sync dialog
 * words — never the shared errors the event calls throw.
 */
export const calendarSyncApi: ICalendarSyncApi = {
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
