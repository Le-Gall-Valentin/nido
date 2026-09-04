import { useEffect, useId, useRef } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { X } from 'lucide-react'
import { useFocusTrap } from '@/shared/lib'
import { NidoMark } from '@/shared/ui'
import { ROUTES } from '@/shared/config'
import { NavList } from './NavList'
import { BRAND_LOGO_GRADIENT } from './Sidebar'
import type { NavItemConfig } from './navConfig'

interface MobileNavDrawerProps {
  items: NavItemConfig[]
  spaceId: string | undefined
  pathname: string
  onClose: () => void
  hasPendingInvitations?: boolean
  openTaskCount?: number
}

/**
 * Replaces the sidebar entirely below the md breakpoint: a burger button in
 * the topbar opens this off-canvas panel over the full nav tree, every
 * sub-item always expanded (see NavList's alwaysExpanded) — unlike a bottom
 * tab bar, this scales to any number of top-level modules and never loses
 * access to sub-navigation.
 */
export function MobileNavDrawer({ items, spaceId, pathname, onClose, hasPendingInvitations, openTaskCount }: MobileNavDrawerProps) {
  const { t } = useTranslation('shell')
  const panelRef = useRef<HTMLDivElement>(null)
  const titleId = useId()
  useFocusTrap(panelRef, true)

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  return (
    <div className="fixed inset-0 z-[100] md:hidden" role="dialog" aria-modal="true" aria-labelledby={titleId}>
      <button
        type="button"
        aria-label={t('topbar.close_menu_label')}
        onClick={onClose}
        className="fixed inset-0 bg-[rgba(44,42,38,0.32)]"
      />
      <div
        ref={panelRef}
        className="fixed inset-y-0 left-0 flex w-[280px] max-w-[85vw] flex-col bg-bg-2 shadow-[0_0_40px_rgba(44,42,38,0.24)]"
      >
        <div className="flex shrink-0 items-center justify-between gap-2 px-[18px] pb-4 pt-5">
          <Link to={ROUTES.ACCOUNT} onClick={onClose} className="flex min-w-0 items-center gap-[11px]">
            <div
              className="grid size-[34px] shrink-0 place-items-center rounded-[10px] text-white"
              style={{ background: BRAND_LOGO_GRADIENT }}
            >
              <NidoMark size={19} />
            </div>
            <span id={titleId} className="truncate text-xl font-bold tracking-tight text-fg-0" style={{ fontFamily: 'var(--font-family-display)' }}>
              {t('brand')}
            </span>
          </Link>
          <button
            type="button"
            onClick={onClose}
            aria-label={t('topbar.close_menu_label')}
            className="grid size-9 shrink-0 place-items-center rounded-[9px] text-fg-2 hover:bg-bg-3"
          >
            <X size={20} />
          </button>
        </div>

        <nav
          className="flex flex-1 flex-col gap-[3px] overflow-y-auto px-3 pb-4"
          aria-label={t('nav.label')}
          onClick={onClose}
        >
          <NavList items={items} spaceId={spaceId} pathname={pathname} alwaysExpanded hasPendingInvitations={hasPendingInvitations} openTaskCount={openTaskCount} />
        </nav>
      </div>
    </div>
  )
}
