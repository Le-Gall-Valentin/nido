import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { SpaceMember } from '@/entities/space'
import {
  useCreateEvent, useUpdateEvent, useDetachOccurrence,
  type CalendarOccurrence, type EventInput,
} from '@/entities/calendar'
import { EventFormModal } from './EventFormModal'

interface EventFormPanelProps {
  spaceId: string
  /** The occurrence being edited, or null when creating. */
  occurrence: CalendarOccurrence | null
  /**
   * Set when editing exactly one occurrence of a series: the write then goes to the slot's
   * upsert endpoint instead of to the event, so the rest of the series is untouched.
   */
  detachSlot: { seriesId: string; date: string } | null
  /** Pre-fills the dates when creating from a day cell. */
  defaultDate: string
  members: SpaceMember[]
  isPersonal: boolean
  onClose: () => void
}

function toInput(occurrence: CalendarOccurrence): EventInput {
  return {
    title: occurrence.title, description: null, location: null, allDay: occurrence.allDay,
    startDate: occurrence.startDate, startTime: occurrence.startTime,
    endDate: occurrence.endDate, endTime: occurrence.endTime,
    color: occurrence.color, participantIds: occurrence.participantIds,
  }
}

/**
 * Writing an event: which of the three calls a submission turns into, and what to say when one
 * fails. Keeping the decision next to the form is what lets EventFormModal stay presentational.
 */
export function EventFormPanel({
  spaceId, occurrence, detachSlot, defaultDate, members, isPersonal, onClose,
}: EventFormPanelProps) {
  const { t } = useTranslation('calendar')
  const [error, setError] = useState<string | null>(null)

  const createEvent = useCreateEvent(spaceId)
  const updateEvent = useUpdateEvent(spaceId)
  const detachOccurrence = useDetachOccurrence(spaceId)

  const isPending = createEvent.isPending || updateEvent.isPending || detachOccurrence.isPending

  const blank: EventInput = {
    title: '', description: null, location: null, allDay: true,
    startDate: defaultDate, startTime: null, endDate: defaultDate, endTime: null,
    color: null, participantIds: [],
  }
  // Editing one occurrence of a series starts from what that occurrence looks like today, but is
  // written through the slot's endpoint — so the form is pre-filled even though no event row exists.
  const initial = occurrence ? toInput(occurrence) : null

  const handleSubmit = (input: EventInput) => {
    setError(null)
    const onError = () => setError(t('form.save_failed'))
    const onSuccess = () => onClose()

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
      initial={initial ?? blank}
      mode={occurrence ? 'edit' : 'create'}
      members={members}
      isPersonal={isPersonal}
      submitError={error}
      isPending={isPending}
      onSubmit={handleSubmit}
      onCancel={onClose}
    />
  )
}
