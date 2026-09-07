import type { RecurrenceInterval } from '@/entities/tasks'

/**
 * Mirrors the backend's validation (RecurrenceScheduler.nextDueDate applied to
 * both the lead time and the main interval from the same anchor, then
 * compared) — a fast client-side check, not a replacement for it. The server
 * remains authoritative and re-validates on submit.
 */
function addInterval(anchorDateIso: string, intervalType: RecurrenceInterval, steps: number): string {
  const anchor = new Date(`${anchorDateIso}T00:00:00Z`)
  const anchorDay = anchor.getUTCDate()
  switch (intervalType) {
    case 'DAILY': {
      const d = new Date(anchor)
      d.setUTCDate(d.getUTCDate() + steps)
      return d.toISOString().slice(0, 10)
    }
    case 'WEEKLY': {
      const d = new Date(anchor)
      d.setUTCDate(d.getUTCDate() + steps * 7)
      return d.toISOString().slice(0, 10)
    }
    case 'MONTHLY': {
      const targetYear = anchor.getUTCFullYear()
      const targetMonth = anchor.getUTCMonth() + steps
      const daysInTargetMonth = new Date(Date.UTC(targetYear, targetMonth + 1, 0)).getUTCDate()
      const day = Math.min(anchorDay, daysInTargetMonth)
      return new Date(Date.UTC(targetYear, targetMonth, day)).toISOString().slice(0, 10)
    }
    case 'YEARLY': {
      const targetYear = anchor.getUTCFullYear() + steps
      const month = anchor.getUTCMonth()
      const daysInTargetMonth = new Date(Date.UTC(targetYear, month + 1, 0)).getUTCDate()
      const day = Math.min(anchorDay, daysInTargetMonth)
      return new Date(Date.UTC(targetYear, month, day)).toISOString().slice(0, 10)
    }
  }
}

export function leadTimeExceedsInterval(
  anchorDate: string, intervalType: RecurrenceInterval, intervalCount: number,
  leadIntervalType: RecurrenceInterval, leadIntervalCount: number,
): boolean {
  const mainDate = addInterval(anchorDate, intervalType, intervalCount)
  const leadDate = addInterval(anchorDate, leadIntervalType, leadIntervalCount)
  return leadDate > mainDate
}
