import { describe, expect, it } from 'vitest'
import { ALL_ICON_NAMES } from './lucideIconNames'

describe('ALL_ICON_NAMES', () => {
  it('includes common icons already used as default category icons', () => {
    expect(ALL_ICON_NAMES).toContain('Wallet')
    expect(ALL_ICON_NAMES).toContain('Home')
    expect(ALL_ICON_NAMES).toContain('Circle')
  })

  it('excludes non-icon lucide-react exports', () => {
    expect(ALL_ICON_NAMES).not.toContain('createLucideIcon')
    expect(ALL_ICON_NAMES).not.toContain('icons')
    expect(ALL_ICON_NAMES).not.toContain('default')
  })

  it('contains no duplicate names', () => {
    expect(new Set(ALL_ICON_NAMES).size).toBe(ALL_ICON_NAMES.length)
  })

  it('is sorted alphabetically', () => {
    expect(ALL_ICON_NAMES).toEqual([...ALL_ICON_NAMES].sort())
  })
})
