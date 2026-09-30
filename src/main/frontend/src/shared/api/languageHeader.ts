import type { AxiosInstance } from 'axios'
import i18n from 'i18next'
import { toLanguage } from '@/shared/lib/resolveLocale'

/**
 * Tells the server which language is on screen. The server needs it when it writes a mail for
 * someone whose account has no language yet — a reset link asked for from the login page, most of
 * all. Normalised to the two languages the app speaks, as the rest of the app does.
 *
 * Imported from its module rather than from `@/shared/lib`, whose index would bring the API client
 * back in through its own imports.
 */
export function attachLanguageHeader(
  instance: AxiosInstance,
  currentLanguage: () => string | undefined = () => i18n.language,
): void {
  instance.interceptors.request.use((config) => {
    config.headers.set('Accept-Language', toLanguage(currentLanguage()))
    return config
  })
}
