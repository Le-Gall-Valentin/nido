import { describe, it, expect } from 'vitest'
import { resolveItemMove } from './resolveItemMove'
import type { ShoppingItem } from '@/entities/shopping-list'

const ITEMS: ShoppingItem[] = [
  { id: 'i1', categoryId: 'cat-1', name: 'Pâtes', quantity: 500, unit: 'GRAM', done: false, position: 0 },
  { id: 'i2', categoryId: 'cat-2', name: 'Riz', quantity: null, unit: null, done: false, position: 0 },
]

describe('resolveItemMove', () => {
  it('returns the item when dropped on a different category', () => {
    expect(resolveItemMove(ITEMS, 'i1', 'cat-2')).toEqual(ITEMS[0])
  })

  it('returns null when dropped back on its own current category', () => {
    expect(resolveItemMove(ITEMS, 'i1', 'cat-1')).toBeNull()
  })

  it('returns null when the item id is not found', () => {
    expect(resolveItemMove(ITEMS, 'does-not-exist', 'cat-2')).toBeNull()
  })
})
