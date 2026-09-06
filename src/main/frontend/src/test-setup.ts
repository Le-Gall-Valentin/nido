import { afterEach, vi } from 'vitest'
import { cleanup } from '@testing-library/react'

// Component tests mock react-i18next's useTranslation directly and never run the real
// i18n.init() from app/i18n.ts, so the real i18next singleton is never initialized —
// formatAmount (and anything else reading i18next.language directly) needs a default here.
// Keeps every other real export (e.g. createInstance, used by roleLabels.test.ts) intact.
vi.mock('i18next', async (importOriginal) => {
  const actual = await importOriginal<typeof import('i18next')>()
  return { ...actual, default: { ...actual.default, language: 'fr' } }
})

afterEach(() => cleanup())