import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { calendarKey } from '@/entities/calendar'

/**
 * Refreshes the calendar after every write that succeeds while it is open.
 *
 * It shows what other modules own — tasks, money, meals, savings — and opens their own editors, whose
 * writes refresh their module and know nothing of the calendar: moving a savings deadline from here
 * left the old date on screen. Listening to every write rather than naming them keeps it true for the
 * next source too.
 */
export function useRefreshAfterWrites(spaceId: string) {
  const queryClient = useQueryClient()
  useEffect(() => queryClient.getMutationCache().subscribe((event) => {
    if (event.type === 'updated' && event.action.type === 'success') {
      void queryClient.invalidateQueries({ queryKey: calendarKey(spaceId) })
    }
  }), [queryClient, spaceId])
}
