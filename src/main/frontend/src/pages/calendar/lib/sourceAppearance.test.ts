import { describe, it, expect } from 'vitest'
import type { CalendarOccurrence } from '@/entities/calendar'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { DOT_CLASS, EVENT_COLORS, SOURCE_TOKEN, TINT_CLASS, tokenFor } from './sourceAppearance'

function occurrence(overrides: Partial<CalendarOccurrence>): CalendarOccurrence {
  return {
    source: 'EVENT', sourceId: 'x', seriesId: null, originalDate: null, materialized: true,
    title: 'x', description: null, location: null, allDay: true, startDate: '2026-01-01', startTime: null,
    endDate: '2026-01-01', endTime: null, color: null, participantIds: [], ...overrides,
  }
}

// Read from disk: vitest hands CSS imports over empty, `?raw` included.
const theme = readFileSync(resolve(process.cwd(), 'src/index.css'), 'utf-8')

const OTHER_SOURCES = [SOURCE_TOKEN.TASK, SOURCE_TOKEN.FINANCE, SOURCE_TOKEN.MEAL, SOURCE_TOKEN.SAVINGS] as string[]

describe('source colours', () => {
  it('gives every source a colour of its own', () => {
    expect(new Set(Object.values(SOURCE_TOKEN)).size).toBe(5)
  })

  it('paints an event with no colour of its own in violet, apart from the green of meals', () => {
    // Both used to be green — and the very same green in the dark theme.
    expect(tokenFor(occurrence({ source: 'EVENT' }))).toBe('event-violet')
    expect(tokenFor(occurrence({ source: 'MEAL' }))).toBe('status-green')
    expect(tokenFor(occurrence({ source: 'TASK' }))).toBe('status-blue')
  })
})

describe('event colours', () => {
  it('offers six colours, none of them a colour another source wears', () => {
    expect(EVENT_COLORS).toEqual(
      ['event-violet', 'event-magenta', 'event-cyan', 'event-graphite', 'event-indigo', 'event-brique'])
    for (const color of EVENT_COLORS) expect(OTHER_SOURCES).not.toContain(color)
  })

  it('honours an event colour an event was given', () => {
    expect(tokenFor(occurrence({ color: 'event-cyan' }))).toBe('event-cyan')
  })

  it('ignores a source colour stored on an event, so that no event passes for a meal', () => {
    expect(tokenFor(occurrence({ color: 'status-green' }))).toBe('event-violet')
  })

  it('ignores an override that is not one of the theme tokens', () => {
    // Stored text; a stale or hand-written value must not produce an unstyled element.
    expect(tokenFor(occurrence({ color: '#ff00ff' }))).toBe('event-violet')
    expect(tokenFor(occurrence({ source: 'SAVINGS', color: '#4a7fa0' }))).toBe('status-red')
  })

  it('has a class written out in full for every colour it can paint', () => {
    // Tailwind keeps only the classes it can read in the source; an interpolated one renders colourless.
    for (const token of [...EVENT_COLORS, ...Object.values(SOURCE_TOKEN)]) {
      expect(DOT_CLASS[token]).toBe(`bg-${token}`)
      expect(TINT_CLASS[token]).toBe(`bg-${token}-dim text-${token}`)
    }
  })

  it('defines every event colour for the light theme and for both ways of asking for the dark one', () => {
    for (const color of EVENT_COLORS) {
      expect(theme.match(new RegExp(`--color-${color}:`, 'g'))).toHaveLength(3)
      expect(theme.match(new RegExp(`--color-${color}-dim:`, 'g'))).toHaveLength(3)
    }
  })
})
