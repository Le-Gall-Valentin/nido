import { act, renderHook } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ForbiddenError } from '@/shared/lib'
import { useDialogSubmit } from './useDialogSubmit'

describe('useDialogSubmit', () => {
  it('runs the action once, however often it is asked while in flight', async () => {
    let finish: () => void = () => {}
    const action = vi.fn(() => new Promise<void>((resolve) => { finish = resolve }))
    const { result } = renderHook(() => useDialogSubmit('delete'))

    let first: Promise<void> = Promise.resolve()
    act(() => { first = result.current.submit(action) })
    expect(result.current.isLoading).toBe(true)
    await act(async () => { await result.current.submit(action) })
    await act(async () => { finish(); await first })

    expect(action).toHaveBeenCalledTimes(1)
    expect(result.current.isLoading).toBe(false)
  })

  it('says what went wrong in the words of its dialog, then forgets it on demand', async () => {
    const { result } = renderHook(() => useDialogSubmit('delete'))

    await act(async () => { await result.current.submit(() => Promise.reject(new ForbiddenError())) })
    expect(result.current.errorKey).toBe('delete.error.forbidden')
    expect(result.current.isLoading).toBe(false)

    act(() => { result.current.clearError() })
    expect(result.current.errorKey).toBeNull()
  })

  it('forgets an earlier failure when it is asked again', async () => {
    const { result } = renderHook(() => useDialogSubmit('delete'))
    await act(async () => { await result.current.submit(() => Promise.reject(new ForbiddenError())) })

    await act(async () => { await result.current.submit(() => Promise.resolve()) })

    expect(result.current.errorKey).toBeNull()
  })
})
