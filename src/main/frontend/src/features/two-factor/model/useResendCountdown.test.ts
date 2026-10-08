import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useResendCountdown } from './useResendCountdown'

describe('useResendCountdown', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  it('counts down to zero from what the server said, then stops', () => {
    const { result } = renderHook(() => useResendCountdown(2))
    expect(result.current.seconds).toBe(2)

    act(() => { vi.advanceTimersByTime(1000) })
    expect(result.current.seconds).toBe(1)
    act(() => { vi.advanceTimersByTime(1000) })
    expect(result.current.seconds).toBe(0)
    act(() => { vi.advanceTimersByTime(5000) })
    expect(result.current.seconds).toBe(0)
  })

  it('starts again when a new code leaves', () => {
    const { result } = renderHook(() => useResendCountdown(0))

    act(() => { result.current.restart(60) })

    expect(result.current.seconds).toBe(60)
  })
})
