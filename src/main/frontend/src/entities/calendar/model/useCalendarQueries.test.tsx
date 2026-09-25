import { describe, it, expect, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { CalendarApiProvider, type CalendarApi } from './calendarApiContext'
import { useOccurrences } from './useCalendarQueries'

describe('useOccurrences', () => {
  it('reads the calendar afresh each time it is opened, even within the app-wide freshness', async () => {
    // The calendar shows what every other module writes, and none of them refreshes it: a task edited
    // on the tasks page moments before must not come back with its old date.
    const listOccurrences = vi.fn().mockResolvedValue([])
    const api = { listOccurrences } as unknown as CalendarApi
    // As the app configures it: half a minute before any query counts as stale.
    const client = new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: false } } })
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={client}><CalendarApiProvider api={api}>{children}</CalendarApiProvider></QueryClientProvider>
    )

    const first = renderHook(() => useOccurrences('space-1', '2026-09-21', '2026-09-27'), { wrapper })
    await waitFor(() => expect(first.result.current.isSuccess).toBe(true))
    first.unmount()
    const again = renderHook(() => useOccurrences('space-1', '2026-09-21', '2026-09-27'), { wrapper })
    await waitFor(() => expect(again.result.current.isSuccess).toBe(true))

    await waitFor(() => expect(listOccurrences).toHaveBeenCalledTimes(2))
  })
})
