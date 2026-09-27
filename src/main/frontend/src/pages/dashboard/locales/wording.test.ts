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

/** Every sentence of a translation file, keyed by its dotted path. */
function sentences(tree: object, prefix = ''): [string, string][] {
  return Object.entries(tree).flatMap(([key, value]) => (typeof value === 'string'
    ? [[`${prefix}${key}`, value] as [string, string]]
    : sentences(value as object, `${prefix}${key}.`)))
}

describe('dashboard wording', () => {
  it('addresses the household formally in French, as the rest of the app does', () => {
    // A letter-aware boundary: JavaScript's \b does not know that "é" is a letter.
    const informal = /(^|[^\p{L}])(tu|te|toi|ton|ta|tes|t')(?=[^\p{L}]|$)/iu
    expect(sentences(fr).filter(([, text]) => informal.test(text))).toEqual([])
  })

  it('says who owes whom formally in French', async () => {
    const t = await makeT('fr')
    expect(t('finance.i_owe', { amount: '42,50 €', name: 'Camille' })).toBe('Vous devez 42,50 € à Camille')
    expect(t('finance.owes_me', { amount: '18,00 €', name: 'Paul' })).toBe('Paul vous doit 18,00 €')
    expect(t('hero.digest', { count: 2, name: 'Valentin' })).toBe('Bonjour Valentin, 2 choses demandent votre attention.')
  })

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
