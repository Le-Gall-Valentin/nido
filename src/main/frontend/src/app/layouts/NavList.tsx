import { useTranslation } from 'react-i18next'
import { NavItem } from './Sidebar'
import type { NavItemConfig } from './navConfig'

export interface NavListProps {
  items: NavItemConfig[]
  spaceId: string | undefined
  pathname: string
  /** Renders every parent's children regardless of the active route — used by the mobile drawer, where sub-navigation must stay reachable without first navigating into the section. */
  alwaysExpanded?: boolean
  /** Marks the "Membres et groupes" item (nav:spaces) with a badge — the only item this currently applies to. */
  hasPendingInvitations?: boolean
  /** Numeric badge on the "Tâches" child item (nav:organisation:tasks) — the only item this currently applies to. */
  openTaskCount?: number
}

/**
 * Renders the persistent sidebar's flat item list. An item whose `to`
 * doesn't resolve yet (a space-scoped item before the current space is
 * known) is skipped entirely — never a dead link. On the desktop sidebar,
 * a parent's children render exactly when the current path already points
 * at one of them, keeping the highlighted parent in sync with the route
 * without a separate click-to-expand affordance; the mobile drawer instead
 * passes `alwaysExpanded` so every sub-item stays visible at all times.
 */
export function NavList({ items, spaceId, pathname, alwaysExpanded, hasPendingInvitations, openTaskCount }: NavListProps) {
  const { t } = useTranslation('shell')

  return (
    <>
      {items.map((item) => {
        const to = item.to(spaceId)
        if (!to) return null
        const onActiveChild = item.children?.some((child) => {
          const childTo = child.to(spaceId)
          return !!childTo && (pathname === childTo || pathname.startsWith(`${childTo}/`))
        }) ?? false
        const expanded = !!item.children && (alwaysExpanded || onActiveChild)
        return (
          <div key={item.id}>
            <NavItem
              to={to}
              icon={item.icon}
              label={t(item.labelKey)}
              pathname={pathname}
              activeOverride={item.children ? onActiveChild : undefined}
              hasBadge={item.id === 'nav:spaces' && !!hasPendingInvitations}
            />
            {expanded && (
              <div className="ml-[22px] mt-[2px] flex flex-col gap-[2px] border-l border-border pl-[11px]">
                {item.children!.map((child) => {
                  const childTo = child.to(spaceId)
                  if (!childTo) return null
                  return (
                    <NavItem key={child.id} to={childTo} icon={child.icon} label={t(child.labelKey)} pathname={pathname}
                      badgeCount={child.id === 'nav:organisation:tasks' ? openTaskCount : undefined} />
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
