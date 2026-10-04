import { act, renderHook } from '@testing-library/react'
import { StrictMode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useCollapsedGroups } from './useCollapsedGroups'

const KEY = 'nido.notification-groups.collapsed'

describe('useCollapsedGroups', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => vi.restoreAllMocks())

  it('writes each fold once, even where React runs an update twice', () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem')
    const { result } = renderHook(() => useCollapsedGroups(), { wrapper: StrictMode })

    act(() => result.current.toggle('space'))

    expect(setItem).toHaveBeenCalledTimes(1)
    expect(result.current.isCollapsed('space')).toBe(true)
  })

  it('unfolds what was folded, and remembers that too', () => {
    const { result } = renderHook(() => useCollapsedGroups())

    act(() => result.current.toggle('space'))
    act(() => result.current.toggle('space'))

    expect(result.current.isCollapsed('space')).toBe(false)
    expect(localStorage.getItem(KEY)).toBe('[]')
  })
})
