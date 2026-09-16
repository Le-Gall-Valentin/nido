import { useWritableSpaces } from '@/features/space-switcher'
import { TransferDialog } from '@/features/transfer-to-space'
import { useCopyEvent, useMoveEvent, type CalendarOccurrence } from '@/entities/calendar'

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
 * Legal from a page, and only from a page: it reaches into two different features, which is what
 * the widgets layer exists to allow elsewhere.
 */
export function TransferEventPanel({ spaceId, occurrence, operation, onClose }: TransferEventPanelProps) {
  const { data: destinations } = useWritableSpaces(spaceId)
  const copyEvent = useCopyEvent(spaceId)
  const moveEvent = useMoveEvent(spaceId)

  return (
    <TransferDialog
      itemName={occurrence.title}
      operation={operation}
      destinations={destinations ?? []}
      onClose={onClose}
      onConfirm={async (destinationSpaceId) => {
        const mutation = operation === 'copy' ? copyEvent : moveEvent
        await mutation.mutateAsync({ eventId: occurrence.sourceId, destinationSpaceId })
        onClose()
      }}
    />
  )
}
