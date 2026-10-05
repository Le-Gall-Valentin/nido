/** A full http(s) address with a host — what the server accepts as Nido's public address. */
export function isHttpAddress(value: string): boolean {
  try {
    const url = new URL(value.trim())
    return (url.protocol === 'http:' || url.protocol === 'https:') && url.hostname !== ''
  } catch {
    return false
  }
}
