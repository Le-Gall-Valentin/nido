import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Info } from 'lucide-react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Dialog } from '@/shared/ui'
import { NetworkError } from '@/shared/lib'
import type { TwoFactorMethod, User } from '@/entities/user'
import {
  AppSetupFlow, MailSetupStep, MethodAlreadyEnabledError, MethodNotEnabledError, MethodUnavailableError, ResendTooSoonError,
  SendLimitError,
  type ITwoFactorMethodsApi, type MailSetupData, type MethodState,
} from '@/features/two-factor'
import { MethodRow, StatusBadge } from './MethodRow'
import { DisableMethodDialog } from './DisableMethodDialog'

const METHODS_KEY = ['two-factor', 'methods'] as const
const ORDER: TwoFactorMethod[] = ['APP', 'MAIL']

type Flash = { kind: 'success' | 'error'; key: string; values?: Record<string, unknown> } | null
type Open =
  | { dialog: 'enable_app' }
  | { dialog: 'enable_mail'; setup: MailSetupData }
  | { dialog: 'disable'; method: TwoFactorMethod; paused: boolean; resendAfterSeconds: number }
  | null

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
  const queryClient = useQueryClient()
  const { data: methods, isError } = useQuery({ queryKey: METHODS_KEY, queryFn: () => api.list() })
  const [open, setOpen] = useState<Open>(null)
  const [busy, setBusy] = useState(false)
  const [flash, setFlash] = useState<Flash>(null)
  const flashTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => () => { if (flashTimer.current) clearTimeout(flashTimer.current) }, [])

  function showFlash(kind: 'success' | 'error', key: string, values?: Record<string, unknown>) {
    if (flashTimer.current) clearTimeout(flashTimer.current)
    setFlash({ kind, key, values })
    flashTimer.current = setTimeout(() => setFlash(null), 4000)
  }

  function settle(method: TwoFactorMethod, enabled: boolean) {
    const on = new Set(user.twoFactorMethods)
    if (enabled) on.add(method)
    else on.delete(method)
    onPatch({ twoFactorMethods: ORDER.filter(m => on.has(m)) })
    void queryClient.invalidateQueries({ queryKey: METHODS_KEY })
  }

  function showError(error: unknown) {
    if (error instanceof MethodUnavailableError) {
      showFlash('error', 'twofa.error.mail_unavailable')
      void queryClient.invalidateQueries({ queryKey: METHODS_KEY })
    } else if (error instanceof MethodAlreadyEnabledError || error instanceof MethodNotEnabledError) {
      // Turned on or off meanwhile — in another tab, or by an administrator: the list shows where things stand.
      showFlash('error', 'twofa.error.changed')
      void queryClient.invalidateQueries({ queryKey: METHODS_KEY })
    } else if (error instanceof SendLimitError) {
      showFlash('error', 'twofa.error.send_limit', { minutes: Math.ceil(error.seconds / 60) })
    } else if (error instanceof NetworkError) {
      showFlash('error', 'twofa.error.network')
    } else {
      showFlash('error', 'twofa.error.server')
    }
  }

  // The mail's code leaves on the click, never from an effect: StrictMode would send it twice.
  async function enable(method: TwoFactorMethod) {
    if (method === 'APP') { setOpen({ dialog: 'enable_app' }); return }
    setBusy(true)
    try {
      setOpen({ dialog: 'enable_mail', setup: await api.setupMail() })
    } catch (error) {
      if (error instanceof ResendTooSoonError) setOpen({ dialog: 'enable_mail', setup: { sentTo: user.email, resendAfterSeconds: error.seconds } })
      else showError(error)
    } finally {
      setBusy(false)
    }
  }

  async function disable(state: MethodState) {
    const paused = state.enabled && !state.usable
    if (state.method === 'APP' || paused) {
      setOpen({ dialog: 'disable', method: state.method, paused, resendAfterSeconds: 0 })
      return
    }
    setBusy(true)
    try {
      const { resendAfterSeconds } = await api.sendDisableCode()
      setOpen({ dialog: 'disable', method: 'MAIL', paused: false, resendAfterSeconds })
    } catch (error) {
      if (error instanceof ResendTooSoonError) setOpen({ dialog: 'disable', method: 'MAIL', paused: false, resendAfterSeconds: error.seconds })
      else showError(error)
    } finally {
      setBusy(false)
    }
  }

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
        <Dialog open onClose={() => setOpen(null)} title={t('twofa.enable_app_title')} maxWidth="max-w-lg">
          <AppSetupFlow
            api={api}
            onSuccess={() => { settle('APP', true); setOpen(null); showFlash('success', 'twofa.success_enabled_app') }}
            onDismiss={() => setOpen(null)}
            dismissLabel={t('setup.dismiss_profile', { ns: 'twoFactor' })}
          />
        </Dialog>
      )}

      {open?.dialog === 'enable_mail' && (
        <Dialog open onClose={() => setOpen(null)} title={t('twofa.enable_mail_title')} maxWidth="max-w-md">
          <MailSetupStep
            variant="dialog"
            sentTo={open.setup.sentTo}
            resendAfterSeconds={open.setup.resendAfterSeconds}
            api={api}
            onSuccess={() => { settle('MAIL', true); setOpen(null); showFlash('success', 'twofa.success_enabled_mail') }}
            onDismiss={() => setOpen(null)}
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
          onClose={() => setOpen(null)}
          onSuccess={() => { settle(open.method, false); setOpen(null); showFlash('success', 'twofa.success_disabled') }}
          onStale={() => {
            setOpen(null)
            showFlash('error', 'twofa.error.changed')
            void queryClient.invalidateQueries({ queryKey: METHODS_KEY })
          }}
        />
      )}
    </section>
  )
}
