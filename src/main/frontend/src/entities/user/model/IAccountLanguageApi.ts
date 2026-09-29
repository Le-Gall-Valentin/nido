import type { Language } from '@/shared/lib'

/** Records the language a person reads the app in, on their account. */
export interface IAccountLanguageApi {
  saveLanguage(language: Language): Promise<void>
}
