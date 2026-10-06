import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  NewPasswordForm,
  passwordResetApi as defaultApi,
  useResetLinkCheck,
  type IPasswordResetApi,
} from '@/features/password-reset'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'
import { AuthShell } from './AuthShell'
import { LinkInvalid, LinkInvalidAction, LinkUnavailable } from './LinkProblem'
import { SignedInNotice } from './SignedInNotice'
import { PASSWORD_RESET_DONE_STATE } from '../model/passwordResetDone'
import { useLinkStep } from '../model/useLinkStep'
import { useLinkToken } from '../model/useLinkToken'

const TITLE_ID = 'reset-password-title'

/**
 * Where the reset mail's link lands. The link is checked at once, so nobody types a new password twice to learn
 * the link had already expired.
 */
export function ResetPasswordPage({ api = defaultApi }: { api?: IPasswordResetApi } = {}) {
  const { t } = useTranslation('login')
  const navigate = useNavigate()
  const { token, refusedOnSave, refuseOnSave } = useLinkToken()
  const link = useResetLinkCheck(token, api)
  const { step, user, signOut } = useLinkStep(link.state, refusedOnSave)

  return (
    <AuthShell>
      {/* The page keeps its name while there is no form to title: a screen reader lands on something. */}
      {(step === 'checking' || step === 'unavailable') && <h1 className="sr-only">{t('reset.title')}</h1>}
      {step === 'checking' && <Spinner label={t('reset.checking')} />}

      {step === 'signed_in' && user && <SignedInNotice username={user.username} onSignOut={signOut} />}

      {step === 'form' && token && (
        <>
          <div className="mb-8">
            <h1 id={TITLE_ID} className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">{t('reset.title')}</h1>
            <p className="text-sm text-fg-2">{t('reset.subtitle')}</p>
          </div>
          <NewPasswordForm
            onSave={(password) => api.confirmReset(token, password)}
            labelId={TITLE_ID}
            onDone={() => void navigate(ROUTES.LOGIN, { replace: true, state: PASSWORD_RESET_DONE_STATE })}
            onInvalid={refuseOnSave}
          />
        </>
      )}

      {step === 'invalid' && (
        <LinkInvalid title={t('reset.invalid.title')} body={t('reset.invalid.body')}>
          <LinkInvalidAction to={ROUTES.FORGOT_PASSWORD}>{t('reset.invalid.again')}</LinkInvalidAction>
        </LinkInvalid>
      )}

      {step === 'unavailable' && <LinkUnavailable message={t('reset.unavailable')} onRetry={link.retry} />}
    </AuthShell>
  )
}
