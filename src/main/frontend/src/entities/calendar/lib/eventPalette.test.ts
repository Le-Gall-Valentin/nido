import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import {
  DEFAULT_EVENT_COLOR, EVENT_COLORS, EVENT_DOT_CLASS, EVENT_TINT_CLASS, eventDotClass, eventTintClass, isEventColor,
} from './eventPalette'

// Read from disk: vitest hands CSS imports over empty, `?raw` included.
const theme = readFileSync(resolve(process.cwd(), 'src/index.css'), 'utf-8')

describe('event palette', () => {
  it('offers six colours, the first for an event given none', () => {
    expect(EVENT_COLORS).toEqual(
      ['event-violet', 'event-magenta', 'event-cyan', 'event-graphite', 'event-indigo', 'event-brique'])
    expect(DEFAULT_EVENT_COLOR).toBe('event-violet')
  })

  it('has a class written out in full for every colour', () => {
    // Tailwind keeps only the classes it can read in the source; an interpolated one renders colourless.
    for (const color of EVENT_COLORS) {
      expect(EVENT_DOT_CLASS[color]).toBe(`bg-${color}`)
      expect(EVENT_TINT_CLASS[color]).toBe(`bg-${color}-dim text-${color}`)
    }
  })

  it('defines every colour for the light theme and for both ways of asking for the dark one', () => {
    for (const color of EVENT_COLORS) {
      expect(theme.match(new RegExp(`--color-${color}:`, 'g'))).toHaveLength(3)
      expect(theme.match(new RegExp(`--color-${color}-dim:`, 'g'))).toHaveLength(3)
    }
  })

  it('paints an event in its own colour', () => {
    expect(eventDotClass('event-indigo')).toBe('bg-event-indigo')
    expect(eventTintClass('event-cyan')).toBe('bg-event-cyan-dim text-event-cyan')
  })

  it('paints a colour it does not know — none, a hex, another source\'s — in the default one', () => {
    // Stored text: a stale or hand-written value must not leave an event unstyled, nor dress it as a meal.
    expect(eventDotClass(null)).toBe('bg-event-violet')
    expect(eventDotClass('#ff0000')).toBe('bg-event-violet')
    expect(eventTintClass('status-green')).toBe('bg-event-violet-dim text-event-violet')
  })

  it('tells an event colour from anything else', () => {
    expect(isEventColor('event-brique')).toBe(true)
    expect(isEventColor('status-blue')).toBe(false)
    expect(isEventColor(null)).toBe(false)
  })
})
