export function parseRetryAfter(headers?: Record<string, unknown>): number | null {
  const value = headers?.['retry-after']
  if (value == null || value === '') return null
  const parsed = parseInt(String(value), 10)
  return Number.isFinite(parsed) ? parsed : null
}