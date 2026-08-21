import { useEffect, useMemo, useRef } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { LucideIcon } from 'lucide-react'
import { useAuth } from '@/features/auth'
import { useFocusTrap } from '@/shared/lib'
import { NidoMark } from '@/shared/ui'
import { ROUTES } from '@/shared/config'
import { isAdminRole } from '@/entities/user'
import { NAV_CONFIG } from './navConfig'

const BRAND_LOGO_GRADIENT = 'linear-gradient(135deg, var(--brand-icon-from), var(--brand-icon-to))'

interface NavItemProps {
  to: string
  icon: LucideIcon
  label: string
  pathname: string
}

function NavItem({ to, icon: Icon, label, pathname }: NavItemProps) {
  const active = pathname === to || (to !== ROUTES.ACCOUNT && pathname.startsWith(to))

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

export interface SidebarProps {
  open: boolean
  onClose: () => void
}

export function Sidebar({ open, onClose }: SidebarProps) {
  const { t } = useTranslation('shell')
  const user = useAuth((s) => s.user)
  const { pathname } = useLocation()
  const sidebarRef = useRef<HTMLElement>(null)

  const prevPathname = useRef(pathname)
  useEffect(() => {
    if (prevPathname.current === pathname) return
    prevPathname.current = pathname
    onClose()
  }, [pathname, onClose])

  useFocusTrap(sidebarRef, open)

  const visibleItems = useMemo(
    () => NAV_CONFIG.filter((item) => !item.adminOnly || isAdminRole(user?.role)),
    [user?.role]
  )

  return (
    <aside
      ref={sidebarRef}
      {...(open ? { role: 'dialog', 'aria-modal': true, 'aria-label': t('sidebar_label') } : {})}
      className={`fixed inset-y-0 left-0 z-50 flex w-64 shrink-0 flex-col overflow-hidden border-r border-border bg-bg-2 transition-transform duration-200 ease-in-out md:static md:inset-auto md:z-auto md:w-[236px] md:translate-x-0 ${open ? 'translate-x-0' : '-translate-x-full'}`}
    >
      {/* Brand */}
      <Link
        to={ROUTES.ACCOUNT}
        className="flex shrink-0 items-center gap-[11px] px-[18px] pb-4 pt-5"
      >
        <div
          className="grid size-[34px] shrink-0 place-items-center rounded-[10px] text-white"
          style={{ background: BRAND_LOGO_GRADIENT }}
        >
          <NidoMark size={19} />
        </div>
        <span
          className="text-xl font-bold tracking-tight text-fg-0"
          style={{ fontFamily: 'var(--font-family-display)' }}
        >
          {t('brand')}
        </span>
      </Link>

      {/* Nav */}
      <nav
        className="flex flex-1 flex-col gap-[3px] overflow-y-auto px-3 py-2"
        aria-label={t('nav.label')}
      >
        {visibleItems.map((item) => (
          <NavItem key={item.to} to={item.to} icon={item.icon} label={t(item.labelKey)} pathname={pathname} />
        ))}
      </nav>
    </aside>
  )
}
