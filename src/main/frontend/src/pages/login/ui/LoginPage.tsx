import { useTranslation } from 'react-i18next'
import { LoginForm } from '@/features/auth'
import { TotpVerifyStep, TotpEnrollProposal, TotpSetupFlow, totpApi as defaultTotpApi } from '@/features/totp'
import type { ITotpVerifyApi, ITotpEnrollApi } from '@/features/totp'
import { LoginBrandPanel } from './LoginBrandPanel'
import { useLoginFlow } from './useLoginFlow'

type TotpApi = ITotpVerifyApi & ITotpEnrollApi

const TITLE_ID = 'login-title'

export function LoginPage({ totpApi = defaultTotpApi }: { totpApi?: TotpApi } = {}) {
  const { t } = useTranslation('login')
  const {
    step,
    pendingUser,
    pendingUsername,
    handleLoginOutcome,
    handleVerified,
    handleBack,
    handleActivate,
    handleSkip,
    handleSetupSuccess,
    handleSetupDismiss,
  } = useLoginFlow()

  return (
    <div className="grid min-h-screen grid-cols-1 md:grid-cols-[1fr_1fr] lg:grid-cols-[1.1fr_1fr]">
      <LoginBrandPanel />

      <main className="relative flex flex-col justify-center bg-bg-0 px-8 py-12 sm:px-11">
        <div className="mx-auto w-full max-w-95">
          <div className="mb-8 flex items-center gap-2.5 md:hidden" data-testid="mobile-header">
            <div
              className="grid size-8 shrink-0 place-items-center rounded-lg font-mono text-sm font-bold text-bg-0"
              style={{ background: 'linear-gradient(135deg, var(--brand-icon-from), var(--brand-icon-to))' }}
            >
              B
            </div>
            <span className="text-[15px] font-semibold tracking-tight text-fg-0">Nido</span>
          </div>

          {step === 'credentials' && (
            <>
              <div className="mb-8">
                <h1 id={TITLE_ID} className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">
                  {t('form.title')}
                </h1>
                <p className="text-sm text-fg-2">{t('form.subtitle')}</p>
              </div>
              <LoginForm labelId={TITLE_ID} onLoginOutcome={handleLoginOutcome} />
              <p className="mt-7 text-center text-[13px] leading-relaxed text-fg-2">
                <span className="font-medium text-fg-1">{t('help.no_account')}</span>{' '}
                {t('help.contact_admin')}
              </p>
            </>
          )}

          {step === 'totp' && (
            <TotpVerifyStep
              username={pendingUsername}
              api={totpApi}
              onVerified={handleVerified}
              onBack={handleBack}
            />
          )}

          {step === 'enroll' && pendingUser && (
            <TotpEnrollProposal
              username={pendingUser.username}
              onActivate={handleActivate}
              onSkip={handleSkip}
            />
          )}

          {step === 'setup' && (
            <TotpSetupFlow
              api={totpApi}
              onSuccess={handleSetupSuccess}
              onDismiss={handleSetupDismiss}
            />
          )}

        </div>

        <footer className="absolute bottom-6 left-0 right-0 text-center text-[11px] text-fg-3">
          {t('footer')}
        </footer>
      </main>
    </div>
  )
}