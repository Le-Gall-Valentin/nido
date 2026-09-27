import type { ReactNode } from 'react'
import { act, renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { QueryClientProvider, useMutation } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { useInvalidateOnAnyWrite } from './useInvalidateOnAnyWrite'

function setup() {
  const client = createTestQueryClient()
  const invalidate = vi.spyOn(client, 'invalidateQueries')
  const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>
  return { client, invalidate, wrapper }
}

describe('useInvalidateOnAnyWrite', () => {
  it('refreshes the key after any write that succeeds, whoever made it', async () => {
    const { invalidate, wrapper } = setup()
    renderHook(() => useInvalidateOnAnyWrite(['dashboard', 'space-1']), { wrapper })
    const writer = renderHook(() => useMutation({ mutationFn: () => Promise.resolve('ok') }), { wrapper })

    act(() => writer.result.current.mutate())

    await waitFor(() => expect(invalidate).toHaveBeenCalledWith({ queryKey: ['dashboard', 'space-1'] }))
  })

  it('leaves it alone after a write that fails', async () => {
    const { invalidate, wrapper } = setup()
    renderHook(() => useInvalidateOnAnyWrite(['calendar', 'space-1']), { wrapper })
    const writer = renderHook(() => useMutation({ mutationFn: () => Promise.reject(new Error('refused')) }), { wrapper })

    act(() => writer.result.current.mutate())

    await waitFor(() => expect(writer.result.current.isError).toBe(true))
    expect(invalidate).not.toHaveBeenCalled()
  })

  it('stops listening once the page that asked is gone', async () => {
    const { invalidate, wrapper } = setup()
    const listener = renderHook(() => useInvalidateOnAnyWrite(['dashboard', 'space-1']), { wrapper })
    const writer = renderHook(() => useMutation({ mutationFn: () => Promise.resolve('ok') }), { wrapper })

    listener.unmount()
    act(() => writer.result.current.mutate())

    await waitFor(() => expect(writer.result.current.isSuccess).toBe(true))
    expect(invalidate).not.toHaveBeenCalled()
  })
})
