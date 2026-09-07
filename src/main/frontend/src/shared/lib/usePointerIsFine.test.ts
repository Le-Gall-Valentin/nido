import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { usePointerIsFine } from './usePointerIsFine'

function mockMatchMedia(matches: boolean) {
  const listeners: Array<(e: MediaQueryListEvent) => void> = []
  const mql = {
    matches,
    addEventListener: (_: string, listener: (e: MediaQueryListEvent) => void) => listeners.push(listener),
    removeEventListener: vi.fn(),
  }
  window.matchMedia = vi.fn().mockReturnValue(mql)
  return { mql, fireChange: (next: boolean) => { mql.matches = next; listeners.forEach((l) => l({ matches: next } as MediaQueryListEvent)) } }
}

describe('usePointerIsFine', () => {
  const originalMatchMedia = window.matchMedia

  beforeEach(() => {})
  afterEach(() => { window.matchMedia = originalMatchMedia })

  it('returns true when the pointer is fine (mouse)', () => {
    mockMatchMedia(true)
    const { result } = renderHook(() => usePointerIsFine())
    expect(result.current).toBe(true)
  })

  it('returns false when the pointer is coarse (touch)', () => {
    mockMatchMedia(false)
    const { result } = renderHook(() => usePointerIsFine())
    expect(result.current).toBe(false)
  })

  it('updates when the underlying media query changes', () => {
    const { fireChange } = mockMatchMedia(false)
    const { result } = renderHook(() => usePointerIsFine())
    expect(result.current).toBe(false)

    act(() => fireChange(true))

    expect(result.current).toBe(true)
  })
})
