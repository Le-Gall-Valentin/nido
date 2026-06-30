const DIVISIONS: { amount: number; unit: Intl.RelativeTimeFormatUnit }[] = [
  { amount: 60, unit: 'second' },
  { amount: 60, unit: 'minute' },
  { amount: 24, unit: 'hour' },
  { amount: 7, unit: 'day' },
  { amount: 4.34524, unit: 'week' },
  { amount: 12, unit: 'month' },
  { amount: Number.POSITIVE_INFINITY, unit: 'year' },
]

// Intl.RelativeTimeFormat is comparatively expensive to construct; cache one
// instance per locale so a list render does not rebuild it on every row.
const RTF_CACHE = new Map<string, Intl.RelativeTimeFormat>()
function relativeTimeFormat(locale: string): Intl.RelativeTimeFormat {
  let rtf = RTF_CACHE.get(locale)
  if (!rtf) {
    rtf = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' })
    RTF_CACHE.set(locale, rtf)
  }
  return rtf
}

/** Localized "time ago" (or "in ...") for relative timestamps, via Intl.RelativeTimeFormat. */
export function formatRelativeTime(iso: string, lang: string, now: Date = new Date()): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '—'
  const locale = lang === 'fr' ? 'fr-FR' : 'en-GB'
  const rtf = relativeTimeFormat(locale)
  let duration = (date.getTime() - now.getTime()) / 1000
  for (const division of DIVISIONS) {
    if (Math.abs(duration) < division.amount) {
      return rtf.format(Math.round(duration), division.unit)
    }
    duration /= division.amount
  }
  return '—'
}