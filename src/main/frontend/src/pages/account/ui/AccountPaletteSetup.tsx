import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { KeyRound, Shield, SlidersHorizontal, User } from 'lucide-react'
import { usePaletteItems } from '@/shared/lib'
import { ROUTES } from '@/shared/config'

export function AccountPaletteSetup() {
  const { t } = useTranslation('account')

  const items = useMemo(() => {
    const pageGroup = t('palette.type_page', { ns: 'shell' })
    const sectionGroup = t('title')
    return [
      { id: 'account:page',        label: t('title'),             to: ROUTES.ACCOUNT,                          icon: User,              group: pageGroup },
      { id: 'account:profile',     label: t('profile.title'),     to: `${ROUTES.ACCOUNT}#section-profile`,     icon: User,              group: sectionGroup },
      { id: 'account:twofa',       label: t('twofa.title'),       to: `${ROUTES.ACCOUNT}#section-twofa`,       icon: Shield,            group: sectionGroup },
      { id: 'account:preferences', label: t('preferences.title'), to: `${ROUTES.ACCOUNT}#section-preferences`, icon: SlidersHorizontal, group: sectionGroup },
      { id: 'account:password',    label: t('password.title'),    to: `${ROUTES.ACCOUNT}#section-password`,    icon: KeyRound,          group: sectionGroup },
    ]
  }, [t])

  usePaletteItems('account', items)
  return null
}