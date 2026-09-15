import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, ForbiddenError, NotFoundError } from '@/shared/lib'
import type { ICalendarApi } from '../model/ICalendarApi'
import type { IRecurringEventSeriesApi } from '../model/IRecurringEventSeriesApi'
import type { CalendarEvent, CalendarOccurrence, RecurringEventSeries } from '../model/types'

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

const base = (spaceId: string) => `/spaces/${spaceId}/calendar`

export const calendarApi: ICalendarApi & IRecurringEventSeriesApi = {
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
}
