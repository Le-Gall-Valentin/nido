import type { CalendarOccurrence, CalendarSourceType } from '@/entities/calendar'

/**
 * One theme token per source. Token names, never hex values: each of these is defined twice in
 * index.css — once for light, once for dark — so storing the name is what keeps a colour correct
 * in both themes. A hex copied out of the light palette would be unreadable on a dark ground.
 */
export const SOURCE_TOKEN: Record<CalendarSourceType, string> = {
  EVENT: 'accent',
  TASK: 'status-blue',
  FINANCE: 'status-orange',
  MEAL: 'status-green',
  SAVINGS: 'status-red',
}

/** Fixed order, so a day's dots keep the same positions from one cell to the next. */
export const SOURCE_ORDER: CalendarSourceType[] = ['EVENT', 'TASK', 'FINANCE', 'MEAL', 'SAVINGS']

const ALLOWED_TOKENS = new Set(Object.values(SOURCE_TOKEN))

/**
 * The token an occurrence paints with: its own override when it has a valid one, its source's
 * otherwise. An unknown override is ignored rather than trusted — `color` is stored text, and a
 * stale or hand-written value must not produce an unstyled element.
 */
export function tokenFor(occurrence: CalendarOccurrence): string {
  if (occurrence.color && ALLOWED_TOKENS.has(occurrence.color)) {
    return occurrence.color
  }
  return SOURCE_TOKEN[occurrence.source]
}
