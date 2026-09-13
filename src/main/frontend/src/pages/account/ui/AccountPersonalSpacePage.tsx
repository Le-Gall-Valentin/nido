import { useTranslation } from 'react-i18next'
import { useAuth } from '@/features/auth'
import { useMySpaces } from '@/features/space-switcher'
import { isPersonal, useUpdateSpace, type SpaceSummary } from '@/entities/space'
import { browserTimezone } from '@/shared/lib'
import { PersonalSpaceSection } from './PersonalSpaceSection'
import { TimezoneSuggestion } from './TimezoneSuggestion'

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
  const { data: spaces } = useMySpaces()

  if (!user) return null

  const personal = spaces?.find(isPersonal)

  return (
    <div className="mx-auto max-w-[760px] px-5 py-6 md:px-10 md:py-[34px]">
      <div className="mb-6">
        <p className="mb-1.5 text-[13px] font-semibold uppercase tracking-[0.05em] text-fg-3">
          {t('kicker')}
        </p>
        <h1 className="text-[32px] font-semibold tracking-tight text-fg-0">{t('pages.personal_space.title')}</h1>
        <p className="mt-1 text-[15px] text-fg-2">{t('pages.personal_space.subtitle')}</p>
      </div>

      {personal && <PersonalSpaceSettings space={personal} />}
    </div>
  )
}

/**
 * Split from the page so the space is known: the write goes through the space entity's own mutation,
 * which needs an id, and a hook cannot wait for a query to resolve. Mounting this only once the
 * personal space is loaded is what makes that id real rather than a placeholder.
 */
function PersonalSpaceSettings({ space }: { space: SpaceSummary }) {
  const updateSpace = useUpdateSpace(space.id)

  function save(timezone: string): Promise<void> {
    return updateSpace.mutateAsync({ timezone })
  }

  return (
    <>
      <TimezoneSuggestion space={space} browserTimezone={browserTimezone()} onAccept={save} />

      <PersonalSpaceSection space={space} onSave={save} />
    </>
  )
}
