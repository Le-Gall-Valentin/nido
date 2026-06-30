import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useDebouncedValue } from './useDebouncedValue'

beforeEach(() => { vi.useFakeTimers() })
afterEach(() => { vi.useRealTimers() })

describe('useDebouncedValue', () => {
  it('returns the initial value immediately', () => {
    const { result } = renderHook(() => useDebouncedValue('a', 300))
    expect(result.current).toBe('a')
  })

  it('does not update before the delay elapses', () => {
    const { result, rerender } = renderHook(({ v }) => useDebouncedValue(v, 300), {
      initialProps: { v: 'a' },
    })
    rerender({ v: 'ab' })
    act(() => { vi.advanceTimersByTime(200) })
    expect(result.current).toBe('a')
  })

  it('updates after the delay', () => {
    const { result, rerender } = renderHook(({ v }) => useDebouncedValue(v, 300), {
      initialProps: { v: 'a' },
    })
    rerender({ v: 'ab' })
    act(() => { vi.advanceTimersByTime(300) })
    expect(result.current).toBe('ab')
  })

  it('restarts the timer on every change (only the last value wins)', () => {
    const { result, rerender } = renderHook(({ v }) => useDebouncedValue(v, 300), {
      initialProps: { v: 'a' },
    })
    rerender({ v: 'ab' })
    act(() => { vi.advanceTimersByTime(200) })
    rerender({ v: 'abc' })
    act(() => { vi.advanceTimersByTime(200) })
    expect(result.current).toBe('a')
    act(() => { vi.advanceTimersByTime(100) })
    expect(result.current).toBe('abc')
  })
})
