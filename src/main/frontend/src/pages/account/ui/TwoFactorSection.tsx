import { useState, useRef, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { Shield } from 'lucide-react'
import { Button, Dialog, CTA_BUTTON_STYLE } from '@/shared/ui'
import { TotpSetupFlow } from '@/features/totp'
import type { ITotpEnrollApi } from '@/features/totp'
import type { User } from '@/entities/user'
import { DisableTotpModal } from './DisableTotpModal'

type Flash = { kind: 'success' | 'error'; key: string } | null

interface TwoFactorSectionProps {
  user: User
  onPatch: (partial: Partial<User>) => void
  enrollApi: ITotpEnrollApi
}

export function TwoFactorSection({ user, onPatch, enrollApi }: TwoFactorSectionProps) {
  const { t } = useTranslation('account')
  const [enableOpen, setEnableOpen] = useState(false)
  const [disableOpen, setDisableOpen] = useState(false)
  const [flash, setFlash] = useState<Flash>(null)
  const flashTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    return () => { if (flashTimer.current) clearTimeout(flashTimer.current) }
  }, [])

  function showFlash(kind: 'success' | 'error', key: string) {
    if (flashTimer.current) clearTimeout(flashTimer.current)
    setFlash({ kind, key })
    flashTimer.current = setTimeout(() => setFlash(null), 3000)
  }

  function handleEnableSuccess() {
    onPatch({ totpEnabled: true })
    setEnableOpen(false)
    showFlash('success', 'twofa.success_enabled')
  }

  function handleDisableSuccess() {
    onPatch({ totpEnabled: false })
    setDisableOpen(false)
    showFlash('success', 'twofa.success_disabled')
  }

  return (
    <section id="section-twofa" className="rounded-md border border-border bg-bg-1 mb-4 overflow-hidden">
      <div className="px-3.5 py-[10px] border-b border-border flex items-center justify-between gap-3">
        <div>
          <div className="text-xs font-semibold text-fg-0 tracking-tight">{t('twofa.title')}</div>
          <div className="text-[11px] text-fg-2 mt-px">{t('twofa.subtitle')}</div>
        </div>
        <span
          className={`shrink-0 inline-flex items-center gap-1 px-[7px] py-[2px] rounded-full border text-[10px] font-semibold font-mono uppercase tracking-[0.04em] whitespace-nowrap ${
            user.totpEnabled
              ? 'border-status-green/20 bg-status-green-dim text-status-green'
              : 'border-status-orange/20 bg-status-orange-dim text-status-orange'
          }`}
        >
          {user.totpEnabled ? t('twofa.status_enabled') : t('twofa.status_disabled')}
        </span>
      </div>
      <div className="p-3.5">
        <div className="flex items-start gap-3.5">
          <div
            className={`w-11 h-11 rounded-[10px] flex items-center justify-center shrink-0 ${
              user.totpEnabled ? 'bg-accent-dim text-accent' : 'bg-bg-3 text-fg-2'
            }`}
          >
            <Shield className="size-5" />
          </div>
          <div className="flex-1 flex flex-col gap-3 sm:flex-row sm:items-start">
            <p className="flex-1 text-[12.5px] text-fg-1 leading-[1.55]">
              {user.totpEnabled ? t('twofa.desc_enabled') : t('twofa.desc_disabled')}
            </p>
            {user.totpEnabled ? (
              <Button
                onClick={() => setDisableOpen(true)}
                className="self-end sm:self-start shrink-0 border-status-red/30 bg-status-red-dim text-status-red hover:bg-status-red/20"
              >
                {t('twofa.btn_disable')}
              </Button>
            ) : (
              <Button onClick={() => setEnableOpen(true)} className="self-end sm:self-start shrink-0 border-transparent font-semibold" style={CTA_BUTTON_STYLE}>
                <Shield className="size-3.5" />
                {t('twofa.btn_enable')}
              </Button>
            )}
          </div>
        </div>
        {flash && (
          <div
            role={flash.kind === 'success' ? 'status' : 'alert'}
            className={`mt-3 text-xs px-3 py-2 rounded-lg ${flash.kind === 'success' ? 'bg-status-green-dim text-status-green' : 'bg-status-red-dim text-status-red'}`}
          >
            {t(flash.key)}
          </div>
        )}
      </div>

      <Dialog
        open={enableOpen}
        onClose={() => setEnableOpen(false)}
        title={t('twofa.btn_enable')}
        maxWidth="max-w-lg"
      >
        <TotpSetupFlow
          api={enrollApi}
          onSuccess={handleEnableSuccess}
          onDismiss={() => setEnableOpen(false)}
          dismissLabel={t('setup.dismiss_profile', { ns: 'totp' })}
        />
      </Dialog>

      <DisableTotpModal
        open={disableOpen}
        onClose={() => setDisableOpen(false)}
        onSuccess={handleDisableSuccess}
        onDisable={enrollApi.disable}
      />
    </section>
  )
}