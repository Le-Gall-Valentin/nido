export const SPACE_ACCENTS = ['#c17a5c', '#4a7fa0', '#5c7a58', '#7a6f9c', '#c98aa6', '#c9a24e'] as const
export const SPACE_GLYPHS = ['🏡', '🏠', '🌷', '🌿', '🐣', '🎒', '🍀', '🚗'] as const
export const PERSONAL_ACCENT = '#8a7d6b'
export const PERSONAL_GLYPH = '👤'

const ALLOWED_ACCENTS: readonly string[] = [...SPACE_ACCENTS, PERSONAL_ACCENT]
const ALLOWED_GLYPHS: readonly string[] = [...SPACE_GLYPHS, PERSONAL_GLYPH]

/**
 * L'accent est appliqué dans un attribut de style : il ne doit jamais y arriver
 * tel qu'il est venu de l'API. Le backend valide déjà la palette, ce contrôle
 * est la seconde barrière, et le repli garantit un rendu correct même si le
 * contrat changeait.
 */
export function safeAccent(accent: string | null | undefined): string {
  return accent && ALLOWED_ACCENTS.includes(accent) ? accent : PERSONAL_ACCENT
}

export function safeGlyph(glyph: string | null | undefined): string {
  return glyph && ALLOWED_GLYPHS.includes(glyph) ? glyph : PERSONAL_GLYPH
}
