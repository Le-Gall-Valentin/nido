import { Suspense, useEffect, useMemo, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/features/auth'
import { useCurrentSpaceId } from '@/features/space-switcher'
import { usePaletteItems } from '@/shared/lib'
import { isAdminRole } from '@/entities/user'
import { Sidebar } from './Sidebar'
import { MobileNavDrawer } from './MobileNavDrawer'
import { GroupAccentStrip } from './GroupAccentStrip'
import { Topbar } from './Topbar'
import { CommandPalette } from './CommandPalette'
import { NAV_CONFIG } from './navConfig'
import { PaletteSetups } from './paletteSetups'

function PageLoader() {
  const { t } = useTranslation('shell')
  return (
    <div
      className="flex h-32 items-center justify-center gap-3 text-xs text-fg-2"
      role="status"
      aria-label={t('loader')}
    >
      <div className="size-4 animate-spin rounded-full border-2 border-border-2 border-t-accent" aria-hidden="true" />
      {t('loader')}
    </div>
  )
}

export function AppLayout() {
  const { t } = useTranslation('shell')
  const user = useAuth((s) => s.user)
  const { spaceId } = useCurrentSpaceId()
  const { pathname } = useLocation()

  const visibleNavItems = useMemo(
    () => NAV_CONFIG.filter((item) => !item.adminOnly || isAdminRole(user?.role)),
    [user?.role]
  )

  const paletteItems = useMemo(
    () => visibleNavItems
      .map((item) => ({ id: item.id, label: t(item.labelKey), to: item.to(spaceId), icon: item.icon, group: t('palette.type_page') }))
      .filter((item) => !!item.to),
    [t, visibleNavItems, spaceId]
  )

  usePaletteItems('shell', paletteItems)

  const [paletteOpen, setPaletteOpen] = useState(false)
  const openPalette = () => setPaletteOpen(true)
  const closePalette = () => setPaletteOpen(false)

  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  const openMobileNav = () => setMobileNavOpen(true)
  const closeMobileNav = () => setMobileNavOpen(false)

  // A route change (tapping a nav link, or any other navigation) always
  // means the drawer's job is done — close it so it never lingers open
  // over the newly-loaded page.
  useEffect(() => {
    setMobileNavOpen(false)
  }, [pathname])

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        setPaletteOpen((prev) => !prev)
      } else if (
        e.key === '/' &&
        document.activeElement?.tagName !== 'INPUT' &&
        document.activeElement?.tagName !== 'TEXTAREA' &&
        !(document.activeElement instanceof HTMLElement && document.activeElement.isContentEditable)
      ) {
        e.preventDefault()
        setPaletteOpen(true)
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [])

  return (
    <div className="flex h-screen overflow-hidden bg-bg-0">
      <PaletteSetups />

      <Sidebar />

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <Topbar onSearchOpen={openPalette} onMenuOpen={openMobileNav} />
        <GroupAccentStrip />
        <main className="flex-1 overflow-y-auto">
          <Suspense fallback={<PageLoader />}>
            <Outlet />
          </Suspense>
        </main>
      </div>

      {mobileNavOpen && (
        <MobileNavDrawer items={visibleNavItems} spaceId={spaceId} pathname={pathname} onClose={closeMobileNav} />
      )}

      {paletteOpen && <CommandPalette onClose={closePalette} />}
    </div>
  )
}