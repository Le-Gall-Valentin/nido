/** The token of a link that carries it after '#': `#token=abc` — whatever else a messaging app appended. */
export function tokenInHash(hash: string): string | null {
  const token = new URLSearchParams(hash.replace(/^#/, '')).get('token')
  return token && token.trim() ? token : null
}
