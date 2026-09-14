/**
 * The zone this browser believes it is in, as an IANA identifier.
 *
 * Used to seed a new space so that it starts on its creator's calendar rather than the server's —
 * somebody creating a household from Toronto gets Toronto, and nobody has to configure anything in
 * the ordinary case. It is only ever a default: the value is shown in the form and can be changed
 * before submitting, and changed again afterwards from the space settings.
 *
 * Deliberately not used to *correct* an existing space. A member opening the app while travelling
 * must not move the household's calendar out from under everyone else.
 *
 * `formatter` is injectable so the fallback can be tested; old engines and locked-down environments
 * report an empty zone, and sending that would be refused by the API.
 */
export function browserTimezone(
  formatter: () => Intl.DateTimeFormat = () => new Intl.DateTimeFormat()
): string {
  return formatter().resolvedOptions().timeZone || 'Europe/Paris'
}
