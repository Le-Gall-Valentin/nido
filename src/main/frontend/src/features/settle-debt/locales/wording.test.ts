import { createInstance } from 'i18next'
import { describe, it, expect } from 'vitest'
import en from './en.json'
import fr from './fr.json'

/** The component mocks `t`; this runs the sentence itself, with an amount already formatted as money. */
async function makeT(language: 'en' | 'fr') {
  const instance = createInstance()
  await instance.init({
    lng: language,
    fallbackLng: false,
    ns: ['settleDebt'],
    defaultNS: 'settleDebt',
    resources: { en: { settleDebt: en }, fr: { settleDebt: fr } },
    interpolation: { escapeValue: false },
    initImmediate: false,
  })
  return instance.getFixedT(null, 'settleDebt')
}

describe('settle debt wording', () => {
  it('names the currency once, in French', async () => {
    const t = await makeT('fr')
    expect(t('message', { from: 'Bob', to: 'Alice', amount: '42,50 €' })).toBe('Bob doit 42,50 € à Alice.')
  })

  it('names the currency once, in English', async () => {
    const t = await makeT('en')
    expect(t('message', { from: 'Bob', to: 'Alice', amount: '€42.50' })).toBe('Bob owes Alice €42.50.')
  })
})
