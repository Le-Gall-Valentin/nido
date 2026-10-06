import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  accountInvitationApi as defaultApi,
  useInvitationLinkCheck,
  type IAccountInvitationApi,
} from '@/features/account-invitation'
import { NewPasswordForm } from '@/features/password-reset'
import {
  capabilitiesApi as defaultCapabilitiesApi,
  usePasswordResetAvailability,
  type ICapabilitiesApi,
} from '@/entities/capabilities'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'
import { AuthShell } from './AuthShell'
import { LinkInvalid, LinkInvalidAction, LinkUnavailable } from './LinkProblem'
import { SignedInNotice } from './SignedInNotice'
import { invitationAcceptedState } from '../model/invitationAccepted'
import { useLinkStep } from '../model/useLinkStep'
import { useLinkToken } from '../model/useLinkToken'

const TITLE_ID = 'welcome-title'

interface WelcomePageProps {
  api?: IAccountInvitationApi
  capabilitiesApi?: ICapabilitiesApi
}

/**
 * Where an invitation link lands: the invited account chooses its first password. The link is checked at once,
 * so nobody types a password twice to learn the invitation had expired. Works without mail: the administrator
 * then passed the link on by other means.
 */
export function WelcomePage({ api = defaultApi, capabilitiesApi = defaultCapabilitiesApi }: WelcomePageProps = {}) {
  const { t } = useTranslation('login')
  const navigate = useNavigate()
  const { token, refusedOnSave, refuseOnSave } = useLinkToken()
  const link = useInvitationLinkCheck(token, api)
  const invitedName = link.username
  const { step, user, signOut } = useLinkStep(link.state, refusedOnSave)
  // "Forgot password" sends an invited account a new invitation — offered only where it exists, with mail on.
  const passwordReset = usePasswordResetAvailability(capabilitiesApi)

  return (
    <AuthShell>
      {(step === 'checking' || step === 'unavailable') && <h1 className="sr-only">{t('welcome.heading')}</h1>}
      {step === 'checking' && <Spinner label={t('welcome.checking')} />}

      {step === 'signed_in' && user && <SignedInNotice username={user.username} onSignOut={signOut} />}

      {step === 'form' && token && invitedName && (
        <>
          <div className="mb-8">
            <h1 id={TITLE_ID} className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">
              {t('welcome.title', { username: invitedName })}
            </h1>
            <p className="text-sm text-fg-2">{t('welcome.subtitle')}</p>
          </div>
          <NewPasswordForm
            onSave={(password) => api.acceptInvitation(token, password)}
            username={invitedName}
            labelId={TITLE_ID}
            onDone={() => void navigate(ROUTES.LOGIN, { replace: true, state: invitationAcceptedState(invitedName) })}
            onInvalid={refuseOnSave}
          />
        </>
      )}

      {step === 'invalid' && (
        <LinkInvalid title={t('welcome.invalid.title')} body={t('welcome.invalid.body')}>
          {passwordReset === 'available' && (
            <LinkInvalidAction to={ROUTES.FORGOT_PASSWORD}>{t('welcome.invalid.forgot')}</LinkInvalidAction>
          )}
        </LinkInvalid>
      )}

      {step === 'unavailable' && <LinkUnavailable message={t('welcome.unavailable')} onRetry={link.retry} />}
    </AuthShell>
  )
}
