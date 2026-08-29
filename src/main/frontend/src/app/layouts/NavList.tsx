import { useTranslation } from 'react-i18next'
import { NavItem } from './Sidebar'
import type { NavItemConfig } from './navConfig'

export interface NavListProps {
  items: NavItemConfig[]
  spaceId: string | undefined
  pathname: string
}

/**
 * Renders the persistent sidebar's flat item list. An item whose `to`
 * doesn't resolve yet (a space-scoped item before the current space is
 * known) is skipped entirely — never a dead link. A parent's children
 * render exactly when the current path already points at one of them;
 * there is no separate click-to-expand affordance to keep in sync with
 * the route.
 */
export function NavList({ items, spaceId, pathname }: NavListProps) {
  const { t } = useTranslation('shell')

  return (
    <>
      {items.map((item) => {
        const to = item.to(spaceId)
        if (!to) return null
        const expanded = item.children?.some((child) => {
          const childTo = child.to(spaceId)
          return !!childTo && pathname.startsWith(childTo)
        }) ?? false
        return (
          <div key={item.id}>
            <NavItem to={to} icon={item.icon} label={t(item.labelKey)} pathname={pathname} />
            {expanded && (
              <div className="ml-[22px] mt-[2px] flex flex-col gap-[2px] border-l border-border pl-[11px]">
                {item.children!.map((child) => {
                  const childTo = child.to(spaceId)
                  if (!childTo) return null
                  return (
                    <NavItem key={child.id} to={childTo} icon={child.icon} label={t(child.labelKey)} pathname={pathname} />
                  )
                })}
              </div>
            )}
          </div>
        )
      })}
    </>
  )
}
