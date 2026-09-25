import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { SpaceMember } from '@/entities/space'
import {
  toEventInput, useCreateEvent, useUpdateEvent, useDetachOccurrence, useCreateRecurringEventSeries,
  useUpdateRecurringEventSeries, type CalendarOccurrence, type EventInput, type RecurringEventSeries, type ScheduleChange,
} from '@/entities/calendar'
import { fromSeries, runsAcross, toSeriesInput, type Recurrence } from '../lib/seriesInput'
import { EventFormModal } from './EventFormModal'

interface EventFormPanelProps {
  spaceId: string
  /** The occurrence being edited, or null when creating. */
  occurrence: CalendarOccurrence | null
  /** The series being edited as a whole: the form opens on its first occurrence and how it repeats. */
  series?: RecurringEventSeries | null
  /**
   * Set when editing exactly one occurrence of a series: the write then goes to the slot's
   * upsert endpoint instead of to the event, so the rest of the series is untouched.
   */
  detachSlot: { seriesId: string; date: string } | null
  /** Pre-fills the dates when creating from a day cell. */
  defaultDate: string
  /** Pre-fills the whole when — times included — for a time picked out in the week or day grid. */
  defaultSchedule?: ScheduleChange | null
  /** Who is creating: they take part by default, and may untick themselves in a shared context. */
  currentUserId?: string
  members: SpaceMember[]
  isPersonal: boolean
  /** The household's today: an edit to a series begun before it leaves the series' past as it was. */
  today: string
  onClose: () => void
}

/**
 * Writing an event or a series: which of the five calls a submission turns into, and what to say
 * when one fails. Keeping the decision next to the form is what lets EventFormModal stay presentational.
 */
export function EventFormPanel({
  spaceId, occurrence, series = null, detachSlot, defaultDate, defaultSchedule = null, currentUserId, members, isPersonal,
  today, onClose,
}: EventFormPanelProps) {
  const { t } = useTranslation('calendar')
  const [error, setError] = useState<string | null>(null)

  const createEvent = useCreateEvent(spaceId)
  const updateEvent = useUpdateEvent(spaceId)
  const detachOccurrence = useDetachOccurrence(spaceId)
  const createSeries = useCreateRecurringEventSeries(spaceId)
  const updateSeries = useUpdateRecurringEventSeries(spaceId)

  const isPending = createEvent.isPending || updateEvent.isPending || detachOccurrence.isPending
    || createSeries.isPending || updateSeries.isPending

  const blank: EventInput = {
    title: '', description: null, location: null, allDay: true,
    startDate: defaultDate, startTime: null, endDate: defaultDate, endTime: null,
    color: null, participantIds: currentUserId ? [currentUserId] : [], ...defaultSchedule,
  }
  // Editing one occurrence of a series starts from what that occurrence looks like today, but is
  // written through the slot's endpoint — so the form is pre-filled even though no event row exists.
  const initial = occurrence ? toEventInput(occurrence) : null
  const editedSeries = series ? fromSeries(series) : null

  const handleSubmit = (input: EventInput, recurrence: Recurrence | null) => {
    setError(null)
    const onError = () => setError(t('form.save_failed'))
    const onSuccess = () => onClose()

    if (series && recurrence) {
      updateSeries.mutate({ seriesId: series.id, input: toSeriesInput(input, recurrence, series) }, { onSuccess, onError })
      return
    }
    if (recurrence) {
      createSeries.mutate(toSeriesInput(input, recurrence), { onSuccess, onError })
      return
    }
    if (detachSlot) {
      detachOccurrence.mutate(
        { seriesId: detachSlot.seriesId, date: detachSlot.date, input }, { onSuccess, onError })
      return
    }
    // A materialized event has a real id to patch; anything else is a new row.
    if (occurrence?.materialized && occurrence.source === 'EVENT') {
      updateEvent.mutate({ eventId: occurrence.sourceId, input }, { onSuccess, onError })
      return
    }
    createEvent.mutate(input, { onSuccess, onError })
  }

  return (
    <EventFormModal
      initial={editedSeries?.event ?? initial ?? blank}
      mode={series ? 'edit-series' : occurrence ? 'edit' : 'create'}
      initialRecurrence={editedSeries?.recurrence ?? null}
      notice={series && runsAcross(series, today) ? t('series.split_notice') : null}
      members={members}
      isPersonal={isPersonal}
      submitError={error}
      isPending={isPending}
      onSubmit={handleSubmit}
      onCancel={onClose}
    />
  )
}
