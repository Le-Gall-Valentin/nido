import { useTranslation } from 'react-i18next'
import { useAuth } from '@/features/auth'
import { useMySpaces } from '@/features/space-switcher'
import { isPersonal } from '@/entities/space'
import { browserTimezone } from '@/shared/lib'
import { PersonalSpaceSection } from './PersonalSpaceSection'
import { TimezoneSuggestion } from './TimezoneSuggestion'
import { personalSpaceApi } from '../api/personalSpaceApi'

/**
 * Settings for the space that has no settings page.
 *
 * A personal space carries one member and a fixed identity, so there was never anything to edit and
 * no page to edit it on. Its calendar changed that: somebody who moves country has to be able to
 * say so, and the shared spaces' own settings screen is not where their personal one lives.
 */
export function AccountPersonalSpacePage() {
  const { t } = useTranslation('account')
  const user = useAuth((s) => s.user)
  const { data: spaces, refetch } = useMySpaces()

  if (!user) return null

  const personal = spaces?.find(isPersonal)

  async function save(timezone: string) {
    if (!personal) return
    await personalSpaceApi.updateTimezone(personal.id, timezone)
    await refetch()
  }

  return (
    <div className="mx-auto max-w-[760px] px-5 py-6 md:px-10 md:py-[34px]">
      <div className="mb-6">
        <p className="mb-1.5 text-[13px] font-semibold uppercase tracking-[0.05em] text-fg-3">
          {t('kicker')}
        </p>
        <h1 className="text-[32px] font-semibold tracking-tight text-fg-0">{t('pages.personal_space.title')}</h1>
        <p className="mt-1 text-[15px] text-fg-2">{t('pages.personal_space.subtitle')}</p>
      </div>

      <TimezoneSuggestion space={personal} browserTimezone={browserTimezone()} onAccept={save} />

      <PersonalSpaceSection space={personal} onSave={save} />
    </div>
  )
}
