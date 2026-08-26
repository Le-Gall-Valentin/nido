import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Users } from 'lucide-react'
import { usePaletteItems } from '@/shared/lib'
import { ROUTES } from '@/shared/config'
import { useMySpaces } from '@/features/space-switcher'

export function SpacesPaletteSetup() {
  const { t } = useTranslation('spaces')
  const { data: spaces } = useMySpaces()

  const items = useMemo(() => {
    const pageGroup = t('palette.type_page', { ns: 'shell' })
    const switchGroup = t('palette.type_switch')
    return [
      { id: 'spaces:page', label: t('title'), to: ROUTES.SPACES, icon: Users, group: pageGroup },
      ...(spaces ?? []).map((space) => ({
        id: `spaces:switch:${space.id}`,
        label: t('palette.switch_to', { name: space.name }),
        to: ROUTES.spaceMembers(space.id),
        icon: Users,
        group: switchGroup,
      })),
    ]
  }, [t, spaces])

  usePaletteItems('spaces', items)
  return null
}
