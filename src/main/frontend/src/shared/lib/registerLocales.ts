import i18next from 'i18next'

type LocaleResources = Record<string, unknown>

/**
 * Self-registers translation resources for a given namespace.
 * Call this as a side-effect in each feature/page locales/index.ts.
 * Works before and after i18next.init() — resources are stored immediately.
 * In dev (HMR), overwrite is enabled so edited JSON files take effect immediately.
 */
export function registerLocales(
  namespace: string,
  translations: Record<string, LocaleResources>
): void {
  if (typeof i18next?.addResourceBundle !== 'function') return
  const overwrite = import.meta.env.DEV
  for (const [lang, resources] of Object.entries(translations)) {
    i18next.addResourceBundle(lang, namespace, resources, true, overwrite)
  }
}