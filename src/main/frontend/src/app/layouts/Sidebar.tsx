import { useEffect, useMemo, useRef } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ChevronRight } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { useAuth } from '@/features/auth'
import { useFocusTrap } from '@/shared/lib'
import { ROUTES } from '@/shared/config'
import { isAdminRole, getInitials } from '@/entities/user'
import type { UserRole } from '@/entities/user'
import { NAV_CONFIG } from './navConfig'

const SUPER_ADMIN_GRADIENT = 'linear-gradient(135deg, #a78bfa, #818cf8)'
const USER_GRADIENT = 'linear-gradient(135deg, var(--color-accent), #38bdf8)'

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
      className={`relative mx-2 flex items-center gap-2.5 rounded px-2.5 py-[7px] text-[13px] transition-colors ${
        active ? 'bg-bg-3 text-fg-0' : 'text-fg-1 hover:bg-bg-2 hover:text-fg-0'
      }`}
    >
      {active && (
        <span className="absolute -left-2 bottom-1.5 top-1.5 w-0.5 rounded-sm bg-accent" />
      )}
      <Icon size={14} className={`shrink-0 ${active ? 'text-fg-1' : 'text-fg-2'}`} />
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

  const visibleSections = useMemo(() => {
    const map = new Map<string, typeof NAV_CONFIG>()
    for (const item of NAV_CONFIG) {
      if (item.adminOnly && !isAdminRole(user?.role)) continue
      const group = map.get(item.sectionKey) ?? []
      if (!map.has(item.sectionKey)) map.set(item.sectionKey, group)
      group.push(item)
    }
    return [...map.entries()].map(([titleKey, items]) => ({ titleKey, items }))
  }, [user?.role])

  const initials = user ? getInitials(user.username) : '??'
  const roleLabel = user ? t(`user.role.${user.role as UserRole}`) : ''
  const avatarGradient = user?.role === 'SUPER_ADMIN' ? SUPER_ADMIN_GRADIENT : USER_GRADIENT
  const accountActive = pathname.startsWith('/account')

  return (
    <aside
      ref={sidebarRef}
      {...(open ? { role: 'dialog', 'aria-modal': true, 'aria-label': t('sidebar_label') } : {})}
      className={`fixed inset-y-0 left-0 z-50 flex w-64 shrink-0 flex-col overflow-hidden border-r border-border bg-bg-1 transition-transform duration-200 ease-in-out md:static md:inset-auto md:z-auto md:w-[232px] md:translate-x-0 ${open ? 'translate-x-0' : '-translate-x-full'}`}
    >
      {/* Brand */}
      <Link
        to={ROUTES.ACCOUNT}
        className="flex shrink-0 items-center gap-2.5 border-b border-border px-4 py-[15px]"
      >
        <div
          className="grid size-[26px] shrink-0 place-items-center rounded-[7px] font-mono text-sm font-bold text-bg-0"
          style={{ background: USER_GRADIENT }}
        >
          S
        </div>
        <span className="text-sm font-semibold tracking-tight text-fg-0">{t('brand')}</span>
      </Link>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto py-1" aria-label={t('nav.label')}>
        {visibleSections.map((section) => (
          <div key={section.titleKey}>
            <div className="px-3 pb-1 pt-3.5 text-[10px] font-semibold uppercase tracking-[0.08em] text-fg-3">
              {t(section.titleKey)}
            </div>
            {section.items.map((item) => (
              <NavItem key={item.to} to={item.to} icon={item.icon} label={t(item.labelKey)} pathname={pathname} />
            ))}
          </div>
        ))}
      </nav>

      {/* User footer */}
      <Link
        to={ROUTES.ACCOUNT}
        className={`flex shrink-0 items-center gap-2.5 border-t border-border p-2.5 transition-colors ${
          accountActive ? 'bg-bg-3' : 'hover:bg-bg-2'
        }`}
      >
        <div
          className="grid size-7 shrink-0 place-items-center rounded-full text-[11px] font-semibold"
          style={{ background: avatarGradient, color: 'var(--color-bg-0)' }}
          aria-hidden="true"
        >
          {initials}
        </div>
        <div className="min-w-0 flex-1 leading-tight">
          <div className="truncate text-xs font-medium text-fg-0">{user?.username ?? '—'}</div>
          <div className="font-mono text-[10px] uppercase tracking-[0.08em] text-fg-3">
            {roleLabel}
          </div>
        </div>
        <ChevronRight size={12} className="shrink-0 text-fg-3" />
      </Link>
    </aside>
  )
}