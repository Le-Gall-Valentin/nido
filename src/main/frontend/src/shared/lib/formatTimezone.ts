/**
 * An IANA identifier as somebody reads it on a card: `Europe/Paris` becomes `Paris`.
 *
 * The identifier is the vocabulary the API speaks and the browser reports, so it is what gets
 * stored — but a slash and a continent are database shape, not information a household needs. The
 * place is the part that answers "whose calendar is this".
 *
 * Where the identifier carries three segments the middle one is kept in brackets: several zones end
 * in the same city name, and a household in one should not read the other's.
 */
export function formatTimezone(timezone: string): string {
  const segments = timezone.split('/')
  // split never returns an empty array, and the three-segment branch below is the only reader of
  // the middle one — both facts the compiler cannot see through an index, so they are read as
  // values here rather than asserted away at the point of use.
  const [, region, city] = segments
  const last = city ?? region ?? timezone
  const place = last.replace(/_/g, ' ')
  if (segments.length < 3 || region === undefined) {
    return place
  }
  return `${place} (${region.replace(/_/g, ' ')})`
}
