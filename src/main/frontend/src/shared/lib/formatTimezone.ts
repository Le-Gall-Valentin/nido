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
  const place = segments[segments.length - 1].replace(/_/g, ' ')
  if (segments.length < 3) {
    return place
  }
  return `${place} (${segments[1].replace(/_/g, ' ')})`
}
