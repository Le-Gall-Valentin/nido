/**
 * The zones offered when setting a space's calendar.
 *
 * A short list rather than the full IANA database: a household is in one place, and scrolling six
 * hundred entries to find it is worse than not offering the choice. Free text would be worse still
 * — the API refuses a zone that names nowhere, and there is no reason to let somebody type one.
 *
 * The space's current zone is always included, whatever it is, so opening the form never silently
 * proposes moving a household that is somewhere this list forgot.
 *
 * In shared rather than beside the spaces page: three pages pick a zone now — creating a space,
 * editing one, and the personal space's own settings under account — and a page may not import
 * from another page.
 */
const COMMON_TIMEZONES = [
  'Europe/Paris',
  'Europe/London',
  'Europe/Brussels',
  'Europe/Madrid',
  'Europe/Lisbon',
  'Europe/Berlin',
  'Europe/Zurich',
  'America/Montreal',
  'America/Toronto',
  'America/New_York',
  'America/Chicago',
  'America/Denver',
  'America/Los_Angeles',
  'America/Cayenne',
  'Indian/Reunion',
  'Pacific/Noumea',
  'Pacific/Tahiti',
  'UTC',
]

export function timezoneChoices(current: string): string[] {
  return COMMON_TIMEZONES.includes(current) ? COMMON_TIMEZONES : [current, ...COMMON_TIMEZONES]
}
