/**
 * A full http(s) address with a host — what the server accepts as Nido's public address: nothing after
 * the host but a slash, since Nido answers at the root of its host only.
 */
export function isHttpAddress(value: string): boolean {
  try {
    const url = new URL(value.trim())
    return (url.protocol === 'http:' || url.protocol === 'https:') && url.hostname !== ''
      && url.pathname === '/' && url.search === '' && url.hash === '' && url.username === '' && url.password === ''
  } catch {
    return false
  }
}
