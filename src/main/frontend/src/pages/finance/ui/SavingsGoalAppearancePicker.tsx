import { useTranslation } from 'react-i18next'
import { SAVINGS_GOAL_COLORS, SAVINGS_GOAL_GLYPHS } from '../lib/savingsGoalAppearance'

interface SavingsGoalAppearancePickerProps {
  color: string
  onColorChange: (color: string) => void
  glyph: string
  onGlyphChange: (glyph: string) => void
}

/** Color and glyph pickers for a savings goal, mirroring the space appearance picker: both options lists come straight from the validated palette, so a value handed back is always palette-safe. */
export function SavingsGoalAppearancePicker({ color, onColorChange, glyph, onGlyphChange }: SavingsGoalAppearancePickerProps) {
  const { t } = useTranslation('finance')

  return (
    <>
      <div>
        <span className="mb-2 block text-[13px] font-semibold text-fg-1">{t('savings.color')}</span>
        <div className="flex flex-wrap gap-2">
          {SAVINGS_GOAL_COLORS.map((c) => (
            <button
              key={c}
              type="button"
              aria-label={t('savings.color_option', { color: c })}
              aria-pressed={color === c}
              onClick={() => onColorChange(c)}
              className={`size-8 rounded-[9px] transition-shadow ${color === c ? 'ring-2 ring-fg-0 ring-offset-2 ring-offset-bg-1' : ''}`}
              style={{ background: c }}
            />
          ))}
        </div>
      </div>

      <div>
        <span className="mb-2 block text-[13px] font-semibold text-fg-1">{t('savings.glyph')}</span>
        <div className="flex flex-wrap gap-2">
          {SAVINGS_GOAL_GLYPHS.map((g) => (
            <button
              key={g}
              type="button"
              aria-label={t('savings.glyph_option', { glyph: g })}
              aria-pressed={glyph === g}
              onClick={() => onGlyphChange(g)}
              className={`flex size-9 items-center justify-center rounded-[10px] border-[1.5px] text-lg transition-colors ${glyph === g ? 'border-accent bg-accent-dim' : 'border-border bg-bg-1 hover:bg-bg-2'}`}
            >
              {g}
            </button>
          ))}
        </div>
      </div>
    </>
  )
}
