import { useEffect } from 'react'
import { useQueryClient, type QueryKey } from '@tanstack/react-query'

/**
 * Invalidates `queryKey` after every write that succeeds while the caller is mounted, whoever made it.
 *
 * For a page that shows what other modules own — the calendar, the dashboard — and opens their
 * editors: those writes refresh their own module and know nothing of the page (moving a savings
 * deadline from the calendar once left the old date on screen). Listening to every write rather than
 * naming them keeps the page true for the next one added to it too.
 */
export function useInvalidateOnAnyWrite(queryKey: QueryKey): void {
  const queryClient = useQueryClient()
  // A key is a new array on every render: the subscription follows its content, not its identity.
  const serialized = JSON.stringify(queryKey)
  useEffect(() => {
    const key = JSON.parse(serialized) as QueryKey
    return queryClient.getMutationCache().subscribe((event) => {
      if (event.type === 'updated' && event.action.type === 'success') {
        void queryClient.invalidateQueries({ queryKey: key })
      }
    })
  }, [queryClient, serialized])
}
