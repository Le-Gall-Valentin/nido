import { useQuery } from '@tanstack/react-query'
import { useDashboardApi } from './dashboardApiContext'

/** A page left open still catches up with the household: every five minutes while it is visible. */
export const DASHBOARD_REFRESH_INTERVAL_MS = 5 * 60 * 1000

export function dashboardKey(spaceId: string) {
  return ['dashboard', spaceId] as const
}

/**
 * The dashboard of a space.
 *
 * It summarises what every other module owns, and no other module's write knows it exists — so it
 * trusts no cache: it shows what it has at once and asks again on every visit (`staleTime: 0`, as the
 * calendar's occurrences do), on focus, and on a timer while visible. Writes made from the page itself
 * are caught by the page's useRefreshDashboardAfterWrites.
 */
export function useDashboard(spaceId: string | undefined) {
  const api = useDashboardApi()
  return useQuery({
    queryKey: dashboardKey(spaceId ?? ''),
    queryFn: () => api.getDashboard(spaceId as string),
    enabled: !!spaceId,
    staleTime: 0,
    refetchOnWindowFocus: true,
    refetchInterval: DASHBOARD_REFRESH_INTERVAL_MS,
    refetchIntervalInBackground: false,
  })
}
