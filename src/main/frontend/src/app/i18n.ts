import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    fallbackLng: 'en',
    supportedLngs: ['en', 'fr'],
    defaultNS: 'common',
    load: 'languageOnly',
    interpolation: { escapeValue: false },
    // No `resources` — each feature/page self-registers via registerLocales()
    // triggered as a side-effect when its index.ts is imported.
  })
  .then(() => {
    document.documentElement.lang = i18n.language
    const onLanguageChanged = (lng: string) => { document.documentElement.lang = lng }
    i18n.on('languageChanged', onLanguageChanged)
    if (import.meta.hot) {
      import.meta.hot.dispose(() => { i18n.off('languageChanged', onLanguageChanged) })
    }
  })
  .catch((err: unknown) => {
    if (import.meta.env.DEV) console.error('[i18n] init failed', err)
  })

export default i18n