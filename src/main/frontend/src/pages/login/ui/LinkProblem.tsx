import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ChevronLeft, Clock } from 'lucide-react'
import { ROUTES } from '@/shared/config'
import { Alert, Button, CTA_ELEVATED_STYLE } from '@/shared/ui'

/**
 * A one-time link that no longer works. An alert: it can stop working while the password is being typed, and
 * the form that disappears must be replaced by something said aloud.
 */
export function LinkInvalid({ title, body, children }: { title: string; body: string; children?: ReactNode }) {
  const { t } = useTranslation('login')
  return (
    <div role="alert">
      <div className="mb-5 grid size-11 place-items-center rounded-xl bg-status-red-dim text-status-red">
        <Clock className="size-5" aria-hidden="true" />
      </div>
      <h1 className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">{title}</h1>
      <p className="mb-6 text-sm leading-relaxed text-fg-2">{body}</p>
      {children}
      <p className="mt-6 text-center">
        <Link to={ROUTES.LOGIN} className="inline-flex items-center gap-1 text-[13px] text-fg-2 hover:text-fg-0">
          <ChevronLeft className="size-3.5" aria-hidden="true" />
          {t('forgot.back')}
        </Link>
      </p>
    </div>
  )
}

/** The way out of an invalid link: a primary action, styled as the page's main button. */
export function LinkInvalidAction({ to, children }: { to: string; children: ReactNode }) {
  return (
    <Link to={to} className="block w-full rounded-[11px] py-3.5 text-center text-[15px] font-semibold" style={CTA_ELEVATED_STYLE}>
      {children}
    </Link>
  )
}

/** The server could not be asked about the link: say so, and ask again on demand. */
export function LinkUnavailable({ message, onRetry }: { message: string; onRetry: () => void }) {
  const { t } = useTranslation('login')
  return (
    <div className="flex flex-col gap-4">
      <Alert variant="error">{message}</Alert>
      <Button type="button" onClick={onRetry} className="w-full rounded-[11px] py-3">{t('reset.retry')}</Button>
    </div>
  )
}
