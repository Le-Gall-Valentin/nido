import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Key, Mail, Smartphone } from 'lucide-react'
import { Alert, Dialog, Button } from '@/shared/ui'
import type { AdminUser, TwoFactorMethod } from '@/entities/user'
import type { MailAvailability } from '@/entities/capabilities'
import { useDialogSubmit } from '../lib/useDialogSubmit'
import { MailNotice } from './MailNotice'

const ORDER: TwoFactorMethod[] = ['APP', 'MAIL']

interface ResetTwoFactorDialogProps {
  user: AdminUser
  mail: MailAvailability
  onClose: () => void
  onReset: (input: { id: string; methods: TwoFactorMethod[] }) => Promise<void>
  onSuccess: () => void
}

/** The administrator ticks what to remove: nothing is ticked when there is a choice to make. */
export function ResetTwoFactorDialog({ user, mail, onClose, onReset, onSuccess }: ResetTwoFactorDialogProps) {
  const { t } = useTranslation('adminUsers')
  const active = ORDER.filter(m => user.twoFactorMethods.includes(m))
  const single = active.length === 1
  const [ticked, setTicked] = useState<TwoFactorMethod[]>(single ? active : [])
  const { submit, isLoading, errorKey, clearError } = useDialogSubmit('reset_two_factor')
  const kept = active.filter(m => !ticked.includes(m))

  function toggle(method: TwoFactorMethod) {
    if (single) return
    setTicked(current => ORDER.filter(m => (m === method ? !current.includes(m) : current.includes(m))))
  }

  function handleSubmit() {
    void submit(async () => {
      await onReset({ id: user.id, methods: ticked })
      onSuccess()
    })
  }

  function handleClose() {
    clearError()
    onClose()
  }

  return (
    <Dialog open onClose={handleClose} title={t('reset_two_factor.title', { username: user.username })} maxWidth="max-w-[460px]">
      <div className="mb-4">
        <h3 className="text-xl font-semibold text-fg-0 mb-1.5">{t('reset_two_factor.title', { username: user.username })}</h3>
        <p className="text-sm text-fg-2 leading-relaxed">{t('reset_two_factor.body', { username: user.username })}</p>
      </div>

      <div className="mb-4 grid gap-2.5">
        {active.map(method => {
          const isTicked = ticked.includes(method)
          const Icon = method === 'APP' ? Smartphone : Mail
          return (
            <label
              key={method}
              className={`grid grid-cols-[18px_34px_minmax(0,1fr)] items-center gap-3 rounded-[12px] border-[1.5px] px-3 py-2.5 ${
                isTicked ? 'border-status-orange bg-status-orange-dim' : 'border-border bg-bg-1'
              } ${single ? '' : 'cursor-pointer'}`}
            >
              <input
                type="checkbox"
                checked={isTicked}
                disabled={single || isLoading}
                onChange={() => toggle(method)}
                className="size-[18px] accent-[var(--color-status-orange)]"
              />
              <span className="grid size-[34px] place-items-center rounded-[9px] bg-accent-dim text-accent">
                <Icon className="size-4" aria-hidden="true" />
              </span>
              <span className="min-w-0">
                <span className="block text-[13.5px] font-semibold text-fg-0">{t(method === 'APP' ? 'reset_two_factor.app' : 'reset_two_factor.mail')}</span>
                <span className="block text-xs text-fg-2">{method === 'APP' ? t('reset_two_factor.app_hint') : user.email}</span>
              </span>
            </label>
          )
        })}
      </div>

      {ticked.length > 0 && (kept.length === 0 ? (
        // A consequence of the boxes ticked, not an error: shown in red, but not announced as an alert at each tick.
        <p className="mb-3 rounded-[10px] bg-status-red-dim px-3.5 py-[11px] text-[13px] text-status-red">
          {t('reset_two_factor.none_left')}
        </p>
      ) : (
        <p className="mb-3 rounded-[10px] bg-bg-3 px-3.5 py-[11px] text-[13px] text-fg-1">
          {t(kept[0] === 'APP' ? 'reset_two_factor.keeps_app' : 'reset_two_factor.keeps_mail')}
        </p>
      ))}

      <Alert variant="warning" className="mb-5">{t('reset_two_factor.warning')}</Alert>

      <MailNotice user={user} mail={mail} />

      {errorKey && <Alert variant="error" className="mb-4">{t(errorKey)}</Alert>}

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={handleClose} disabled={isLoading}>{t('reset_two_factor.cancel')}</Button>
        <Button
          onClick={handleSubmit}
          isLoading={isLoading}
          disabled={ticked.length === 0}
          className="border-status-orange/30 bg-status-orange-dim text-status-orange hover:bg-status-orange/20"
        >
          <Key className="size-4" />
          {t(ticked.length === 2 ? 'reset_two_factor.submit_all' : 'reset_two_factor.submit_one')}
        </Button>
      </div>
    </Dialog>
  )
}
