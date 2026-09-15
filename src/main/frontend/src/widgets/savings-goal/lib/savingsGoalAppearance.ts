export const SAVINGS_GOAL_COLORS = ['#5c7a58', '#c17a5c', '#4a7fa0', '#7a6f9c', '#c9736a'] as const
export const SAVINGS_GOAL_GLYPHS = ['🎯', '🏖️', '🏠', '🚗', '🎁', '💍', '🎓', '🛟'] as const

const DEFAULT_COLOR: string = SAVINGS_GOAL_COLORS[0]
const DEFAULT_GLYPH: string = SAVINGS_GOAL_GLYPHS[0]

/**
 * The color is applied in a style attribute: it must never reach it exactly
 * as it came from the API. The backend already validates the palette, this
 * check is the second barrier, and the fallback guarantees a correct render
 * even if the contract changed.
 */
export function safeSavingsGoalColor(color: string | null | undefined): string {
  return color && (SAVINGS_GOAL_COLORS as readonly string[]).includes(color) ? color : DEFAULT_COLOR
}

export function safeSavingsGoalGlyph(glyph: string | null | undefined): string {
  return glyph && (SAVINGS_GOAL_GLYPHS as readonly string[]).includes(glyph) ? glyph : DEFAULT_GLYPH
}
