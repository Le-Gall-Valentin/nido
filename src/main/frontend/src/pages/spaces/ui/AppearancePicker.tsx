import { useTranslation } from 'react-i18next'
import { Check } from 'lucide-react'
import { SPACE_ACCENTS, SPACE_GLYPHS } from '@/entities/space'

/** Shared by CreateSpaceModal and EditSpaceModal so the limit is declared once. */
export const NAME_MAX = 80
export const DESCRIPTION_MAX = 280

interface AppearancePickerProps {
  /** Translation namespace under which `accent`, `accent_option`, `glyph` and `glyph_option` live ('create' or 'edit'). */
  prefix: string
  accent: string
  onAccentChange: (accent: string) => void
  glyph: string
  onGlyphChange: (glyph: string) => void
  disabled?: boolean
}

/**
 * Accent and glyph pickers, identical between creating and editing a space.
 * Both options lists come straight from SPACE_ACCENTS / SPACE_GLYPHS — the
 * validated palette from @/entities/space — so a value handed back through
 * onAccentChange/onGlyphChange is always palette-safe before it ever reaches
 * a style attribute.
 */
export function AppearancePicker({ prefix, accent, onAccentChange, glyph, onGlyphChange, disabled }: AppearancePickerProps) {
  const { t } = useTranslation('spaces')

  return (
    <>
      <div className="mb-4">
        <span className="mb-2 block text-[13px] font-semibold text-fg-1">{t(`${prefix}.accent`)}</span>
        <div className="flex flex-wrap gap-2">
          {SPACE_ACCENTS.map((a) => (
            <button
              key={a}
              type="button"
              aria-label={t(`${prefix}.accent_option`, { color: a })}
              aria-pressed={accent === a}
              disabled={disabled}
              onClick={() => onAccentChange(a)}
              className="flex size-9 items-center justify-center rounded-full disabled:opacity-50"
              style={{ background: a }}
            >
              {accent === a && <Check className="size-4 text-white" />}
            </button>
          ))}
        </div>
      </div>

      <div className="mb-1">
        <span className="mb-2 block text-[13px] font-semibold text-fg-1">{t(`${prefix}.glyph`)}</span>
        <div className="flex flex-wrap gap-2">
          {SPACE_GLYPHS.map((g) => (
            <button
              key={g}
              type="button"
              aria-label={t(`${prefix}.glyph_option`, { glyph: g })}
              aria-pressed={glyph === g}
              disabled={disabled}
              onClick={() => onGlyphChange(g)}
              className={`flex size-9 items-center justify-center rounded-[10px] border-[1.5px] text-lg transition-colors disabled:opacity-50 ${glyph === g ? 'border-accent bg-accent-dim' : 'border-border bg-bg-1 hover:bg-bg-2'}`}
            >
              {g}
            </button>
          ))}
        </div>
      </div>
    </>
  )
}
