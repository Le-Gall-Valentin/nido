import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('i18next', () => ({
  default: { addResourceBundle: vi.fn() },
}))

import i18next from 'i18next'
import { registerLocales } from './registerLocales'

const mockAdd = vi.mocked(i18next.addResourceBundle)

beforeEach(() => mockAdd.mockClear())

describe('registerLocales', () => {
  it('calls addResourceBundle for both languages with the correct namespace', () => {
    const en = { key: 'hello' }
    const fr = { key: 'bonjour' }

    registerLocales('test-ns', { en, fr })

    expect(mockAdd).toHaveBeenCalledTimes(2)
    expect(mockAdd).toHaveBeenCalledWith('en', 'test-ns', en, true, import.meta.env.DEV)
    expect(mockAdd).toHaveBeenCalledWith('fr', 'test-ns', fr, true, import.meta.env.DEV)
  })

  it('supports arbitrary language keys (OCP — Record<string, LocaleResources>)', () => {
    const de = { key: 'hallo' }

    registerLocales('test-ns', { de })

    expect(mockAdd).toHaveBeenCalledWith('de', 'test-ns', de, true, import.meta.env.DEV)
  })

  it('is a no-op when addResourceBundle is not a function', () => {
    const saved = i18next.addResourceBundle
    // @ts-expect-error - simulating unavailable method
    i18next.addResourceBundle = undefined

    expect(() => registerLocales('ns', { en: {}, fr: {} })).not.toThrow()
    expect(mockAdd).not.toHaveBeenCalled()

    i18next.addResourceBundle = saved
  })

  it('is a no-op when i18next itself is nullish', () => {
    const saved = i18next.addResourceBundle
    // @ts-expect-error - simulating missing object
    i18next.addResourceBundle = null

    expect(() => registerLocales('ns', { en: {}, fr: {} })).not.toThrow()

    i18next.addResourceBundle = saved
  })
})