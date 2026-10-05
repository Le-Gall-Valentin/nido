import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useLanguage, type Language } from '@/shared/lib'
import { NidoMark } from '@/shared/ui'

export const STEPS = ['code', 'admin', 'address', 'mail', 'key'] as const
export type SetupStep = (typeof STEPS)[number]

const LANGUAGES: Language[] = ['fr', 'en']

/** The frame of the setup: the brand, a language switch, how far along, and the step. */
export function SetupFrame({ step, children }: { step: SetupStep | 'done'; children: ReactNode }) {
  const { t } = useTranslation('setup')
  const { language, setLanguage } = useLanguage()
  const index = step === 'done' ? STEPS.length : STEPS.indexOf(step)

  return (
    <main className="min-h-screen bg-bg-0 px-4 py-10 sm:py-16">
      <div className="mx-auto w-full max-w-[520px]">
        <header className="mb-8 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div
              className="grid size-8 shrink-0 place-items-center rounded-[10px] text-white"
              style={{ background: 'linear-gradient(135deg, var(--brand-icon-from), var(--brand-icon-to))' }}
            >
              <NidoMark size={17} />
            </div>
            <span className="text-[17px] font-bold tracking-tight text-fg-0" style={{ fontFamily: 'var(--font-family-display)' }}>
              Nido
            </span>
          </div>
          <div role="group" aria-label={t('language.label')} className="flex gap-1">
            {LANGUAGES.map((code) => (
              <button
                key={code}
                type="button"
                aria-pressed={language === code}
                onClick={() => setLanguage(code)}
                className={`rounded-md px-2 py-1 text-xs font-semibold ${language === code ? 'bg-bg-2 text-fg-0' : 'text-fg-3 hover:text-fg-0'}`}
              >
                {code.toUpperCase()}
              </button>
            ))}
          </div>
        </header>
        {step !== 'done' && (
          <ol className="mb-6 flex gap-1.5" aria-label={t('progress', { current: index + 1, total: STEPS.length })}>
            {STEPS.map((name, i) => (
              <li key={name} className={`h-1 flex-1 rounded-full ${i <= index ? 'bg-accent' : 'bg-bg-3'}`} />
            ))}
          </ol>
        )}
        <section className="rounded-2xl border border-border bg-bg-1 p-6 sm:p-8">{children}</section>
      </div>
    </main>
  )
}
