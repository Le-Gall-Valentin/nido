import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { LoginForm } from '@/features/auth'
import { TotpVerifyStep, TotpEnrollProposal, TotpSetupFlow, totpApi as defaultTotpApi } from '@/features/totp'
import type { ITotpVerifyApi, ITotpEnrollApi } from '@/features/totp'
import {
  passwordResetApi as defaultPasswordResetApi,
  usePasswordResetAvailability,
  type IPasswordResetApi,
} from '@/features/password-reset'
import { ROUTES } from '@/shared/config'
import { Alert } from '@/shared/ui'
import { AuthShell } from './AuthShell'
import { isPasswordResetDone } from '../model/passwordResetDone'
import { acceptedInvitationIdentifier } from '../model/invitationAccepted'
import { useLoginFlow } from './useLoginFlow'

type TotpApi = ITotpVerifyApi & ITotpEnrollApi

const TITLE_ID = 'login-title'

interface LoginPageProps {
  totpApi?: TotpApi
  passwordResetApi?: IPasswordResetApi
}

export function LoginPage({ totpApi = defaultTotpApi, passwordResetApi = defaultPasswordResetApi }: LoginPageProps = {}) {
  const { t } = useTranslation('login')
  const location = useLocation()
  const navigate = useNavigate()
  const [resetDone] = useState(() => isPasswordResetDone(location.state))
  const [welcomed] = useState(() => acceptedInvitationIdentifier(location.state))
  const passwordReset = usePasswordResetAvailability(passwordResetApi)
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

  // History state survives a reload. Once the banner has read it, the reason leaves the entry, so a
  // reload of the login page does not announce a password change, or a welcome, a second time.
  useEffect(() => {
    if (resetDone || welcomed !== null) void navigate(location.pathname, { replace: true, state: null })
  }, [resetDone, welcomed, navigate, location.pathname])

  return (
    <AuthShell>
      {step === 'credentials' && (
        <>
          {resetDone && <Alert variant="success" className="mb-6">{t('reset.done')}</Alert>}
          {welcomed !== null && <Alert variant="success" className="mb-6">{t('welcome.done')}</Alert>}
          <div className="mb-8">
            <h1 id={TITLE_ID} className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">
              {t('form.title')}
            </h1>
            <p className="text-sm text-fg-2">{t('form.subtitle')}</p>
          </div>
          <LoginForm labelId={TITLE_ID} onLoginOutcome={handleLoginOutcome} initialIdentifier={welcomed ?? undefined} />
          {passwordReset === 'available' && (
            <p className="mt-4 text-center">
              <Link to={ROUTES.FORGOT_PASSWORD} className="text-[13px] font-semibold text-accent hover:underline">
                {t('forgot.link')}
              </Link>
            </p>
          )}
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
    </AuthShell>
  )
}
