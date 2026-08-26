import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Check, ChevronDown, UserPlus } from 'lucide-react'
import { SpaceAvatar, isPersonal, type SpaceSummary, type SpaceRole } from '@/entities/space'
import { ROUTES } from '@/shared/config'
import { useMySpaces } from '../model/useMySpaces'
import { useActiveSpace } from '../model/useActiveSpace'
import { activeSpaceStore } from '../model/activeSpaceStore'

// The personal space must always appear first in the list, regardless of
// the order the API returns.
function sortSpaces(spaces: SpaceSummary[]): SpaceSummary[] {
  return [...spaces].sort((a, b) => Number(isPersonal(b)) - Number(isPersonal(a)))
}

export function SpaceSwitcher() {
  const { t } = useTranslation('space')
  const navigate = useNavigate()
  const { spaceId } = useActiveSpace()
  const { data: spaces } = useMySpaces()
  const [open, setOpen] = useState(false)

  const closeMenu = useCallback(() => setOpen(false), [])

  useEffect(() => {
    if (!open) return
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') closeMenu()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [open, closeMenu])

  const ordered = useMemo(() => sortSpaces(spaces ?? []), [spaces])
  // The URL does not always carry a spaceId (e.g. /account): in that case
  // the trigger button falls back to the personal space to stay
  // informative, but the checkmark in the panel only lights up on a strict
  // match with the URL — never on this fallback.
  const current = ordered.find((s) => s.id === spaceId) ?? ordered.find((s) => isPersonal(s))

  function roleLabel(role: SpaceRole): string {
    return t(`role.${role}`)
  }

  function subtitleFor(space: SpaceSummary): string {
    return isPersonal(space) ? t('switcher.personal_subtitle') : roleLabel(space.myRole)
  }

  function choose(space: SpaceSummary): void {
    activeSpaceStore.getState().remember(space.id)
    navigate(ROUTES.space(space.id))
    closeMenu()
  }

  function createOrJoin(): void {
    navigate(ROUTES.SPACES)
    closeMenu()
  }

  if (!current) return null

  return (
    <div className="relative shrink-0">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={t('switcher.trigger_label', { name: current.name })}
        className="flex items-center gap-2.5 rounded-[11px] border border-border bg-bg-1 px-2 py-1.5 transition-colors hover:bg-bg-2"
      >
        <SpaceAvatar space={current} size="sm" />
        <span className="flex flex-col items-start leading-tight">
          <span className="text-[10px] font-bold uppercase tracking-[0.08em] text-fg-3">
            {t('switcher.kicker')}
          </span>
          <span className="max-w-[160px] truncate text-sm font-semibold text-fg-0">{current.name}</span>
        </span>
        <ChevronDown size={16} className="text-fg-3" aria-hidden="true" />
      </button>

      {open && (
        <>
          <button
            type="button"
            aria-label={t('switcher.close_label')}
            onClick={closeMenu}
            className="fixed inset-0 z-40 cursor-default bg-transparent"
          />
          <div className="absolute left-0 top-[52px] z-50 w-[280px] rounded-[15px] border border-border bg-bg-1 p-2 shadow-[0_12px_40px_rgba(44,42,38,0.14)]">
            <div className="px-2.5 pb-2 pt-1 text-xs font-semibold uppercase tracking-[0.04em] text-fg-3">
              {t('switcher.title')}
            </div>
            <ul role="menu">
              {ordered.map((space) => (
                <li key={space.id} role="none">
                  <button
                    type="button"
                    role="menuitem"
                    onClick={() => choose(space)}
                    className="flex w-full items-center gap-[11px] rounded-[9px] p-2.5 text-left transition-colors hover:bg-bg-2"
                  >
                    <SpaceAvatar space={space} size="sm" />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-medium text-fg-0">{space.name}</span>
                      <span className="block truncate text-xs text-fg-3">{subtitleFor(space)}</span>
                    </span>
                    {space.id === spaceId && (
                      <Check size={16} className="shrink-0 text-accent" aria-hidden="true" />
                    )}
                  </button>
                </li>
              ))}
            </ul>
            <div className="mx-1.5 my-1.5 h-px bg-border" />
            <button
              type="button"
              onClick={createOrJoin}
              className="flex w-full items-center gap-[11px] rounded-[9px] p-2.5 text-left text-sm font-medium text-fg-1 transition-colors hover:bg-bg-2 hover:text-fg-0"
            >
              <UserPlus size={18} className="text-fg-2" aria-hidden="true" />
              {t('switcher.create_action')}
            </button>
          </div>
        </>
      )}
    </div>
  )
}
