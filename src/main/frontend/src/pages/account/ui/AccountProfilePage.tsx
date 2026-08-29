import { useTranslation } from 'react-i18next'
import { useShallow } from 'zustand/react/shallow'
import { useAuth } from '@/features/auth'
import { accountApi } from '../api/accountApi'
import type { IAccountApi } from '../model/IAccountApi'
import { ProfileSummaryCard } from './ProfileSummaryCard'
import { ProfileEditSection } from './ProfileEditSection'

interface AccountProfilePageProps {
  /** Composition seam: defaults to the real implementation; tests inject a fake. */
  api?: IAccountApi
}

export function AccountProfilePage({ api = accountApi }: AccountProfilePageProps = {}) {
  const { t } = useTranslation('account')
  const { user, patchUser } = useAuth(
    useShallow(s => ({ user: s.user, patchUser: s.patchUser }))
  )

  if (!user) return null

  return (
    <div className="mx-auto max-w-[760px] px-5 py-6 md:px-10 md:py-[34px]">
      <div className="mb-6">
        <p className="mb-1.5 text-[13px] font-semibold uppercase tracking-[0.05em] text-fg-3">
          {t('kicker')}
        </p>
        <h1 className="text-[32px] font-semibold tracking-tight text-fg-0">{t('pages.profile.title')}</h1>
        <p className="mt-1 text-[15px] text-fg-2">{t('pages.profile.subtitle')}</p>
      </div>

      <ProfileSummaryCard user={user} />

      <ProfileEditSection
        user={user}
        onPatch={patchUser}
        onUpdateProfile={api.updateProfile}
      />
    </div>
  )
}
