import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { dashboardKey } from '@/entities/dashboard'

/**
 * Refreshes the dashboard after every write that succeeds while it is open — accepting an invitation,
 * ticking a task, settling a debt, adding a task. Those writes refresh their own module and know
 * nothing of the dashboard; listening to every write, as the calendar's useRefreshAfterWrites does,
 * keeps the page true for the next action added to it too.
 */
export function useRefreshDashboardAfterWrites(spaceId: string) {
  const queryClient = useQueryClient()
  useEffect(() => queryClient.getMutationCache().subscribe((event) => {
    if (event.type === 'updated' && event.action.type === 'success') {
      void queryClient.invalidateQueries({ queryKey: dashboardKey(spaceId) })
    }
  }), [queryClient, spaceId])
}
