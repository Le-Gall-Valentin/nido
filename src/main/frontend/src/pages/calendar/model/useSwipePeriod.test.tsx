import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useSwipePeriod } from './useSwipePeriod'

function touches(...points: Array<[number, number]>) {
  const list = points.map(([x, y]) => ({ clientX: x, clientY: y }))
  return { touches: list, changedTouches: list } as unknown as React.TouchEvent
}

/** A finger lifting: nothing left on the screen, the lifted one in changedTouches. */
function lift(x: number, y: number) {
  return { touches: [], changedTouches: [{ clientX: x, clientY: y }] } as unknown as React.TouchEvent
}

describe('useSwipePeriod', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  function swipe(from: [number, number], to: [number, number], durationMs: number) {
    const onShift = vi.fn()
    const { result } = renderHook(() => useSwipePeriod(onShift))
    act(() => { result.current.handlers.onTouchStart(touches(from)) })
    act(() => { vi.advanceTimersByTime(durationMs) })
    act(() => { result.current.handlers.onTouchEnd(lift(to[0], to[1])) })
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

  it('reads touches, never pointers — a flick is a pan, and a pan cancels the pointer', () => {
    // Chrome sends pointercancel as soon as it takes a horizontal flick as a pan, so a swipe read
    // from pointer events never fired on a real phone. Touches still end normally. A mouse sends
    // no touches, so it can never swipe either.
    const { result } = renderHook(() => useSwipePeriod(vi.fn()))
    expect(Object.keys(result.current.handlers).sort()).toEqual(['onTouchCancel', 'onTouchEnd', 'onTouchStart'])
  })

  it('ignores a two-finger gesture, which is a pinch', () => {
    const onShift = vi.fn()
    const { result } = renderHook(() => useSwipePeriod(onShift))
    act(() => { result.current.handlers.onTouchStart(touches([300, 200], [100, 200])) })
    act(() => { result.current.handlers.onTouchEnd(lift(150, 205)) })
    expect(onShift).not.toHaveBeenCalled()
  })

  it('gives up a gesture once a drag has taken it over', () => {
    // The drag starts during the move, so it always arrives before the finger lifts — no
    // dependence on which listener hears the release first.
    const onShift = vi.fn()
    const { result } = renderHook(() => useSwipePeriod(onShift))
    act(() => { result.current.handlers.onTouchStart(touches([300, 200])) })
    act(() => { result.current.cancel() })
    act(() => { result.current.handlers.onTouchEnd(lift(150, 205)) })
    expect(onShift).not.toHaveBeenCalled()
  })

  it('forgets a gesture that was cancelled', () => {
    const onShift = vi.fn()
    const { result } = renderHook(() => useSwipePeriod(onShift))
    act(() => { result.current.handlers.onTouchStart(touches([300, 200])) })
    act(() => { result.current.handlers.onTouchCancel() })
    act(() => { result.current.handlers.onTouchEnd(lift(200, 205)) })
    expect(onShift).not.toHaveBeenCalled()
  })

  it('ignores a touch end that never had a touch start', () => {
    const onShift = vi.fn()
    const { result } = renderHook(() => useSwipePeriod(onShift))
    act(() => { result.current.handlers.onTouchEnd(lift(200, 205)) })
    expect(onShift).not.toHaveBeenCalled()
  })
})
