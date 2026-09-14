import { resolveLocale } from '@/shared/lib'

/** Formats a user's creation date for the supported UI languages. */
export function formatUserDate(createdAt: string, lang: string): string {
  const date = new Date(createdAt)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString(resolveLocale(lang), {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}
