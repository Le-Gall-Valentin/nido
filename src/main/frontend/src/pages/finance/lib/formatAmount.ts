import i18next from 'i18next'
import { resolveLocale } from '@/shared/lib'

// Nido is single-currency today — the currency is a fixed business fact, never derived
// from the UI language, so a future language never implies a different currency. Real
// multi-currency support would need a currency field on domain data (space/transaction),
// not a per-language branch here.
const CURRENCY = 'EUR'

/** Resolves the active UI language itself (see shared/lib/registerLocales.ts for the same pattern) so callers never have to thread a lang param through. */
export function formatAmount(amount: number): string {
  // resolveLocale rather than a comparison: i18next.language is "fr-FR" on a French browser, and
  // `=== 'fr'` was false there — every amount in the application read €1,234.56.
  return new Intl.NumberFormat(resolveLocale(i18next.language), { style: 'currency', currency: CURRENCY }).format(amount)
}
