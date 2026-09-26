/**
 * The calendar's event palette, as full class names so Tailwind keeps them (an interpolated
 * `bg-${token}` is purged from the build). The calendar page holds the same list in
 * pages/calendar/lib/sourceAppearance.ts; a page may not import another page, so the dashboard keeps
 * its own copy of the six tokens — if the palette grows, grow both.
 */
const EVENT_TOKENS = ['event-violet', 'event-magenta', 'event-cyan', 'event-graphite', 'event-indigo', 'event-brique'] as const
type EventToken = (typeof EVENT_TOKENS)[number]

const BAR_CLASS: Record<EventToken, string> = {
  'event-violet': 'bg-event-violet',
  'event-magenta': 'bg-event-magenta',
  'event-cyan': 'bg-event-cyan',
  'event-graphite': 'bg-event-graphite',
  'event-indigo': 'bg-event-indigo',
  'event-brique': 'bg-event-brique',
}

const TINT_CLASS: Record<EventToken, string> = {
  'event-violet': 'bg-event-violet-dim text-event-violet',
  'event-magenta': 'bg-event-magenta-dim text-event-magenta',
  'event-cyan': 'bg-event-cyan-dim text-event-cyan',
  'event-graphite': 'bg-event-graphite-dim text-event-graphite',
  'event-indigo': 'bg-event-indigo-dim text-event-indigo',
  'event-brique': 'bg-event-brique-dim text-event-brique',
}

function tokenOf(color: string | null): EventToken {
  return color !== null && (EVENT_TOKENS as readonly string[]).includes(color) ? (color as EventToken) : 'event-violet'
}

/** The thin bar beside a timed event. */
export function eventBarClass(color: string | null): string {
  return BAR_CLASS[tokenOf(color)]
}

/** The banner of an all-day event. */
export function eventTintClass(color: string | null): string {
  return TINT_CLASS[tokenOf(color)]
}
