import { useWritableSpaces } from '@/features/space-switcher'
import { TransferDialog } from '@/features/transfer-to-space'
import {
  useCopyEvent, useCopyOccurrence, useMoveEvent, useMoveOccurrence, type CalendarOccurrence,
} from '@/entities/calendar'

interface TransferEventPanelProps {
  spaceId: string
  occurrence: CalendarOccurrence
  operation: 'copy' | 'move'
  onClose: () => void
}

/**
 * Sending an event to another context. Both the destinations the caller may write to and the call
 * itself are asked for only once this is open, which is also the only time either means anything.
 *
 * An occurrence still to come has no event of its own: it is sent by its series and its day, and the
 * server makes it one in the destination. Any other — a single event, or an occurrence edited on its
 * own — is sent as the event it is.
 *
 * Legal from a page, and only from a page: it reaches into two different features, which is what
 * the widgets layer exists to allow elsewhere.
 */
export function TransferEventPanel({ spaceId, occurrence, operation, onClose }: TransferEventPanelProps) {
  const { data: destinations } = useWritableSpaces(spaceId)
  const copyEvent = useCopyEvent(spaceId)
  const moveEvent = useMoveEvent(spaceId)
  const copyOccurrence = useCopyOccurrence(spaceId)
  const moveOccurrence = useMoveOccurrence(spaceId)

  return (
    <TransferDialog
      itemName={occurrence.title}
      operation={operation}
      destinations={destinations ?? []}
      onClose={onClose}
      onConfirm={async (destinationSpaceId) => {
        const { seriesId, originalDate } = occurrence
        if (!occurrence.materialized && seriesId && originalDate) {
          const mutation = operation === 'copy' ? copyOccurrence : moveOccurrence
          await mutation.mutateAsync({ seriesId, date: originalDate, destinationSpaceId })
        } else {
          const mutation = operation === 'copy' ? copyEvent : moveEvent
          await mutation.mutateAsync({ eventId: occurrence.sourceId, destinationSpaceId })
        }
        onClose()
      }}
    />
  )
}
