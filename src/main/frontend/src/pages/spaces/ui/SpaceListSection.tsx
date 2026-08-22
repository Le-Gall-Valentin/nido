import { useTranslation } from 'react-i18next'
import { ChevronRight, Plus } from 'lucide-react'
import { SpaceAvatar, isPersonal, type SpaceSummary } from '@/entities/space'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'

interface SpaceListSectionProps {
  spaces: SpaceSummary[]
  /** Choosing a group navigates to its members page; the personal space is not clickable. */
  onSelect: (spaceId: string) => void
  onCreateClick: () => void
}

export function SpaceListSection({ spaces, onSelect, onCreateClick }: SpaceListSectionProps) {
  const { t } = useTranslation('spaces')
  const personal = spaces.find((s) => isPersonal(s))
  const shared = spaces.filter((s) => !isPersonal(s))

  return (
    <section>
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="text-[13px] font-semibold uppercase tracking-[0.05em] text-fg-3">{t('list.title')}</h2>
        <Button
          onClick={onCreateClick}
          className="border-transparent font-semibold"
          style={CTA_BUTTON_STYLE}
        >
          <Plus className="size-4" />
          {t('list.action_create')}
        </Button>
      </div>

      <ul className="flex flex-col gap-2">
        {personal && (
          <li className="flex items-center gap-3.5 rounded-2xl border border-border bg-bg-1 px-[18px] py-3.5">
            <SpaceAvatar space={personal} size="md" />
            <div className="min-w-0 flex-1">
              <div className="text-[14.5px] font-semibold text-fg-0 truncate">{personal.name}</div>
              <div className="text-[12.5px] text-fg-3">{t('list.personal_subtitle')}</div>
            </div>
          </li>
        )}

        {shared.map((space) => (
          <li key={space.id}>
            <button
              type="button"
              onClick={() => onSelect(space.id)}
              className="flex w-full items-center gap-3.5 rounded-2xl border border-border bg-bg-1 px-[18px] py-3.5 text-left transition-colors hover:bg-bg-2"
            >
              <SpaceAvatar space={space} size="md" />
              <div className="min-w-0 flex-1">
                <div className="text-[14.5px] font-semibold text-fg-0 truncate">{space.name}</div>
                <div className="text-[12.5px] text-fg-3 truncate">
                  {t('list.member_count', { count: space.memberCount })} · {t(`space:role.${space.myRole}`)}
                </div>
              </div>
              <ChevronRight className="size-4 shrink-0 text-fg-3" aria-hidden="true" />
            </button>
          </li>
        ))}

        {shared.length === 0 && (
          <li className="rounded-2xl border border-dashed border-border px-[18px] py-6 text-center text-sm text-fg-2">
            {t('list.empty')}
          </li>
        )}
      </ul>
    </section>
  )
}
