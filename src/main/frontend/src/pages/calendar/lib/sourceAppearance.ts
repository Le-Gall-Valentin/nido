import type { CalendarOccurrence, CalendarSourceType } from '@/entities/calendar'

/**
 * One theme token per source. Token names, never hex values: each of these is defined twice in
 * index.css — once for light, once for dark — so storing the name is what keeps a colour correct
 * in both themes. A hex copied out of the light palette would be unreadable on a dark ground.
 */
export const SOURCE_TOKEN = {
  EVENT: 'accent',
  TASK: 'status-blue',
  FINANCE: 'status-orange',
  MEAL: 'status-green',
  SAVINGS: 'status-red',
} as const satisfies Record<CalendarSourceType, string>

type SourceToken = (typeof SOURCE_TOKEN)[CalendarSourceType]

/** Fixed order, so a day's dots keep the same positions from one cell to the next. */
export const SOURCE_ORDER: CalendarSourceType[] = ['EVENT', 'TASK', 'FINANCE', 'MEAL', 'SAVINGS']

const ALLOWED_TOKENS = new Set<SourceToken>(Object.values(SOURCE_TOKEN))

/**
 * The token an occurrence paints with: its own override when it has a valid one, its source's
 * otherwise. An unknown override is ignored rather than trusted — `color` is stored text, and a
 * stale or hand-written value must not produce an unstyled element.
 */
export function tokenFor(occurrence: CalendarOccurrence): SourceToken {
  if (occurrence.color && ALLOWED_TOKENS.has(occurrence.color as SourceToken)) {
    return occurrence.color as SourceToken
  }
  return SOURCE_TOKEN[occurrence.source]
}

/**
 * Full class names, spelled out literally.
 *
 * Tailwind only keeps a class it can read in the source, so an interpolated `bg-${token}` would
 * be purged from the build and the dot would render colourless. Writing each one out once here is
 * what keeps the five source colours alive — and keeps that constraint in a single place rather
 * than scattered through every view.
 */
export const DOT_CLASS: Record<SourceToken, string> = {
  'accent': 'bg-accent',
  'status-blue': 'bg-status-blue',
  'status-orange': 'bg-status-orange',
  'status-green': 'bg-status-green',
  'status-red': 'bg-status-red',
}

export const TINT_CLASS: Record<SourceToken, string> = {
  'accent': 'bg-accent-dim text-accent',
  'status-blue': 'bg-status-blue-dim text-status-blue',
  'status-orange': 'bg-status-orange-dim text-status-orange',
  'status-green': 'bg-status-green-dim text-status-green',
  'status-red': 'bg-status-red-dim text-status-red',
}

/** The dot class for one occurrence, honouring its colour override. */
export function dotClassFor(occurrence: CalendarOccurrence): string {
  return DOT_CLASS[tokenFor(occurrence)]
}

/** The dot class for a whole source, used by the phone month view's per-source dots. */
export function dotClassForSource(source: CalendarSourceType): string {
  return DOT_CLASS[SOURCE_TOKEN[source]]
}

/** The tinted block class for one occurrence, used by the week and day grids. */
export function tintClassFor(occurrence: CalendarOccurrence): string {
  return TINT_CLASS[tokenFor(occurrence)]
}
