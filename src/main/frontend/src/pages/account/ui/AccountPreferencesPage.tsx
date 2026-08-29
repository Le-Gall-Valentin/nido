import { useTranslation } from 'react-i18next'
import { useAuth } from '@/features/auth'
import { PreferencesSection } from './PreferencesSection'

export function AccountPreferencesPage() {
  const { t } = useTranslation('account')
  const user = useAuth((s) => s.user)

  if (!user) return null

  return (
    <div className="mx-auto max-w-[760px] px-5 py-6 md:px-10 md:py-[34px]">
      <div className="mb-6">
        <p className="mb-1.5 text-[13px] font-semibold uppercase tracking-[0.05em] text-fg-3">
          {t('kicker')}
        </p>
        <h1 className="text-[32px] font-semibold tracking-tight text-fg-0">{t('pages.preferences.title')}</h1>
        <p className="mt-1 text-[15px] text-fg-2">{t('pages.preferences.subtitle')}</p>
      </div>

      <PreferencesSection />
    </div>
  )
}
