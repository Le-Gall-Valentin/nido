import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ChevronLeft, Mail } from 'lucide-react'
import { passwordResetApi as defaultApi, RequestResetForm, type IPasswordResetApi } from '@/features/password-reset'
import { ROUTES } from '@/shared/config'
import { AuthShell } from './AuthShell'

const TITLE_ID = 'forgot-password-title'

/**
 * "Forgot password". The confirmation never says whether an account matched — only that a link left
 * if one did — and shows no address, not even masked: either would tell a stranger the account exists.
 */
export function ForgotPasswordPage({ api = defaultApi }: { api?: IPasswordResetApi } = {}) {
  const { t } = useTranslation('login')
  const [sentFor, setSentFor] = useState<string | null>(null)

  return (
    <AuthShell>
      <Link to={ROUTES.LOGIN} className="mb-6 inline-flex items-center gap-1 text-[13px] text-fg-2 hover:text-fg-0">
        <ChevronLeft className="size-3.5" aria-hidden="true" />
        {t('forgot.back')}
      </Link>

      {sentFor === null ? (
        <>
          <div className="mb-8">
            <h1 id={TITLE_ID} className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">{t('forgot.title')}</h1>
            <p className="text-sm leading-relaxed text-fg-2">{t('forgot.subtitle')}</p>
          </div>
          <RequestResetForm api={api} labelId={TITLE_ID} onSent={setSentFor} />
        </>
      ) : (
        <div role="status">
          <div className="mb-5 grid size-11 place-items-center rounded-xl bg-accent-dim text-accent">
            <Mail className="size-5" aria-hidden="true" />
          </div>
          <h1 className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">{t('forgot.sent.title')}</h1>
          <p className="text-sm leading-relaxed text-fg-2">{t('forgot.sent.body', { identifier: sentFor })}</p>
          <p className="mt-3 text-sm leading-relaxed text-fg-2">{t('forgot.sent.hint')}</p>
        </div>
      )}
    </AuthShell>
  )
}
