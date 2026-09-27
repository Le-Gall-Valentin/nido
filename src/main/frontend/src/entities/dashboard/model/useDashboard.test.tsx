import type { ReactNode } from 'react'
import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, it, expect, vi } from 'vitest'
import { QueryClient, QueryClientProvider, focusManager } from '@tanstack/react-query'
import { DashboardApiProvider } from './dashboardApiContext'
import { useDashboard, dashboardKey } from './useDashboard'
import type { IDashboardApi } from './IDashboardApi'
import type { Dashboard } from './types'

const DASHBOARD: Dashboard = { date: '2026-09-26', spaceType: 'SHARED', canWrite: true, complete: true, attention: [], cards: {} }

/** The app's own defaults (app/App.tsx): data kept fresh for 30 s, no refetch on focus. */
function appLikeClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: 30_000, refetchOnWindowFocus: false } } })
}

function setup() {
  const api: IDashboardApi = { getDashboard: vi.fn().mockResolvedValue(DASHBOARD) }
  const client = appLikeClient()
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}><DashboardApiProvider api={api}>{children}</DashboardApiProvider></QueryClientProvider>
  )
  return { api, wrapper }
}

afterEach(() => focusManager.setFocused(undefined))

describe('useDashboard', () => {
  it('keys the query by space', () => {
    expect(dashboardKey('space-1')).toEqual(['dashboard', 'space-1'])
  })

  it('refetches on every visit, even though the app keeps other data fresh for 30 s', async () => {
    const { api, wrapper } = setup()
    const first = renderHook(() => useDashboard('space-1'), { wrapper })
    await waitFor(() => expect(first.result.current.isSuccess).toBe(true))
    first.unmount()

    renderHook(() => useDashboard('space-1'), { wrapper })

    await waitFor(() => expect(api.getDashboard).toHaveBeenCalledTimes(2))
  })

  it('refetches when the window comes back into focus', async () => {
    const { api, wrapper } = setup()
    const { result } = renderHook(() => useDashboard('space-1'), { wrapper })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    act(() => {
      focusManager.setFocused(false)
      focusManager.setFocused(true)
    })

    await waitFor(() => expect(api.getDashboard).toHaveBeenCalledTimes(2))
  })

  it('asks nothing before the space is known', () => {
    const { api, wrapper } = setup()
    renderHook(() => useDashboard(undefined), { wrapper })
    expect(api.getDashboard).not.toHaveBeenCalled()
  })
})
