import { describe, it, expect } from 'vitest'
import widgetFr from './fr.json'
import widgetEn from './en.json'
// The two imports below reach into pages/tasks, which a widget may never do at runtime.
// They are test fixtures: this file exists precisely to compare the two halves of a namespace
// that was split between a page and a widget, so it has to be able to read both. Nothing here
// ships — the assertion is the point, and a widget file importing page JSON in a *.test.ts is
// not the coupling the boundary rule is guarding against.
/* eslint-disable boundaries/dependencies */
import pageFr from '@/pages/finance/locales/fr.json'
import pageEn from '@/pages/finance/locales/en.json'
/* eslint-enable boundaries/dependencies */

// Three groups are deliberately duplicated between the page bundle and this one so each module
// is self-sufficient: the calendar mounts this widget without ever loading pages/tasks, and the
// tasks page renders priorities and columns without this widget. Duplication is only safe while
// the two copies agree — i18next deep-merges them and whichever registers last silently wins.
const DUPLICATED = ['delete_confirm', 'form', 'recurring_series', 'type'] as const

describe('finance namespace duplication', () => {
  it.each(DUPLICATED)('agrees on "%s" in French', (group) => {
    expect(widgetFr[group]).toEqual(pageFr[group as keyof typeof pageFr])
  })

  it.each(DUPLICATED)('agrees on "%s" in English', (group) => {
    expect(widgetEn[group]).toEqual(pageEn[group as keyof typeof pageEn])
  })
})
