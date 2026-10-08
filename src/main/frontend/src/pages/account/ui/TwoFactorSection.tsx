import { useTranslation } from 'react-i18next'
import { Info } from 'lucide-react'
import { Dialog } from '@/shared/ui'
import type { User } from '@/entities/user'
import { AppSetupFlow, MailSetupStep, type ITwoFactorMethodsApi, type MethodState } from '@/features/two-factor'
import { useTwoFactorMethods } from '../model/useTwoFactorMethods'
import { MethodRow, StatusBadge } from './MethodRow'
import { DisableMethodDialog } from './DisableMethodDialog'

function summaryOf(methods: MethodState[]): 'enabled' | 'paused' | 'disabled' {
  if (methods.some(m => m.enabled && m.usable)) return 'enabled'
  if (methods.some(m => m.enabled)) return 'paused'
  return 'disabled'
}

interface TwoFactorSectionProps {
  user: User
  onPatch: (partial: Partial<User>) => void
  api: ITwoFactorMethodsApi
}

export function TwoFactorSection({ user, onPatch, api }: TwoFactorSectionProps) {
  const { t } = useTranslation('account')
  const { methods, isError, busy, open, flash, close, enable, disable, enabled, disabled, stale } =
    useTwoFactorMethods({ user, onPatch, api })
  const appOn = methods?.some(m => m.method === 'APP' && m.enabled) ?? false

  return (
    <section id="section-twofa" className="rounded-2xl border border-border bg-bg-1 mb-4 overflow-hidden">
      <div className="px-7 pt-6 pb-4">
        <div className="flex flex-wrap items-center gap-[9px]">
          <h3 className="text-lg font-semibold text-fg-0">{t('twofa.title')}</h3>
          {methods && <StatusBadge status={summaryOf(methods)} />}
        </div>
        <p className="text-[13.5px] text-fg-2 mt-0.5">{t('twofa.subtitle')}</p>
      </div>

      {isError && <p role="alert" className="px-7 pb-4 text-[13px] text-status-red">{t('twofa.error.load')}</p>}

      {methods?.map(state => (
        <MethodRow
          key={state.method}
          state={state}
          email={user.email}
          appOn={appOn}
          busy={busy}
          onEnable={() => void enable(state.method)}
          onDisable={() => void disable(state)}
        />
      ))}

      {flash && (
        <div
          role={flash.kind === 'success' ? 'status' : 'alert'}
          className={`mx-7 mb-4 text-xs px-3 py-2 rounded-lg ${flash.kind === 'success' ? 'bg-status-green-dim text-status-green' : 'bg-status-red-dim text-status-red'}`}
        >
          {t(flash.key, flash.values)}
        </div>
      )}

      <div className="flex gap-2 border-t border-border px-7 py-4 text-[12.5px] text-fg-2">
        <Info className="mt-0.5 size-3.5 shrink-0" />
        <span>{t('twofa.help')}</span>
      </div>

      {open?.dialog === 'enable_app' && (
        <Dialog open onClose={close} title={t('twofa.enable_app_title')} maxWidth="max-w-lg">
          <AppSetupFlow
            api={api}
            onSuccess={() => enabled('APP')}
            onDismiss={close}
            dismissLabel={t('setup.dismiss_profile', { ns: 'twoFactor' })}
          />
        </Dialog>
      )}

      {open?.dialog === 'enable_mail' && (
        <Dialog open onClose={close} title={t('twofa.enable_mail_title')} maxWidth="max-w-md">
          <MailSetupStep
            variant="dialog"
            sentTo={open.setup.sentTo}
            resendAfterSeconds={open.setup.resendAfterSeconds}
            api={api}
            onSuccess={() => enabled('MAIL')}
            onDismiss={close}
            dismissLabel={t('setup.dismiss_profile', { ns: 'twoFactor' })}
          />
        </Dialog>
      )}

      {open?.dialog === 'disable' && (
        <DisableMethodDialog
          method={open.method}
          paused={open.paused}
          address={user.email}
          resendAfterSeconds={open.resendAfterSeconds}
          api={api}
          onClose={close}
          onSuccess={() => disabled(open.method)}
          onStale={stale}
        />
      )}
    </section>
  )
}
