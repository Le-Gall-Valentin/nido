import { describe, expect, it } from 'vitest'

/**
 * The one test that reads the translations as data rather than through a mock.
 *
 * Every component test replaces `useTranslation` with a stub that returns the key it is given, which
 * is what keeps those tests about behaviour instead of wording. The cost is that the whole suite is
 * blind to the translations themselves: a key that exists in one language and not the other, or a key
 * that a bad edit moved out of its block, changes nothing any test can see and everything the user
 * sees. That happened while splitting a locale file — valid JSON, green suite, `profile.error.conflict`
 * silently renamed to `profile.conflict`.
 *
 * So this compares the two languages' key trees, per namespace, and says which keys are missing where.
 */
// `default` alone: an eagerly globbed JSON module also exposes each top-level key as a named export,
// which would count every key twice and double every message this test prints.
const modules = import.meta.glob<{ default: Record<string, unknown> }>(
  '../../**/locales/{en,fr}.json', { eager: true })

function leafKeys(value: unknown, prefix = ''): string[] {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) return [prefix]
  return Object.entries(value as Record<string, unknown>)
    .flatMap(([key, child]) => leafKeys(child, prefix ? `${prefix}.${key}` : key))
}

/** "../../pages/tasks/locales/fr.json" → "pages/tasks" */
function namespaceOf(path: string): string {
  return path.replace(/^(\.\.\/)+/, '').replace(/\/locales\/(en|fr)\.json$/, '')
}

const namespaces = [...new Set(Object.keys(modules).map(namespaceOf))].sort()

describe('translations', () => {
  it('covers every namespace in the application', () => {
    // Guards the glob itself: a pattern that silently matches nothing would make every test below
    // pass without reading a single file.
    expect(namespaces.length).toBeGreaterThanOrEqual(14)
  })

  it.each(namespaces)('%s says the same things in French and in English', (namespace) => {
    const read = (lang: string) => {
      const entry = Object.entries(modules).find(([p]) => namespaceOf(p) === namespace && p.endsWith(`${lang}.json`))
      expect(entry, `${namespace} has no ${lang}.json`).toBeDefined()
      return leafKeys((entry as [string, { default: Record<string, unknown> }])[1].default).sort()
    }
    const fr = read('fr')
    const en = read('en')

    expect(fr.filter((k) => !en.includes(k)), `missing from ${namespace}/en.json`).toEqual([])
    expect(en.filter((k) => !fr.includes(k)), `missing from ${namespace}/fr.json`).toEqual([])
  })
})
