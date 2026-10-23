import { useId, useRef, type KeyboardEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { EVENT_COLORS, EVENT_DOT_CLASS, type EventColor } from '../lib/eventPalette'

/**
 * The colours of events, one to pick: a radio group named by its visible label, one tab stop, the arrows to move from
 * a colour to the next. `describe` words a colour's button from its name ("Indigo for « Club »"); `deselectable` lets
 * the colour chosen be taken back by choosing it again, leaving none.
 */
export function ColorSwatches({ label, value, onChange, describe, deselectable = false }: {
  label: string
  value: EventColor | null
  onChange: (color: EventColor | null) => void
  describe?: (colorName: string) => string
  deselectable?: boolean
}) {
  const { t } = useTranslation('calendarColors')
  const labelId = useId()
  const buttons = useRef<(HTMLButtonElement | null)[]>([])
  // The tab stop: the colour chosen, or the first when there is none.
  const focusable = value === null ? 0 : EVENT_COLORS.indexOf(value)

  const move = (event: KeyboardEvent, index: number) => {
    const step = event.key === 'ArrowRight' || event.key === 'ArrowDown' ? 1
      : event.key === 'ArrowLeft' || event.key === 'ArrowUp' ? -1 : 0
    if (step === 0) return
    event.preventDefault()
    const next = (index + step + EVENT_COLORS.length) % EVENT_COLORS.length
    onChange(EVENT_COLORS[next] ?? null)
    buttons.current[next]?.focus()
  }

  return (
    <div>
      <span id={labelId} className="mb-1 block text-xs font-semibold text-fg-2">{label}</span>
      <div role="radiogroup" aria-labelledby={labelId} className="flex gap-1.5">
        {EVENT_COLORS.map((color, index) => {
          const name = t(`colors.${color}`)
          return (
            <button key={color} type="button" role="radio" aria-checked={value === color}
              ref={(button) => { buttons.current[index] = button }}
              tabIndex={index === focusable ? 0 : -1}
              aria-label={describe ? describe(name) : name}
              onClick={() => onChange(deselectable && value === color ? null : color)}
              onKeyDown={(event) => move(event, index)}
              className={`size-6 rounded-full border-2 ${value === color ? 'border-fg-1' : 'border-transparent'} ${EVENT_DOT_CLASS[color]}`} />
          )
        })}
      </div>
    </div>
  )
}
