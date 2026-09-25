import { useNavigate } from 'react-router-dom'
import { ROUTES } from '@/shared/config'
import { useSpaceMembers } from '@/entities/space'
import { useAuth } from '@/features/auth'
import {
  useJoinEvent, useLeaveEvent, useRecurringEventSeries, type CalendarOccurrence,
} from '@/entities/calendar'
import type { CalendarDialogState } from '../model/useCalendarDialog'
import { DayDetailModal } from './DayDetailModal'
import { OccurrenceRouter } from './OccurrenceRouter'
import { EventDetailModal } from './EventDetailModal'
import { EventFormPanel } from './EventFormPanel'
import { DeleteEventPanel } from './DeleteEventPanel'
import { DeleteSeriesPanel } from './DeleteSeriesPanel'
import { TransferEventPanel } from './TransferEventPanel'
import { MealEntryModal } from './MealEntryModal'
import { OccurrenceScopeDialog } from './OccurrenceScopeDialog'
import { RecurringEventSeriesPanel } from './RecurringEventSeriesPanel'

interface CalendarDialogsProps {
  spaceId: string
  dialog: CalendarDialogState
  /** The occurrences on screen, which the open detail is read from afresh after every write. */
  occurrences: CalendarOccurrence[]
  /** The day the calendar shows, where a form opened from nowhere in particular starts. */
  date: string
  today: string
  canWrite: boolean
  isPersonal: boolean
}

/** Whichever dialog the calendar has open, wired to the next one it leads to. */
export function CalendarDialogs({
  spaceId, dialog, occurrences, date, today, canWrite, isPersonal,
}: CalendarDialogsProps) {
  const navigate = useNavigate()
  const { open, show, close, act, answerScope, closeForm } = dialog
  const { data: members = [] } = useSpaceMembers(spaceId)
  // Whether the caller is already a participant decides between offering "join" and "leave".
  const currentUserId = useAuth((state) => state.user?.id) ?? ''
  const joinEvent = useJoinEvent(spaceId)
  const leaveEvent = useLeaveEvent(spaceId)
  // Loaded only once a series is acted on: the feed carries its id, the form needs all of it.
  const { data: seriesList } = useRecurringEventSeries(open?.kind === 'series' ? spaceId : undefined)

  const showOccurrence = (occurrence: CalendarOccurrence) => show({ kind: 'occurrence', sourceId: occurrence.sourceId })

  switch (open?.kind) {
    case undefined:
      return null

    case 'day':
      return (
        <DayDetailModal date={open.date} occurrences={occurrences} canWrite={canWrite}
          onCreateEvent={(day) => show({ kind: 'create', date: day, returnToDay: true })}
          onSelectOccurrence={showOccurrence} onClose={close} />
      )

    case 'occurrence': {
      const occurrence = occurrences.find((candidate) => candidate.sourceId === open.sourceId)
      if (!occurrence) return null
      if (occurrence.source !== 'EVENT') {
        return (
          <OccurrenceRouter spaceId={spaceId} occurrence={occurrence} members={members}
            onOpenInModule={(item) => {
              close()
              // A task still to come has no row to open here: its series lives on the tasks page.
              if (item.source === 'TASK') void navigate(ROUTES.spaceOrganisationTasks(spaceId))
              if (item.source === 'MEAL') show({ kind: 'meal', date: item.startDate })
            }}
            onClose={close} />
        )
      }
      return (
        <EventDetailModal occurrence={occurrence} members={members} currentUserId={currentUserId}
          canWrite={canWrite} isPersonal={isPersonal}
          onEdit={() => act(occurrence, 'edit')} onDelete={() => act(occurrence, 'delete')}
          onCopy={() => show({ kind: 'transfer', occurrence, operation: 'copy' })}
          onMove={() => show({ kind: 'transfer', occurrence, operation: 'move' })}
          onJoin={() => joinEvent.mutate(occurrence.sourceId)} onLeave={() => leaveEvent.mutate(occurrence.sourceId)}
          onClose={close} />
      )
    }

    case 'scope':
      return <OccurrenceScopeDialog action={open.action} onChoose={answerScope} onCancel={close} />

    case 'create':
    case 'edit':
      return (
        // Keyed by kind, so a form never inherits the state of the one before it.
        <EventFormPanel key={open.kind} spaceId={spaceId}
          occurrence={open.kind === 'edit' ? open.occurrence : null}
          detachSlot={open.kind === 'edit' ? open.detachSlot : null}
          defaultDate={open.kind === 'create' ? open.date : date}
          defaultSchedule={open.kind === 'create' ? open.schedule ?? null : null}
          currentUserId={currentUserId} members={members} isPersonal={isPersonal} today={today}
          onClose={closeForm} />
      )

    case 'delete':
      return <DeleteEventPanel spaceId={spaceId} occurrence={open.occurrence} onClose={close} />

    case 'series': {
      const series = seriesList?.find((candidate) => candidate.id === open.seriesId)
      if (!series) return null
      return open.action === 'edit'
        ? <EventFormPanel key="series" spaceId={spaceId} occurrence={null} series={series} detachSlot={null}
            defaultDate={date} currentUserId={currentUserId} members={members} isPersonal={isPersonal}
            today={today} onClose={close} />
        : <DeleteSeriesPanel spaceId={spaceId} series={series} today={today} onClose={close} />
    }

    case 'transfer':
      return <TransferEventPanel spaceId={spaceId} occurrence={open.occurrence} operation={open.operation} onClose={close} />

    case 'meal':
      return <MealEntryModal spaceId={spaceId} date={open.date} onClose={close} />

    case 'manage-series':
      return (
        <RecurringEventSeriesPanel spaceId={spaceId} members={members} currentUserId={currentUserId}
          isPersonal={isPersonal} today={today} onClose={close} />
      )
  }
}
