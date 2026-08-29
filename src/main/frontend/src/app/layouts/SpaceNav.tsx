import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ArrowLeft } from 'lucide-react'
import { ROUTES } from '@/shared/config'
import { NavItem } from './Sidebar'
import type { SpaceNavItemConfig } from './navConfig'

export interface SpaceNavProps {
  items: SpaceNavItemConfig[]
  spaceId: string
  pathname: string
}

/**
 * Renders the nav for a space-scoped route: a link back out to the space
 * list, then each configured item. An item's children render underneath it
 * exactly when the current path already points at one of them — there is no
 * separate click-to-expand affordance to keep in sync with the route.
 */
export function SpaceNav({ items, spaceId, pathname }: SpaceNavProps) {
  const { t } = useTranslation('shell')

  return (
    <>
      <Link
        to={ROUTES.SPACES}
        className="mb-1 flex items-center gap-2 rounded-[10px] px-3 py-2 text-xs font-semibold text-fg-3 transition-colors hover:bg-bg-3 hover:text-fg-1"
      >
        <ArrowLeft size={15} className="shrink-0" aria-hidden="true" />
        {t('nav.back_to_groups')}
      </Link>
      {items.map((item) => {
        const expanded = item.children?.some((child) => pathname.startsWith(child.to(spaceId))) ?? false
        return (
          <div key={item.id}>
            <NavItem to={item.to(spaceId)} icon={item.icon} label={t(item.labelKey)} pathname={pathname} />
            {expanded && (
              <div className="ml-[22px] mt-[2px] flex flex-col gap-[2px] border-l border-border pl-[11px]">
                {item.children!.map((child) => (
                  <NavItem
                    key={child.id}
                    to={child.to(spaceId)}
                    icon={child.icon}
                    label={t(child.labelKey)}
                    pathname={pathname}
                  />
                ))}
              </div>
            )}
          </div>
        )
      })}
    </>
  )
}
