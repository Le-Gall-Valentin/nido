import { Suspense, useCallback, useEffect, useMemo, useState } from 'react'
import { Outlet } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/features/auth'
import { usePaletteItems } from '@/shared/lib'
import { isAdminRole } from '@/entities/user'
import { Sidebar } from './Sidebar'
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

  const paletteItems = useMemo(
    () => {
      const pageGroup = t('palette.type_page')
      return [
        ...NAV_CONFIG
          .filter((item) => !item.adminOnly || isAdminRole(user?.role))
          .map((item) => ({ id: item.id, label: t(item.labelKey), to: item.to, icon: item.icon, group: pageGroup })),
      ]
    },
    [t, user?.role]
  )

  usePaletteItems('shell', paletteItems)

  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [paletteOpen, setPaletteOpen] = useState(false)

  const openSidebar = useCallback(() => setSidebarOpen(true), [])
  const closeSidebar = useCallback(() => setSidebarOpen(false), [])
  const openPalette = useCallback(() => setPaletteOpen(true), [])
  const closePalette = useCallback(() => setPaletteOpen(false), [])

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
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 md:hidden"
          aria-hidden="true"
          onClick={closeSidebar}
        />
      )}

      <Sidebar open={sidebarOpen} onClose={closeSidebar} />

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <Topbar onMenuOpen={openSidebar} onSearchOpen={openPalette} />
        <main className="flex-1 overflow-y-auto">
          <Suspense fallback={<PageLoader />}>
            <Outlet />
          </Suspense>
        </main>
      </div>

      {paletteOpen && <CommandPalette onClose={closePalette} />}
    </div>
  )
}