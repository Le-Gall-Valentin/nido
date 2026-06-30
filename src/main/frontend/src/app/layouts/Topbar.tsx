import { Fragment, useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Menu, RefreshCcw, Search } from 'lucide-react'
import { useBreadcrumbs } from './useBreadcrumbs'

interface TopbarProps {
  onMenuOpen: () => void
  onSearchOpen: () => void
}

export function Topbar({ onMenuOpen, onSearchOpen }: TopbarProps) {
  const { t } = useTranslation('shell')
  const crumbs = useBreadcrumbs()
  const [refreshing, setRefreshing] = useState(false)

  useEffect(() => {
    if (!refreshing) return
    const id = setTimeout(() => setRefreshing(false), 700)
    return () => clearTimeout(id)
  }, [refreshing])

  const handleRefresh = useCallback(() => setRefreshing(true), [])

  return (
    <header className="sticky top-0 z-10 flex h-12 shrink-0 items-center gap-3 border-b border-border bg-bg-1 px-4">
      {/* Mobile hamburger */}
      <button
        type="button"
        className="grid size-7 place-items-center rounded text-fg-2 transition-colors hover:bg-bg-2 hover:text-fg-0 md:hidden"
        onClick={onMenuOpen}
        aria-label={t('topbar.menu_label')}
      >
        <Menu size={18} />
      </button>

      {/* Breadcrumbs */}
      <nav
        aria-label={t('topbar.breadcrumb_nav')}
        className="flex min-w-0 flex-1 items-center gap-2 overflow-hidden whitespace-nowrap text-xs"
      >
        {crumbs.map((crumb, i) => (
          <Fragment key={i}>
            {i > 0 && <span className="shrink-0 text-fg-3">/</span>}
            {crumb.to && i < crumbs.length - 1 ? (
              <Link
                to={crumb.to}
                className="max-w-[240px] truncate rounded px-1.5 py-0.5 text-fg-2 transition-colors hover:bg-bg-2 hover:text-fg-0"
              >
                {crumb.label}
              </Link>
            ) : (
              <span className="max-w-[380px] truncate px-1.5 py-0.5 font-medium text-fg-0">
                {crumb.label}
              </span>
            )}
          </Fragment>
        ))}
      </nav>

      {/* Actions */}
      <div className="ml-auto flex shrink-0 items-center gap-2.5">
        {/* Search trigger — full on sm+, icon only on mobile */}
        <button
          type="button"
          onClick={onSearchOpen}
          className="hidden min-w-[200px] items-center gap-2 rounded border border-border bg-bg-2 px-2.5 py-1.5 text-xs text-fg-2 transition-colors hover:border-border-2 hover:text-fg-1 sm:flex"
          aria-label={t('topbar.search_label')}
        >
          <Search size={13} className="shrink-0 text-fg-3" />
          <span className="flex-1 text-left">{t('topbar.search_placeholder')}</span>
          <kbd className="rounded border border-border-2 bg-bg-2 px-1 py-0.5 font-mono text-[10px] text-fg-2">
            ⌘K
          </kbd>
        </button>
        <button
          type="button"
          onClick={onSearchOpen}
          className="grid size-7 place-items-center rounded text-fg-2 transition-colors hover:bg-bg-2 hover:text-fg-0 sm:hidden"
          aria-label={t('topbar.search_label')}
        >
          <Search size={16} />
        </button>

        {/* Refresh */}
        <button
          type="button"
          onClick={handleRefresh}
          className="grid size-7 place-items-center rounded border border-transparent text-fg-2 transition-colors hover:bg-bg-3 hover:text-fg-0"
          aria-label={t('topbar.refresh_label')}
        >
          <RefreshCcw size={14} className={refreshing ? 'animate-spin' : ''} />
        </button>
      </div>
    </header>
  )
}