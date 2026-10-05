import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ChevronLeft, Clock } from 'lucide-react'
import {
  NewPasswordForm,
  accountInvitationApi as defaultApi,
  passwordResetApi as defaultPasswordResetApi,
  useInvitationLinkCheck,
  usePasswordResetAvailability,
  type IAccountInvitationApi,
  type IPasswordResetApi,
} from '@/features/password-reset'
import { useAuth } from '@/features/auth'
import { ROUTES } from '@/shared/config'
import { Alert, Button, Spinner, AUTH_SUBMIT_CLASS, CTA_ELEVATED_STYLE } from '@/shared/ui'
import { AuthShell } from './AuthShell'
import { tokenInHash } from '../model/linkToken'
import { invitationAcceptedState } from '../model/invitationAccepted'

const TITLE_ID = 'welcome-title'

type Step = 'checking' | 'signed_in' | 'form' | 'invalid' | 'unavailable'

interface WelcomePageProps {
  api?: IAccountInvitationApi
  passwordResetApi?: IPasswordResetApi
}

/**
 * Where an invitation link lands: the invited account chooses its first password. The token is read from the
 * part after '#' — which the browser never sends to a server — then taken out of the address bar. The link is
 * checked at once, so nobody types a password twice to learn the invitation had expired. Works without mail:
 * the administrator then passed the link on by other means.
 */
export function WelcomePage({ api = defaultApi, passwordResetApi = defaultPasswordResetApi }: WelcomePageProps = {}) {
  const { t } = useTranslation('login')
  const location = useLocation()
  const navigate = useNavigate()
  const tokenInAddress = tokenInHash(location.hash)
  const [token, setToken] = useState(tokenInAddress)
  // Kept once the address bar is cleaned below — but a newer link opened in the same tab is the one meant now.
  if (tokenInAddress !== null && tokenInAddress !== token) setToken(tokenInAddress)
  const link = useInvitationLinkCheck(token, api)
  const invitedName = link.username
  // The link can also stop working while the password is typed: the save is refused with 410.
  const [refusedOnSaveFor, setRefusedOnSaveFor] = useState<string | null>(null)
  const linkStep: Step = token !== null && refusedOnSaveFor === token ? 'invalid' : link.state === 'valid' ? 'form' : link.state
  // Someone signed in on this device follows the link: as for a reset link, they sign out first.
  const user = useAuth((s) => s.user)
  const isRestoringSession = useAuth((s) => s.isInitializing)
  const logout = useAuth((s) => s.logout)
  const step: Step = isRestoringSession ? 'checking' : user ? 'signed_in' : linkStep
  // "Forgot password" sends an invited account a new invitation — offered only where it exists, with mail on.
  const passwordReset = usePasswordResetAvailability(passwordResetApi)

  useEffect(() => {
    if (location.hash) void navigate({ pathname: location.pathname, search: location.search }, { replace: true })
  }, [location.hash, location.pathname, location.search, navigate])

  return (
    <AuthShell>
      {(step === 'checking' || step === 'unavailable') && <h1 className="sr-only">{t('welcome.heading')}</h1>}
      {step === 'checking' && <Spinner label={t('welcome.checking')} />}

      {step === 'signed_in' && user && (
        <div>
          <h1 className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">{t('reset.signed_in.title')}</h1>
          <p className="mb-6 text-sm leading-relaxed text-fg-2">{t('reset.signed_in.body', { username: user.username })}</p>
          <Button
            type="button"
            onClick={() => { logout().catch(() => {}) }}
            className={AUTH_SUBMIT_CLASS}
            style={CTA_ELEVATED_STYLE}
          >
            {t('reset.signed_in.sign_out')}
          </Button>
          <p className="mt-6 text-center">
            <Link to={ROUTES.HOME} className="text-[13px] text-fg-2 hover:text-fg-0">{t('reset.signed_in.back')}</Link>
          </p>
        </div>
      )}

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
            onInvalid={() => setRefusedOnSaveFor(token)}
          />
        </>
      )}

      {step === 'invalid' && (
        <div role="alert">
          <div className="mb-5 grid size-11 place-items-center rounded-xl bg-status-red-dim text-status-red">
            <Clock className="size-5" aria-hidden="true" />
          </div>
          <h1 className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">{t('welcome.invalid.title')}</h1>
          <p className="mb-6 text-sm leading-relaxed text-fg-2">{t('welcome.invalid.body')}</p>
          {passwordReset === 'available' && (
            <Link
              to={ROUTES.FORGOT_PASSWORD}
              className="block w-full rounded-[11px] py-3.5 text-center text-[15px] font-semibold"
              style={CTA_ELEVATED_STYLE}
            >
              {t('welcome.invalid.forgot')}
            </Link>
          )}
          <p className="mt-6 text-center">
            <Link to={ROUTES.LOGIN} className="inline-flex items-center gap-1 text-[13px] text-fg-2 hover:text-fg-0">
              <ChevronLeft className="size-3.5" aria-hidden="true" />
              {t('forgot.back')}
            </Link>
          </p>
        </div>
      )}

      {step === 'unavailable' && (
        <div className="flex flex-col gap-4">
          <Alert variant="error">{t('welcome.unavailable')}</Alert>
          <Button type="button" onClick={link.retry} className="w-full rounded-[11px] py-3">{t('reset.retry')}</Button>
        </div>
      )}
    </AuthShell>
  )
}
