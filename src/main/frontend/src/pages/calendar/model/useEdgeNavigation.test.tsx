import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useEdgeNavigation } from './useEdgeNavigation'

describe('useEdgeNavigation', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  it('changes period while the pointer holds still at an edge — no pointer move needed', () => {
    const onShift = vi.fn()
    const { result } = renderHook(() => useEdgeNavigation(onShift))
    act(() => { result.current.update(1) })
    act(() => { vi.advanceTimersByTime(700) })
    expect(onShift).toHaveBeenCalledWith(1)
    expect(result.current.armed).toBe(1)
  })

  it('stops counting once the drag is over, so nothing flips a second later', () => {
    const onShift = vi.fn()
    const { result } = renderHook(() => useEdgeNavigation(onShift))
    act(() => { result.current.update(1) })
    act(() => { vi.advanceTimersByTime(300) })
    act(() => { result.current.stop() })
    act(() => { vi.advanceTimersByTime(2000) })
    expect(onShift).not.toHaveBeenCalled()
    expect(result.current.armed).toBe(0)
  })

  it('calls the newest onShift on a repeat, not the one captured when counting started', () => {
    // Otherwise holding at the edge goes "next month" once, then keeps asking for the same month.
    const first = vi.fn()
    const second = vi.fn()
    const { result, rerender } = renderHook(({ cb }) => useEdgeNavigation(cb), { initialProps: { cb: first } })
    act(() => { result.current.update(1) })
    act(() => { vi.advanceTimersByTime(650) })
    rerender({ cb: second })
    act(() => { vi.advanceTimersByTime(950) })
    expect(first).toHaveBeenCalledTimes(1)
    expect(second).toHaveBeenCalledTimes(1)
  })

  it('stops its timer when the page unmounts mid-drag', () => {
    const onShift = vi.fn()
    const { result, unmount } = renderHook(() => useEdgeNavigation(onShift))
    act(() => { result.current.update(-1) })
    unmount()
    act(() => { vi.advanceTimersByTime(2000) })
    expect(onShift).not.toHaveBeenCalled()
  })
})
