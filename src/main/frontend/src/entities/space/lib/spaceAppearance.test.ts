import { describe, it, expect } from 'vitest'
import { safeAccent, safeGlyph, PERSONAL_ACCENT, PERSONAL_GLYPH, SPACE_ACCENTS } from './spaceAppearance'

describe('safeAccent', () => {
  it('lets an allowed accent through', () => {
    expect(safeAccent('#c17a5c')).toBe('#c17a5c')
    expect(safeAccent(PERSONAL_ACCENT)).toBe(PERSONAL_ACCENT)
  })

  it('falls back for anything outside the palette', () => {
    // The accent comes from the API: it must never reach a style attribute unchecked.
    expect(safeAccent('#123456')).toBe(PERSONAL_ACCENT)
    expect(safeAccent('red; background: url(evil)')).toBe(PERSONAL_ACCENT)
    expect(safeAccent('')).toBe(PERSONAL_ACCENT)
    expect(safeAccent(undefined)).toBe(PERSONAL_ACCENT)
    expect(safeAccent(null)).toBe(PERSONAL_ACCENT)
  })

  it('exposes exactly the six group accents', () => {
    expect(SPACE_ACCENTS).toHaveLength(6)
    expect(SPACE_ACCENTS).not.toContain(PERSONAL_ACCENT)
  })
})

describe('safeGlyph', () => {
  it('lets an allowed glyph through', () => {
    expect(safeGlyph('🏡')).toBe('🏡')
    expect(safeGlyph(PERSONAL_GLYPH)).toBe(PERSONAL_GLYPH)
  })

  it('falls back for anything else', () => {
    expect(safeGlyph('💀')).toBe(PERSONAL_GLYPH)
    expect(safeGlyph('<script>')).toBe(PERSONAL_GLYPH)
    expect(safeGlyph(undefined)).toBe(PERSONAL_GLYPH)
  })
})
