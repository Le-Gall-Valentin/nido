import { describe, it, expect } from 'vitest'
import { avatarHue } from './avatarHue'

describe('avatarHue', () => {
  it('gives an id the same hue on every call', () => {
    expect(avatarHue('3f2b8c1e-5d4a-4e7b-9c1f-2a6d8e0b7c35')).toBe(avatarHue('3f2b8c1e-5d4a-4e7b-9c1f-2a6d8e0b7c35'))
  })

  it('stays on the colour wheel', () => {
    for (let i = 0; i < 500; i++) {
      const hue = avatarHue(`user-${i}`)
      expect(Number.isInteger(hue)).toBe(true)
      expect(hue).toBeGreaterThanOrEqual(0)
      expect(hue).toBeLessThan(360)
    }
  })

  it('spreads ids that differ by one character around the whole wheel', () => {
    const sectors = new Set(Array.from({ length: 120 }, (_, i) => Math.floor(avatarHue(`user-${i}`) / 30)))
    expect(sectors.size).toBe(12)
  })
})
