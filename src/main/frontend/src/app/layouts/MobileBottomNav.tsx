import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { NavItemConfig } from './navConfig'

export interface MobileBottomNavProps {
  items: NavItemConfig[]
  spaceId: string | undefined
  pathname: string
}

/**
 * Replaces the sidebar entirely below the md breakpoint — the mockup has no
 * mobile drawer, only this persistent bottom tab bar over the same item
 * list as the desktop sidebar (tapping a parent item with children, e.g.
 * Paramètres, lands on its first child; there is no sub-navigation here).
 */
export function MobileBottomNav({ items, spaceId, pathname }: MobileBottomNavProps) {
  const { t } = useTranslation('shell')

  return (
    <nav
      className="fixed inset-x-0 bottom-0 z-40 flex border-t border-border bg-bg-2/95 pb-[calc(env(safe-area-inset-bottom)+4px)] pt-1 backdrop-blur-md md:hidden"
      aria-label={t('nav.label')}
    >
      {items.map((item) => {
        const to = item.to(spaceId)
        if (!to) return null
        const active = pathname === to || pathname.startsWith(`${to}/`)
          || (item.children?.some((child) => {
            const childTo = child.to(spaceId)
            return !!childTo && (pathname === childTo || pathname.startsWith(`${childTo}/`))
          }) ?? false)
        const Icon = item.icon
        return (
          <Link
            key={item.id}
            to={to}
            className={`flex flex-1 flex-col items-center gap-[3px] px-1 py-1.5 ${active ? 'text-status-green' : 'text-fg-3'}`}
          >
            <Icon size={22} className={active ? 'text-accent' : 'text-fg-3'} />
            <span className="max-w-full truncate text-[10.5px] font-semibold">{t(item.labelKey)}</span>
          </Link>
        )
      })}
    </nav>
  )
}
