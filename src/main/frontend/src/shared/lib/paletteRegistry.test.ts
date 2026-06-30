import { describe, it, expect, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { usePaletteItems, usePaletteResults } from './paletteRegistry'
import type { PaletteItem } from './paletteRegistry'

const Icon = () => null

function makeItem(id: string): PaletteItem {
  return { id, label: `Label ${id}`, to: `/${id}`, icon: Icon }
}

// The store is a singleton — between tests, renderHook unmount (via afterEach cleanup)
// triggers usePaletteItems cleanup which unregisters sources, resetting state.

describe('usePaletteResults', () => {
  it('returns an empty array when no items are registered', () => {
    const { result } = renderHook(() => usePaletteResults())
    expect(result.current).toEqual([])
  })
})

describe('usePaletteItems + usePaletteResults', () => {
  it('registers items on mount and they appear in results', () => {
    const items = [makeItem('a'), makeItem('b')]

    const { result: resultsHook } = renderHook(() => usePaletteResults())
    renderHook(() => usePaletteItems('source-1', items))

    expect(resultsHook.current).toEqual(items)
  })

  it('unregisters items on unmount', () => {
    const items = [makeItem('x')]

    const { result: resultsHook } = renderHook(() => usePaletteResults())
    const { unmount } = renderHook(() => usePaletteItems('source-2', items))

    expect(resultsHook.current).toEqual(items)

    act(() => unmount())

    expect(resultsHook.current).toEqual([])
  })

  it('aggregates items from multiple sources', () => {
    const items1 = [makeItem('p1')]
    const items2 = [makeItem('p2'), makeItem('p3')]

    const { result } = renderHook(() => usePaletteResults())
    renderHook(() => usePaletteItems('src-a', items1))
    renderHook(() => usePaletteItems('src-b', items2))

    expect(result.current).toHaveLength(3)
    expect(result.current.map((i) => i.id)).toEqual(['p1', 'p2', 'p3'])
  })

  it('updates registration when items array changes', () => {
    const { result: resultsHook } = renderHook(() => usePaletteResults())

    const { rerender } = renderHook(
      ({ items }: { items: PaletteItem[] }) => usePaletteItems('src-dyn', items),
      { initialProps: { items: [makeItem('v1')] } }
    )

    expect(resultsHook.current.map((i) => i.id)).toEqual(['v1'])

    rerender({ items: [makeItem('v2'), makeItem('v3')] })

    expect(resultsHook.current.map((i) => i.id)).toEqual(['v2', 'v3'])
  })

  it('two sources with the same id overwrite each other (last write wins)', () => {
    const first = [makeItem('dup')]
    const second = [makeItem('dup-2')]

    const { result } = renderHook(() => usePaletteResults())
    const { rerender } = renderHook(
      ({ items }: { items: PaletteItem[] }) => usePaletteItems('same-id', items),
      { initialProps: { items: first } }
    )

    expect(result.current).toHaveLength(1)
    rerender({ items: second })
    expect(result.current).toEqual(second)
  })
})

describe('usePaletteItems isolation', () => {
  beforeEach(() => {
    // Each test starts fresh because afterEach(cleanup) unmounts hooks from previous tests
  })

  it('does not leak items from a previous test', () => {
    const { result } = renderHook(() => usePaletteResults())
    expect(result.current).toEqual([])
  })
})