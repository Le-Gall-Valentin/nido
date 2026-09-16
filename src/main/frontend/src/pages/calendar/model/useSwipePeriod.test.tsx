import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useSwipePeriod } from './useSwipePeriod'

function pointer(x: number, y: number) {
  return { clientX: x, clientY: y } as React.PointerEvent
}

describe('useSwipePeriod', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  function swipe(from: [number, number], to: [number, number], durationMs: number) {
    const onShift = vi.fn()
    const { result } = renderHook(() => useSwipePeriod(onShift))
    act(() => { result.current.onPointerDown(pointer(from[0], from[1])) })
    act(() => { vi.advanceTimersByTime(durationMs) })
    act(() => { result.current.onPointerUp(pointer(to[0], to[1])) })
    return onShift
  }

  it('moves to the next period on a short leftward flick', () => {
    expect(swipe([300, 200], [200, 205], 120)).toHaveBeenCalledWith(1)
  })

  it('moves to the previous period on a short rightward flick', () => {
    expect(swipe([200, 200], [320, 205], 120)).toHaveBeenCalledWith(-1)
  })

  it('ignores a slow drag, which belongs to the drag-and-drop sensor', () => {
    expect(swipe([300, 200], [200, 205], 600)).not.toHaveBeenCalled()
  })

  it('ignores a gesture too short to be a flick', () => {
    expect(swipe([300, 200], [270, 205], 100)).not.toHaveBeenCalled()
  })

  it('ignores a mostly-vertical gesture, which belongs to the scroller', () => {
    expect(swipe([300, 200], [230, 400], 120)).not.toHaveBeenCalled()
  })

  it('forgets a gesture that was cancelled', () => {
    const onShift = vi.fn()
    const { result } = renderHook(() => useSwipePeriod(onShift))
    act(() => { result.current.onPointerDown(pointer(300, 200)) })
    act(() => { result.current.onPointerCancel() })
    act(() => { result.current.onPointerUp(pointer(200, 205)) })
    expect(onShift).not.toHaveBeenCalled()
  })

  it('ignores a pointer up that never had a pointer down', () => {
    const onShift = vi.fn()
    const { result } = renderHook(() => useSwipePeriod(onShift))
    act(() => { result.current.onPointerUp(pointer(200, 205)) })
    expect(onShift).not.toHaveBeenCalled()
  })
})
