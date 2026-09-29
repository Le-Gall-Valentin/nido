import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ChevronLeft, Clock } from 'lucide-react'
import {
  InvalidResetLinkError,
  NewPasswordForm,
  passwordResetApi as defaultApi,
  type IPasswordResetApi,
} from '@/features/password-reset'
import { ROUTES } from '@/shared/config'
import { Alert, Button, Spinner, CTA_BUTTON_SHADOW, CTA_BUTTON_STYLE } from '@/shared/ui'
import { AuthShell } from './AuthShell'
import { PASSWORD_RESET_DONE_STATE } from '../model/passwordResetDone'

const TITLE_ID = 'reset-password-title'

type Step = 'checking' | 'form' | 'invalid' | 'unavailable'

function tokenIn(hash: string): string | null {
  const token = new URLSearchParams(hash.replace(/^#/, '')).get('token')
  return token && token.trim() ? token : null
}

/**
 * Where the reset mail's link lands. The token is read from the part after '#' — which the browser
 * never sends to a server — then taken out of the address bar, so it stays in no history entry and on
 * no screen shared later. The link is checked at once, so nobody types a new password twice to learn
 * the link had already expired.
 */
export function ResetPasswordPage({ api = defaultApi }: { api?: IPasswordResetApi } = {}) {
  const { t } = useTranslation('login')
  const location = useLocation()
  const navigate = useNavigate()
  const [token] = useState(() => tokenIn(location.hash))
  const [step, setStep] = useState<Step>(token ? 'checking' : 'invalid')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    if (location.hash) void navigate({ pathname: location.pathname, search: location.search }, { replace: true })
  }, [location.hash, location.pathname, location.search, navigate])

  useEffect(() => {
    if (!token) return
    let current = true
    void api.checkToken(token).then(
      () => { if (current) setStep('form') },
      (error: unknown) => { if (current) setStep(error instanceof InvalidResetLinkError ? 'invalid' : 'unavailable') },
    )
    return () => { current = false }
  }, [api, token, attempt])

  function retry() {
    setStep('checking')
    setAttempt((n) => n + 1)
  }

  return (
    <AuthShell>
      {step === 'checking' && <Spinner label={t('reset.checking')} />}

      {step === 'form' && token && (
        <>
          <div className="mb-8">
            <h1 id={TITLE_ID} className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">{t('reset.title')}</h1>
            <p className="text-sm text-fg-2">{t('reset.subtitle')}</p>
          </div>
          <NewPasswordForm
            api={api}
            token={token}
            labelId={TITLE_ID}
            onDone={() => void navigate(ROUTES.LOGIN, { replace: true, state: PASSWORD_RESET_DONE_STATE })}
            onInvalid={() => setStep('invalid')}
          />
        </>
      )}

      {step === 'invalid' && (
        <div>
          <div className="mb-5 grid size-11 place-items-center rounded-xl bg-status-red-dim text-status-red">
            <Clock className="size-5" aria-hidden="true" />
          </div>
          <h1 className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">{t('reset.invalid.title')}</h1>
          <p className="mb-6 text-sm leading-relaxed text-fg-2">{t('reset.invalid.body')}</p>
          <Link
            to={ROUTES.FORGOT_PASSWORD}
            className="block w-full rounded-[11px] py-3.5 text-center text-[15px] font-semibold"
            style={{ ...CTA_BUTTON_STYLE, boxShadow: CTA_BUTTON_SHADOW }}
          >
            {t('reset.invalid.again')}
          </Link>
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
          <Alert variant="error">{t('reset.unavailable')}</Alert>
          <Button type="button" onClick={retry} className="w-full rounded-[11px] py-3">{t('reset.retry')}</Button>
        </div>
      )}
    </AuthShell>
  )
}
