import type { ReactNode } from 'react'
import { act, renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { QueryClientProvider, useMutation } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { useRefreshDashboardAfterWrites } from './useRefreshDashboardAfterWrites'

function setup(mutationFn: () => Promise<string>) {
  const client = createTestQueryClient()
  const invalidate = vi.spyOn(client, 'invalidateQueries')
  const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>
  const { result } = renderHook(() => {
    useRefreshDashboardAfterWrites('space-1')
    return useMutation({ mutationFn })
  }, { wrapper })
  return { result, invalidate }
}

describe('useRefreshDashboardAfterWrites', () => {
  it('refreshes the dashboard after any write that succeeds', async () => {
    const { result, invalidate } = setup(async () => 'ok')

    act(() => result.current.mutate())

    await waitFor(() => expect(invalidate).toHaveBeenCalledWith({ queryKey: ['dashboard', 'space-1'] }))
  })

  it('leaves it alone after a write that fails', async () => {
    const { result, invalidate } = setup(async () => { throw new Error('refused') })

    act(() => result.current.mutate())

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(invalidate).not.toHaveBeenCalled()
  })
})
