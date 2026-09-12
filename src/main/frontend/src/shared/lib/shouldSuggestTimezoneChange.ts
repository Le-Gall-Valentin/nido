interface SuggestionInput {
  /** Undefined while the spaces are still loading. */
  space: { type: 'PERSONAL' | 'SHARED'; timezone: string } | undefined
  browser: string
  /** The zone a previous suggestion was turned down for, if any. */
  dismissedFor: string | null
}

/**
 * Whether to offer moving a space's calendar to the one the browser reports.
 *
 * Only ever for a **personal** space. A shared calendar belongs to every member: one of them opening
 * the app from a hotel must not be invited to change the day the rent falls due for everyone else.
 * That decision exists, in the space's own settings, where it reads as what it is — a decision about
 * the household. A personal space has one member, so the question is only ever about them.
 *
 * A refusal is remembered **per zone**, not once and for all. Two weeks abroad should ask once
 * rather than every morning; moving country for good has to be asked about, even by somebody who
 * said no to a holiday last year.
 *
 * Pure on purpose — where the refusal is stored is the caller's problem, and this is the part worth
 * being sure about.
 */
export function shouldSuggestTimezoneChange({ space, browser, dismissedFor }: SuggestionInput): boolean {
  if (!space || space.type !== 'PERSONAL') {
    return false
  }
  return browser !== space.timezone && browser !== dismissedFor
}
