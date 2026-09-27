/**
 * The colours an event may be given — its own palette, none of them a colour another calendar source
 * wears: an event painted in a source's colour would pass for a meal, a task or a debit. The first is
 * the colour of an event given none.
 *
 * Token names, never hex values: each is defined in index.css for the light theme and for both ways of
 * asking for the dark one, so storing the name is what keeps a colour right in both. The calendar and
 * the dashboard both paint events from here; neither keeps a copy.
 */
export const EVENT_COLORS = [
  'event-violet', 'event-magenta', 'event-cyan', 'event-graphite', 'event-indigo', 'event-brique',
] as const

export type EventColor = (typeof EVENT_COLORS)[number]

export const DEFAULT_EVENT_COLOR: EventColor = EVENT_COLORS[0]

/**
 * Full class names, spelled out literally: Tailwind only keeps a class it can read in the source, so an
 * interpolated `bg-${token}` would be purged from the build and the event would render colourless.
 */
export const EVENT_DOT_CLASS: Record<EventColor, string> = {
  'event-violet': 'bg-event-violet',
  'event-magenta': 'bg-event-magenta',
  'event-cyan': 'bg-event-cyan',
  'event-graphite': 'bg-event-graphite',
  'event-indigo': 'bg-event-indigo',
  'event-brique': 'bg-event-brique',
}

export const EVENT_TINT_CLASS: Record<EventColor, string> = {
  'event-violet': 'bg-event-violet-dim text-event-violet',
  'event-magenta': 'bg-event-magenta-dim text-event-magenta',
  'event-cyan': 'bg-event-cyan-dim text-event-cyan',
  'event-graphite': 'bg-event-graphite-dim text-event-graphite',
  'event-indigo': 'bg-event-indigo-dim text-event-indigo',
  'event-brique': 'bg-event-brique-dim text-event-brique',
}

const KNOWN = new Set<string>(EVENT_COLORS)

/** Whether stored text is one of the event colours — it is never trusted to be. */
export function isEventColor(color: string | null | undefined): color is EventColor {
  return color != null && KNOWN.has(color)
}

/** The dot or bar of an event: its own colour when it has a valid one, the default otherwise. */
export function eventDotClass(color: string | null | undefined): string {
  return EVENT_DOT_CLASS[isEventColor(color) ? color : DEFAULT_EVENT_COLOR]
}

/** The tinted block or banner of an event: its own colour when it has a valid one, the default otherwise. */
export function eventTintClass(color: string | null | undefined): string {
  return EVENT_TINT_CLASS[isEventColor(color) ? color : DEFAULT_EVENT_COLOR]
}
