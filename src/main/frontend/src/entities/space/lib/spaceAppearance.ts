export const SPACE_ACCENTS = ['#c17a5c', '#4a7fa0', '#5c7a58', '#7a6f9c', '#c98aa6', '#c9a24e'] as const
export const SPACE_GLYPHS = ['🏡', '🏠', '🌷', '🌿', '🐣', '🎒', '🍀', '🚗'] as const
export const PERSONAL_ACCENT = '#8a7d6b'
export const PERSONAL_GLYPH = '👤'

const ALLOWED_ACCENTS: readonly string[] = [...SPACE_ACCENTS, PERSONAL_ACCENT]
const ALLOWED_GLYPHS: readonly string[] = [...SPACE_GLYPHS, PERSONAL_GLYPH]

/**
 * The accent is applied in a style attribute: it must never reach it exactly
 * as it came from the API. The backend already validates the palette, this
 * check is the second barrier, and the fallback guarantees a correct render
 * even if the contract changed.
 */
export function safeAccent(accent: string | null | undefined): string {
  return accent && ALLOWED_ACCENTS.includes(accent) ? accent : PERSONAL_ACCENT
}

export function safeGlyph(glyph: string | null | undefined): string {
  return glyph && ALLOWED_GLYPHS.includes(glyph) ? glyph : PERSONAL_GLYPH
}
