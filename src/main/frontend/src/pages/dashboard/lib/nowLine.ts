import type { AgendaEvent } from '@/entities/dashboard'
import { shortTime } from './dates'

/** Where the now line goes among the day's timed events (in start order): after every one that has started. */
export function nowLineIndex(timed: AgendaEvent[], now: string): number {
  return timed.filter((event) => event.startTime !== null && shortTime(event.startTime) <= now).length
}

/**
 * Whether an event is over, and therefore dimmed: its end — or its start, when it has no end — is not
 * after now. An event that goes on into a later day is never over today.
 */
export function isEventOver(event: AgendaEvent, today: string, now: string): boolean {
  if (event.endDate > today) return false
  const end = event.endTime ?? event.startTime
  return end !== null && shortTime(end) <= now
}
