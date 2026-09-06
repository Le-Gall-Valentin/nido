import { describe, expect, it } from 'vitest'
import { Circle, Home } from 'lucide-react'
import { resolveCategoryIcon } from './resolveCategoryIcon'

describe('resolveCategoryIcon', () => {
  it('resolves a known lucide icon name', () => {
    expect(resolveCategoryIcon('Home')).toBe(Home)
  })

  it('falls back to Circle for an unknown icon name', () => {
    expect(resolveCategoryIcon('NotARealIcon')).toBe(Circle)
  })
})
