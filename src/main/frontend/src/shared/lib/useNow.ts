import { useEffect, useState } from 'react'

export interface ZonedNow {
  /** `YYYY-MM-DD` on the space's calendar. */
  date: string
  /** `HH:mm`, 24-hour, on the space's clock. */
  time: string
}

/** Date and time of `now` on a given zone's calendar and clock (the viewer's own when `zone` is omitted). */
export function nowInZone(now: Date, zone?: string): ZonedNow {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
  }).formatToParts(now)
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find((p) => p.type === type)?.value ?? '00'
  return { date: `${part('year')}-${part('month')}-${part('day')}`, time: `${part('hour')}:${part('minute')}` }
}

/** The space's current date and minute, ticking every minute — what moves the now line. */
export function useNow(zone?: string, intervalMs = 60_000): ZonedNow {
  const [now, setNow] = useState(() => nowInZone(new Date(), zone))

  useEffect(() => {
    setNow(nowInZone(new Date(), zone))
    const id = window.setInterval(() => setNow(nowInZone(new Date(), zone)), intervalMs)
    return () => window.clearInterval(id)
  }, [zone, intervalMs])

  return now
}
