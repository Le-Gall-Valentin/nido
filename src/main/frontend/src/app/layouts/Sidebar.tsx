import { useMemo } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { LucideIcon } from 'lucide-react'
import { ChevronLeft } from 'lucide-react'
import { useAuth } from '@/features/auth'
import { useCurrentSpaceId } from '@/features/space-switcher'
import { NidoMark } from '@/shared/ui'
import { ROUTES } from '@/shared/config'
import { isAdminRole } from '@/entities/user'
import { NAV_CONFIG } from './navConfig'
import { NavList } from './NavList'
import { sidebarCollapseStore } from './sidebarCollapseStore'

const BRAND_LOGO_GRADIENT = 'linear-gradient(135deg, var(--brand-icon-from), var(--brand-icon-to))'

interface NavItemProps {
  to: string
  icon: LucideIcon
  label: string
  pathname: string
  /** Overrides the pathname-derived active check — used for a parent item that should stay highlighted while any of its children is the active route. */
  activeOverride?: boolean
  /** Hides the visible label (icon-only rail); the label still reaches assistive tech via `title`. */
  hideLabel?: boolean
}

export function NavItem({ to, icon: Icon, label, pathname, activeOverride, hideLabel }: NavItemProps) {
  const active = activeOverride ?? (pathname === to || pathname.startsWith(`${to}/`))

  return (
    <Link
      to={to}
      title={label}
      className={`flex items-center gap-3 rounded-[10px] px-3 py-2.5 text-sm transition-colors ${
        active
          ? 'bg-accent-dim font-semibold text-status-green'
          : 'font-medium text-fg-1 hover:bg-bg-3 hover:text-fg-0'
      }`}
    >
      <Icon size={19} className={`shrink-0 ${active ? 'text-accent' : 'text-fg-2'}`} />
      {!hideLabel && <span className="min-w-0 flex-1 truncate">{label}</span>}
    </Link>
  )
}

/**
 * The desktop shell chrome — always visible on md+ screens, replaced by
 * MobileBottomNav below that breakpoint (the mockup has no mobile drawer:
 * the sidebar is either the full rail or entirely absent, never an overlay).
 */
export function Sidebar() {
  const { t } = useTranslation('shell')
  const user = useAuth((s) => s.user)
  const { pathname } = useLocation()
  const { spaceId } = useCurrentSpaceId()
  const collapsed = sidebarCollapseStore((s) => s.collapsed)
  const toggleCollapsed = sidebarCollapseStore((s) => s.toggle)

  const visibleItems = useMemo(
    () => NAV_CONFIG.filter((item) => !item.adminOnly || isAdminRole(user?.role)),
    [user?.role]
  )

  return (
    <aside
      className={`hidden shrink-0 flex-col border-r border-border bg-bg-2 transition-[width] duration-200 ease-in-out md:sticky md:top-0 md:flex md:h-screen ${collapsed ? 'md:w-[74px]' : 'md:w-[236px]'}`}
    >
      {/* Brand */}
      <Link to={ROUTES.ACCOUNT} className="flex shrink-0 items-center gap-[11px] px-[18px] pb-4 pt-5">
        <div
          className="grid size-[34px] shrink-0 place-items-center rounded-[10px] text-white"
          style={{ background: BRAND_LOGO_GRADIENT }}
        >
          <NidoMark size={19} />
        </div>
        {!collapsed && (
          <span className="text-xl font-bold tracking-tight text-fg-0" style={{ fontFamily: 'var(--font-family-display)' }}>
            {t('brand')}
          </span>
        )}
      </Link>

      {/* Nav */}
      <nav className="flex flex-1 flex-col gap-[3px] overflow-y-auto px-3 py-2" aria-label={t('nav.label')}>
        <NavList items={visibleItems} spaceId={spaceId} pathname={pathname} collapsed={collapsed} />
      </nav>

      {/* Collapse toggle */}
      <div className="border-t border-border p-3">
        <button
          type="button"
          onClick={toggleCollapsed}
          aria-label={t(collapsed ? 'nav.expand' : 'nav.collapse')}
          className="flex w-full items-center gap-[11px] rounded-[9px] px-[11px] py-[9px] text-[13.5px] text-fg-3 hover:bg-bg-3"
        >
          <ChevronLeft size={19} className={`shrink-0 text-fg-2 transition-transform ${collapsed ? 'rotate-180' : ''}`} />
          {!collapsed && <span>{t('nav.collapse')}</span>}
        </button>
      </div>
    </aside>
  )
}
