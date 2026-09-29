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

const MINUTE_MS = 60_000

/**
 * The space's current date and minute — what moves the now line. It turns when the clock turns:
 * each wait runs to the next whole minute, measured afresh every time, so a page opened at 10:15:40
 * reads 10:16 at 10:16:00, not at 10:16:40, and a timer that fires late never carries its delay over.
 * Whole minutes are the same instants in every zone, whose offsets are whole minutes too.
 */
export function useNow(zone?: string): ZonedNow {
  const [clock, setClock] = useState(() => ({ zone, now: nowInZone(new Date(), zone) }))

  useEffect(() => {
    let id: number
    const tick = () => {
      setClock({ zone, now: nowInZone(new Date(), zone) })
      id = window.setTimeout(tick, MINUTE_MS - (Date.now() % MINUTE_MS))
    }
    tick()
    return () => window.clearTimeout(id)
  }, [zone])

  // The zone often arrives after the first render, with the spaces list. The render it arrives in
  // reads the date there itself rather than wait for the effect above: that render's own effects —
  // a link opening the new-event form on "today" — otherwise got the previous zone's date, which
  // opened the form on the machine's day, not the household's.
  return clock.zone === zone ? clock.now : nowInZone(new Date(), zone)
}
