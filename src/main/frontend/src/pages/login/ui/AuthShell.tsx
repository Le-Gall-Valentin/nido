import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { NidoMark } from '@/shared/ui'
import { LoginBrandPanel } from './LoginBrandPanel'

/** The frame of every signed-out page: the brand panel, and the form column with its footer. */
export function AuthShell({ children }: { children: ReactNode }) {
  const { t } = useTranslation('login')

  return (
    <div className="grid min-h-screen grid-cols-1 md:grid-cols-[1fr_1fr] lg:grid-cols-[1.1fr_1fr]">
      <LoginBrandPanel />

      <main className="relative flex flex-col justify-center bg-bg-0 px-8 py-12 sm:px-11">
        <div className="mx-auto w-full max-w-95">
          <div className="mb-8 flex items-center gap-2.5 md:hidden" data-testid="mobile-header">
            <div
              className="grid size-8 shrink-0 place-items-center rounded-[10px] text-white"
              style={{ background: 'linear-gradient(135deg, var(--brand-icon-from), var(--brand-icon-to))' }}
            >
              <NidoMark size={17} />
            </div>
            <span
              className="text-[17px] font-bold tracking-tight text-fg-0"
              style={{ fontFamily: 'var(--font-family-display)' }}
            >
              Nido
            </span>
          </div>

          {children}
        </div>

        <footer className="absolute bottom-6 left-0 right-0 text-center text-[11px] text-fg-3">
          {t('footer')}
        </footer>
      </main>
    </div>
  )
}
