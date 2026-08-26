import { createInstance } from 'i18next'
import { describe, it, expect } from 'vitest'
import spacesEn from './en.json'
import spacesFr from './fr.json'
import en from '@/features/space-switcher/locales/en.json'
import fr from '@/features/space-switcher/locales/fr.json'

/**
 * The context role labels are declared once, in the switcher slice's `space`
 * namespace, and read from two places: that slice directly, and this page
 * through the cross-namespace `space:role.X` form. A wrong
 * prefix there fails silently — i18next renders the raw key to the user —
 * and every component test mocks `t` to the identity, so nothing else in the
 * suite would notice. This asserts both forms against the real resources.
 */
const ROLES = ['OWNER', 'ADMIN', 'MEMBER', 'VIEWER'] as const

async function makeI18n(language: 'en' | 'fr') {
  const instance = createInstance()
  await instance.init({
    lng: language,
    fallbackLng: false,
    ns: ['space', 'spaces'],
    defaultNS: 'space',
    resources: {
      en: { space: en, spaces: spacesEn },
      fr: { space: fr, spaces: spacesFr },
    },
    interpolation: { escapeValue: false },
    initImmediate: false,
  })
  return instance
}

describe('context role labels', () => {
  for (const language of ['en', 'fr'] as const) {
    it(`resolves every role from its own namespace in ${language}`, async () => {
      const i18n = await makeI18n(language)
      const t = i18n.getFixedT(null, 'space')

      for (const role of ROLES) {
        expect(t(`role.${role}`), `role.${role} (${language})`).not.toBe(`role.${role}`)
      }
    })

    it(`resolves every role from the pages namespace in ${language}`, async () => {
      const i18n = await makeI18n(language)
      const t = i18n.getFixedT(null, 'spaces')

      for (const role of ROLES) {
        const key = `space:role.${role}`
        expect(t(key), `${key} (${language})`).not.toBe(key)
      }
    })
  }

  it('declares the role labels in exactly one place', async () => {
    // A second table is how the two vocabularies diverged before: the switcher
    // said "Administrateur" while the pill next to it said "Admin".
    const i18n = await makeI18n('fr')
    const t = i18n.getFixedT(null, 'spaces')

    expect(t('role.ADMIN')).toBe('role.ADMIN')
  })
})
