import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ROUTES } from '@/shared/config'
import { Button, AUTH_SUBMIT_CLASS, CTA_ELEVATED_STYLE } from '@/shared/ui'

/** A one-time link followed by someone signed in on this device: they sign out first, or go back to the app. */
export function SignedInNotice({ username, onSignOut }: { username: string; onSignOut: () => void }) {
  const { t } = useTranslation('login')
  return (
    <div>
      <h1 className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">{t('reset.signed_in.title')}</h1>
      <p className="mb-6 text-sm leading-relaxed text-fg-2">{t('reset.signed_in.body', { username })}</p>
      <Button type="button" onClick={onSignOut} className={AUTH_SUBMIT_CLASS} style={CTA_ELEVATED_STYLE}>
        {t('reset.signed_in.sign_out')}
      </Button>
      <p className="mt-6 text-center">
        <Link to={ROUTES.HOME} className="text-[13px] text-fg-2 hover:text-fg-0">{t('reset.signed_in.back')}</Link>
      </p>
    </div>
  )
}
