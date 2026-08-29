import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Lock, SlidersHorizontal, User } from 'lucide-react'
import { usePaletteItems } from '@/shared/lib'
import { ROUTES } from '@/shared/config'

export function AccountPaletteSetup() {
  const { t } = useTranslation('account')

  const items = useMemo(() => {
    const pageGroup = t('palette.type_page', { ns: 'shell' })
    return [
      { id: 'account:profile',     label: t('pages.profile.title'),     to: ROUTES.ACCOUNT_PROFILE,     icon: User,              group: pageGroup },
      { id: 'account:security',    label: t('pages.security.title'),    to: ROUTES.ACCOUNT_SECURITY,    icon: Lock,              group: pageGroup },
      { id: 'account:preferences', label: t('pages.preferences.title'), to: ROUTES.ACCOUNT_PREFERENCES, icon: SlidersHorizontal, group: pageGroup },
    ]
  }, [t])

  usePaletteItems('account', items)
  return null
}