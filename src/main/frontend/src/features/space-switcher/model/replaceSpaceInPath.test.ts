import { describe, it, expect } from 'vitest'
import { replaceSpaceInPath } from './replaceSpaceInPath'

describe('replaceSpaceInPath', () => {
  it('swaps the space id while keeping the rest of the path', () => {
    expect(replaceSpaceInPath('/s/space-2/kitchen/recipes', 'personal-1')).toBe('/s/personal-1/kitchen/recipes')
  })

  it('swaps the space id on a bare space path', () => {
    expect(replaceSpaceInPath('/s/space-2', 'personal-1')).toBe('/s/personal-1')
  })

  it('swaps the space id on a deeply nested path', () => {
    expect(replaceSpaceInPath('/s/space-2/kitchen/recipes/r1', 'personal-1')).toBe('/s/personal-1/kitchen/recipes/r1')
  })

  it('leaves an unscoped path untouched', () => {
    expect(replaceSpaceInPath('/account/profile', 'personal-1')).toBe('/account/profile')
  })
})
