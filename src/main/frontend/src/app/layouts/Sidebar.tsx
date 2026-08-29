import { useMemo } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { LucideIcon } from 'lucide-react'
import { useAuth } from '@/features/auth'
import { useCurrentSpaceId } from '@/features/space-switcher'
import { NidoMark } from '@/shared/ui'
import { ROUTES } from '@/shared/config'
import { isAdminRole } from '@/entities/user'
import { NAV_CONFIG } from './navConfig'
import { NavList } from './NavList'

export const BRAND_LOGO_GRADIENT = 'linear-gradient(135deg, var(--brand-icon-from), var(--brand-icon-to))'

interface NavItemProps {
  to: string
  icon: LucideIcon
  label: string
  pathname: string
  /** Overrides the pathname-derived active check — used for a parent item that should stay highlighted while any of its children is the active route. */
  activeOverride?: boolean
}

export function NavItem({ to, icon: Icon, label, pathname, activeOverride }: NavItemProps) {
  const active = activeOverride ?? (pathname === to || pathname.startsWith(`${to}/`))

  return (
    <Link
      to={to}
      className={`flex items-center gap-3 rounded-[10px] px-3 py-2.5 text-sm transition-colors ${
        active
          ? 'bg-accent-dim font-semibold text-status-green'
          : 'font-medium text-fg-1 hover:bg-bg-3 hover:text-fg-0'
      }`}
    >
      <Icon size={19} className={`shrink-0 ${active ? 'text-accent' : 'text-fg-2'}`} />
      <span className="min-w-0 flex-1 truncate">{label}</span>
    </Link>
  )
}

/**
 * The desktop shell chrome — always visible on md+ screens, at a fixed
 * width. There is no collapse-to-icon-rail: a narrower rail hides item
 * labels and sub-navigation, which makes Cuisine/Paramètres' sub-items
 * unreachable, so the full nav is always shown. Replaced by
 * MobileNavDrawer below the md breakpoint.
 */
export function Sidebar() {
  const { t } = useTranslation('shell')
  const user = useAuth((s) => s.user)
  const { pathname } = useLocation()
  const { spaceId } = useCurrentSpaceId()

  const visibleItems = useMemo(
    () => NAV_CONFIG.filter((item) => !item.adminOnly || isAdminRole(user?.role)),
    [user?.role]
  )

  return (
    <aside className="hidden shrink-0 flex-col border-r border-border bg-bg-2 md:sticky md:top-0 md:flex md:h-screen md:w-[236px]">
      {/* Brand */}
      <Link to={ROUTES.ACCOUNT} className="flex shrink-0 items-center gap-[11px] px-[18px] pb-4 pt-5">
        <div
          className="grid size-[34px] shrink-0 place-items-center rounded-[10px] text-white"
          style={{ background: BRAND_LOGO_GRADIENT }}
        >
          <NidoMark size={19} />
        </div>
        <span className="text-xl font-bold tracking-tight text-fg-0" style={{ fontFamily: 'var(--font-family-display)' }}>
          {t('brand')}
        </span>
      </Link>

      {/* Nav */}
      <nav className="flex flex-1 flex-col gap-[3px] overflow-y-auto px-3 py-2" aria-label={t('nav.label')}>
        <NavList items={visibleItems} spaceId={spaceId} pathname={pathname} />
      </nav>
    </aside>
  )
}
