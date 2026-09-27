import { createInstance } from 'i18next'
import { describe, it, expect } from 'vitest'
import en from './en.json'
import fr from './fr.json'

/**
 * Component tests mock `t` to the identity, so they never see a plural resolved or a sentence read
 * aloud. These checks run the dashboard's own sentences through a real i18next instance.
 */
async function makeT(language: 'en' | 'fr') {
  const instance = createInstance()
  await instance.init({
    lng: language,
    fallbackLng: false,
    ns: ['dashboard'],
    defaultNS: 'dashboard',
    resources: { en: { dashboard: en }, fr: { dashboard: fr } },
    interpolation: { escapeValue: false },
    initImmediate: false,
  })
  return instance.getFixedT(null, 'dashboard')
}

describe('dashboard wording', () => {
  it('agrees "and N more" with its number in French', async () => {
    const t = await makeT('fr')
    expect(t('shopping.and_more', { count: 1 })).toBe('et 1 autre')
    expect(t('shopping.and_more', { count: 3 })).toBe('et 3 autres')
  })

  it('agrees "and N more" with its number in English', async () => {
    const t = await makeT('en')
    expect(t('shopping.and_more', { count: 1 })).toBe('and 1 more')
    expect(t('shopping.and_more', { count: 3 })).toBe('and 3 more')
  })
})
