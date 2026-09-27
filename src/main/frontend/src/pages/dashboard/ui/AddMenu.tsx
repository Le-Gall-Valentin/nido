import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { CalendarPlus, Plus, ShoppingCart, SquareCheck, Wallet, type LucideIcon } from 'lucide-react'
import { ROUTES } from '@/shared/config'
import { withCreateIntent } from '@/shared/lib'
import { CTA_BUTTON_SHADOW, CTA_BUTTON_STYLE } from '@/shared/ui'
import { useDashboardActions } from '../model/dashboardActions'

const ITEM = 'flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left text-sm font-medium text-fg-1 hover:bg-bg-2'

/**
 * "Ajouter": a task here, in the tasks widget's own form; an expense, a shopping item or an event on
 * the page that owns it, which opens its creation form from the link (see useCreateIntent). Moving
 * three forms out of their pages to open them here was the alternative, and the more costly one.
 */
export function AddMenu({ onAddTask }: { onAddTask: () => void }) {
  const { t } = useTranslation('dashboard')
  const { spaceId } = useDashboardActions()
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const onPointer = (event: MouseEvent) => {
      if (!ref.current?.contains(event.target as Node)) setOpen(false)
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onPointer)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onPointer)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  const links: { key: string; to: string; label: string; icon: LucideIcon }[] = [
    { key: 'transaction', to: withCreateIntent(ROUTES.spaceFinance(spaceId), 'transaction'), label: t('add_menu.transaction'), icon: Wallet },
    { key: 'item', to: withCreateIntent(ROUTES.spaceOrganisationCourses(spaceId), 'item'), label: t('add_menu.item'), icon: ShoppingCart },
    { key: 'event', to: withCreateIntent(ROUTES.spaceOrganisationCalendar(spaceId), 'event'), label: t('add_menu.event'), icon: CalendarPlus },
  ]

  return (
    <div ref={ref} className="relative shrink-0">
      <button type="button" aria-haspopup="menu" aria-expanded={open} aria-label={t('hero.add')} onClick={() => setOpen((value) => !value)}
        className="flex size-[42px] items-center justify-center gap-1.5 rounded-[10px] text-sm font-semibold sm:h-auto sm:w-auto sm:px-4 sm:py-2.5"
        style={{ ...CTA_BUTTON_STYLE, boxShadow: CTA_BUTTON_SHADOW }}>
        <Plus className="size-4" aria-hidden="true" /><span className="hidden sm:inline">{t('hero.add')}</span>
      </button>
      {open && (
        <div role="menu" className="absolute right-0 top-full z-20 mt-2 w-56 rounded-xl border border-border bg-bg-1 p-1 shadow-lg">
          <button type="button" role="menuitem" className={ITEM} onClick={() => { setOpen(false); onAddTask() }}>
            <SquareCheck className="size-4 text-fg-3" aria-hidden="true" />{t('add_menu.task')}
          </button>
          {links.map(({ key, to, label, icon: Icon }) => (
            <Link key={key} role="menuitem" to={to} className={ITEM} onClick={() => setOpen(false)}>
              <Icon className="size-4 text-fg-3" aria-hidden="true" />{label}
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
