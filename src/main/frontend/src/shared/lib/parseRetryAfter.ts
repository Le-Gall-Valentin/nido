export function parseRetryAfter(headers?: Record<string, unknown>): number | null {
  const value = headers?.['retry-after']
  // Anything else stringifies to something meaningless ("[object Object]"), which parseInt would
  // happily turn into NaN — and a header that is not a string or a number is not a delay.
  if (typeof value !== 'string' && typeof value !== 'number') return null
  const parsed = parseInt(String(value), 10)
  return Number.isFinite(parsed) ? parsed : null
}