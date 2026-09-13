export type Language = 'fr' | 'en'

/** The full locale each supported language formats with. */
const LOCALE_OF: Record<Language, string> = {
  fr: 'fr-FR',
  en: 'en-GB',
}

/**
 * Which of the two languages the application speaks a browser's tag means.
 *
 * <p>The tag is rarely bare. i18next-browser-languagedetector reports what the browser says, which on
 * a French machine is {@code "fr-FR"} — so comparing it to {@code 'fr'} with {@code ===} is false, and
 * that is exactly what four formatters did. French users read English month names, English relative
 * times ("2 days ago") and amounts as {@code €1,234.56} instead of {@code 1 234,50 €}. The prefix is
 * what carries the language; the region only says which variety of it.
 */
export function toLanguage(tag: string | undefined): Language {
  return tag?.startsWith('fr') ? 'fr' : 'en'
}

/**
 * The locale to hand to {@code Intl} for a browser's tag.
 *
 * <p>Deliberately not the tag itself: {@code fr-CA} is French, and this application's French is
 * written for France — a Canadian tag must not silently switch date order or currency placement. One
 * locale per language, chosen here, so every formatter agrees.
 */
export function resolveLocale(tag: string | undefined): string {
  return LOCALE_OF[toLanguage(tag)]
}
