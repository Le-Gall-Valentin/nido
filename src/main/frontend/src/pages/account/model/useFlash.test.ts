import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useFlash } from './useFlash'

describe('useFlash', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  it('shows a message, then lets it fade after the wait', () => {
    const { result } = renderHook(() => useFlash(3000))

    act(() => { result.current.showFlash('success', 'profile.success') })
    expect(result.current.flash).toEqual({ kind: 'success', key: 'profile.success' })

    act(() => { vi.advanceTimersByTime(3000) })
    expect(result.current.flash).toBeNull()
  })

  it('a new message replaces the last one and gets the whole wait', () => {
    const { result } = renderHook(() => useFlash(3000))

    act(() => { result.current.showFlash('success', 'first') })
    act(() => { vi.advanceTimersByTime(2000) })
    act(() => { result.current.showFlash('error', 'second', { minutes: 5 }) })
    act(() => { vi.advanceTimersByTime(2000) })

    expect(result.current.flash).toEqual({ kind: 'error', key: 'second', values: { minutes: 5 } })
  })

  it('can be taken down at once', () => {
    const { result } = renderHook(() => useFlash(3000))

    act(() => { result.current.showFlash('success', 'profile.success') })
    act(() => { result.current.clearFlash() })

    expect(result.current.flash).toBeNull()
  })
})
