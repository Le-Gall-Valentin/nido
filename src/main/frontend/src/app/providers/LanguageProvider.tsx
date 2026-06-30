import { useEffect, useState, type ReactNode } from 'react'
import { LanguageContext, type Language } from '@/shared/lib/language'
import i18n from '../i18n'
export { useLanguage, type Language } from '@/shared/lib/language'

function toLanguage(lng: string): Language {
  return lng.startsWith('fr') ? 'fr' : 'en'
}

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(() =>
    toLanguage(i18n.language)
  )

  useEffect(() => {
    const handler = (lng: string) => setLanguageState(toLanguage(lng))
    i18n.on('languageChanged', handler)
    return () => { i18n.off('languageChanged', handler) }
  }, [])

  function setLanguage(lang: Language): void {
    void i18n.changeLanguage(lang)
  }

  return (
    <LanguageContext.Provider value={{ language, setLanguage }}>
      {children}
    </LanguageContext.Provider>
  )
}