import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { LoginForm } from '@/features/auth'
import { MethodChoiceStep, CodeStep, EnrollProposal, AppSetupFlow, MailSetupStep, twoFactorApi as defaultTwoFactorApi } from '@/features/two-factor'
import type { ITwoFactorChallengeApi, ITwoFactorMethodsApi } from '@/features/two-factor'
import { capabilitiesApi as defaultCapabilitiesApi, useMailAvailability, usePasswordResetAvailability, type ICapabilitiesApi } from '@/entities/capabilities'
import { ROUTES } from '@/shared/config'
import { Alert } from '@/shared/ui'
import { AuthShell } from './AuthShell'
import { isPasswordResetDone } from '../model/passwordResetDone'
import { acceptedInvitationIdentifier } from '../model/invitationAccepted'
import { useLoginFlow } from './useLoginFlow'

const TITLE_ID = 'login-title'

interface LoginPageProps {
  twoFactorApi?: ITwoFactorChallengeApi & ITwoFactorMethodsApi
  capabilitiesApi?: ICapabilitiesApi
}

export function LoginPage({ twoFactorApi = defaultTwoFactorApi, capabilitiesApi = defaultCapabilitiesApi }: LoginPageProps = {}) {
  const { t } = useTranslation('login')
  const location = useLocation()
  const navigate = useNavigate()
  const [resetDone] = useState(() => isPasswordResetDone(location.state))
  const [welcomed] = useState(() => acceptedInvitationIdentifier(location.state))
  const passwordReset = usePasswordResetAvailability(capabilitiesApi)
  const mail = useMailAvailability(capabilitiesApi)
  const {
    step,
    challenge,
    pendingUser,
    handleLoginOutcome,
    handleChoice,
    handleChooseAnother,
    handleVerified,
    handleBack,
    handleAppChosen,
    handleMailStarted,
    handleBackToProposal,
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
      {step.name === 'credentials' && (
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

      {step.name === 'choose' && challenge && (
        <MethodChoiceStep
          username={challenge.username}
          maskedEmail={challenge.maskedEmail}
          api={twoFactorApi}
          onChoose={handleChoice}
          onBack={handleBack}
        />
      )}

      {step.name === 'code' && challenge && (
        <CodeStep
          username={challenge.username}
          method={step.method}
          maskedEmail={challenge.maskedEmail}
          resendAfterSeconds={step.resendAfterSeconds}
          mailLimitSeconds={step.mailLimitSeconds}
          api={twoFactorApi}
          onVerified={handleVerified}
          onBack={handleBack}
          onChooseAnother={challenge.methods.length > 1 ? handleChooseAnother : undefined}
        />
      )}

      {step.name === 'propose' && pendingUser && (
        <EnrollProposal
          username={pendingUser.username}
          email={pendingUser.email}
          mailAvailable={mail === 'available'}
          api={twoFactorApi}
          onAppChosen={handleAppChosen}
          onMailStarted={handleMailStarted}
          onSkip={handleSkip}
        />
      )}

      {step.name === 'setup_app' && (
        <AppSetupFlow api={twoFactorApi} onSuccess={() => handleSetupSuccess('APP')} onDismiss={handleSetupDismiss} />
      )}

      {step.name === 'setup_mail' && (
        <MailSetupStep
          variant="page"
          sentTo={step.sentTo}
          resendAfterSeconds={step.resendAfterSeconds}
          api={twoFactorApi}
          onSuccess={() => handleSetupSuccess('MAIL')}
          onBack={handleBackToProposal}
          onDismiss={handleSetupDismiss}
        />
      )}
    </AuthShell>
  )
}
