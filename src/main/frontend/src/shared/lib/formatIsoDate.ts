import i18next from 'i18next'
import { resolveLocale } from './resolveLocale'

/**
 * A calendar day (YYYY-MM-DD) written in full in the interface's language: "30 septembre 2026". Read at UTC, the zone
 * that has no daylight saving and no offset: a day has no time, and read at midnight in the browser's zone it would be
 * the day before west of Greenwich.
 */
export function formatIsoDate(isoDate: string): string {
  return new Intl.DateTimeFormat(resolveLocale(i18next.language), { dateStyle: 'long', timeZone: 'UTC' })
    .format(new Date(`${isoDate}T00:00:00Z`))
}
