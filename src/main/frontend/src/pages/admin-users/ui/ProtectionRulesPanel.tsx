import { useTranslation, Trans } from 'react-i18next'
import { Shield } from 'lucide-react'

const RULE_KEYS = ['super_admin', 'self', 'admin_admin', 'totp'] as const

export function ProtectionRulesPanel() {
  const { t } = useTranslation('adminUsers')

  return (
    <div className="mt-3 rounded-md border border-border bg-bg-1 p-4">
      <div className="flex gap-3">
        <Shield className="size-4 shrink-0 text-fg-3 mt-0.5" aria-hidden="true" />
        <div className="text-[12px] leading-relaxed">
          <div className="font-semibold text-fg-0 mb-2">{t('rules.title')}</div>
          <ul className="space-y-1 text-fg-2">
            {RULE_KEYS.map(key => (
              <li key={key}>
                {'· '}
                <Trans t={t} i18nKey={`rules.${key}`} components={{ strong: <strong /> }} />
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}
