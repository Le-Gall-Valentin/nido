import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { LogOut, Search, Settings } from 'lucide-react'
import { useAuth } from '@/features/auth'
import { useShallow } from 'zustand/react/shallow'
import { UserAvatar } from '@/entities/user'
import { SpaceSwitcher } from '@/features/space-switcher'
import { ROUTES } from '@/shared/config'

interface TopbarProps {
  onSearchOpen: () => void
}

export function Topbar({ onSearchOpen }: TopbarProps) {
  const { t } = useTranslation('shell')
  const { user, logout } = useAuth(useShallow((s) => ({ user: s.user, logout: s.logout })))
  const [menuOpen, setMenuOpen] = useState(false)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState(false)
  const pendingRef = useRef(false)

  const closeMenu = useCallback(() => setMenuOpen(false), [])

  useEffect(() => {
    if (!menuOpen) return
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') closeMenu()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [menuOpen, closeMenu])

  async function handleLogout(): Promise<void> {
    if (pendingRef.current) return
    pendingRef.current = true
    setIsLoggingOut(true)
    setLogoutError(false)
    try {
      await logout()
    } catch {
      setLogoutError(true)
    } finally {
      pendingRef.current = false
      setIsLoggingOut(false)
    }
  }

  return (
    <header className="sticky top-0 z-30 flex h-[68px] shrink-0 items-center gap-4 border-b border-border bg-bg-2/85 px-4 backdrop-blur-md md:px-6">
      <SpaceSwitcher />

      {/* Search trigger — pill on sm+, icon only on mobile */}
      <div className="hidden max-w-[420px] flex-1 sm:block">
        <button
          type="button"
          onClick={onSearchOpen}
          className="flex w-full items-center gap-[9px] rounded-[11px] bg-bg-3 px-[13px] py-[9px] text-left text-fg-3 transition-colors hover:text-fg-2"
          aria-label={t('topbar.search_label')}
        >
          <Search size={17} className="shrink-0" />
          <span className="flex-1 truncate text-sm">{t('topbar.search_placeholder')}</span>
          <kbd className="rounded-[6px] border border-border bg-bg-1 px-[7px] py-0.5 text-[11px] font-semibold text-fg-3">
            /
          </kbd>
        </button>
      </div>
      <button
        type="button"
        onClick={onSearchOpen}
        className="grid size-10 place-items-center rounded-[11px] border border-border bg-bg-1 text-fg-2 transition-colors hover:text-fg-0 sm:hidden"
        aria-label={t('topbar.search_label')}
      >
        <Search size={18} />
      </button>

      {/* Profile menu */}
      {user && (
        <div className="relative ml-auto shrink-0">
          <button
            type="button"
            onClick={() => setMenuOpen((v) => !v)}
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            aria-label={t('topbar.profile_label')}
            className="flex items-center rounded-full border border-border bg-bg-1 p-1"
          >
            <UserAvatar username={user.username} role={user.role} className="size-8 rounded-full text-[13px]" />
          </button>

          {menuOpen && (
            <>
              <button
                type="button"
                aria-label={t('topbar.close_menu_label')}
                onClick={closeMenu}
                className="fixed inset-0 z-40 cursor-default bg-transparent"
              />
              <div className="absolute right-0 top-[52px] z-50 w-[250px] rounded-[15px] border border-border bg-bg-1 p-2 shadow-[0_12px_40px_rgba(44,42,38,0.14)]">
                <div className="flex items-center gap-[11px] px-2.5 pb-3 pt-2">
                  <UserAvatar username={user.username} role={user.role} className="size-10 rounded-full text-[15px]" />
                  <div className="min-w-0">
                    <div className="truncate text-sm font-semibold text-fg-0">{user.username}</div>
                    <div className="truncate text-xs text-fg-3">{user.email}</div>
                  </div>
                </div>
                <div className="mx-1.5 mb-1.5 h-px bg-border" />
                <Link
                  to={ROUTES.ACCOUNT}
                  onClick={closeMenu}
                  className="flex w-full items-center gap-[11px] rounded-[9px] p-2.5 text-sm font-medium text-fg-1 transition-colors hover:bg-bg-2 hover:text-fg-0"
                >
                  <Settings size={18} className="text-fg-2" />
                  {t('menu.settings')}
                </Link>
                <button
                  type="button"
                  onClick={() => { void handleLogout() }}
                  disabled={isLoggingOut}
                  className="flex w-full items-center gap-[11px] rounded-[9px] p-2.5 text-left text-sm font-semibold text-status-red transition-colors hover:bg-status-red-dim disabled:opacity-50"
                >
                  <LogOut size={18} />
                  {t('menu.logout')}
                </button>
                {logoutError && (
                  <p role="alert" className="px-2.5 pb-1 pt-1.5 text-xs text-status-red">
                    {t('menu.logout_error')}
                  </p>
                )}
              </div>
            </>
          )}
        </div>
      )}
    </header>
  )
}
