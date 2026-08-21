import { useCallback, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Search } from 'lucide-react'
import { usePaletteResults, useFocusTrap } from '@/shared/lib'

interface CommandPaletteProps {
  onClose: () => void
}

export function CommandPalette({ onClose }: CommandPaletteProps) {
  const { t } = useTranslation('shell')
  const nav = useNavigate()
  const dialogRef = useRef<HTMLDivElement>(null)
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState(0)

  const allItems = usePaletteResults()

  const items = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return allItems
    return allItems.filter((item) => item.label.toLowerCase().includes(q))
  }, [query, allItems])

  // Clamp selected to valid range without a separate useEffect render
  const safeSelected = items.length > 0 ? Math.min(selected, items.length - 1) : 0

  useFocusTrap(dialogRef, true)

  const go = useCallback(
    (item: { to?: string; action?: () => void }) => {
      if (item.action) item.action()
      else if (item.to) nav(item.to)
      onClose()
    },
    [nav, onClose]
  )

  const onKey = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Escape') { onClose(); return }
      if (e.key === 'ArrowDown') { e.preventDefault(); setSelected((s) => Math.min(items.length - 1, s + 1)) }
      if (e.key === 'ArrowUp') { e.preventDefault(); setSelected((s) => Math.max(0, s - 1)) }
      if (e.key === 'Enter') { e.preventDefault(); const item = items[safeSelected]; if (item) go(item) }
    },
    [items, safeSelected, go, onClose]
  )

  return (
    <div
      className="fixed inset-0 z-[200] flex justify-center bg-[rgba(44,42,38,0.32)] px-5 pt-[12vh]"
      onClick={onClose}
    >
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-label={t('palette_dialog_label')}
        className="h-fit w-full max-w-[560px] overflow-hidden rounded-2xl bg-bg-1 shadow-[0_24px_70px_rgba(44,42,38,0.28)]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Input */}
        <div className="flex items-center gap-3 border-b border-bg-3 px-5 py-4">
          <Search size={20} className="shrink-0 text-fg-3" />
          <input
            autoFocus
            className="flex-1 bg-transparent text-base text-fg-0 placeholder:text-fg-3 outline-none"
            placeholder={t('palette.placeholder')}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={onKey}
          />
          <kbd className="rounded-[6px] bg-bg-3 px-2 py-[3px] text-[11px] font-semibold text-fg-3">
            {t('palette.escape_hint')}
          </kbd>
        </div>

        {/* Results */}
        <div className="max-h-[52vh] overflow-y-auto p-2">
          {items.length === 0 && (
            <div className="py-8 text-center text-[13.5px] text-fg-3">
              {t('palette.no_results', { query })}
            </div>
          )}
          {items.map((item, i) => {
            const Icon = item.icon
            return (
              <button
                key={item.id}
                type="button"
                className={`flex w-full items-center gap-[13px] rounded-[10px] px-3 py-[11px] text-left transition-colors ${
                  i === safeSelected ? 'bg-bg-3' : 'hover:bg-bg-2'
                }`}
                onMouseEnter={() => setSelected(i)}
                onClick={() => go(item)}
              >
                <Icon size={16} className="shrink-0 text-fg-3" />
                <span className="min-w-0 flex-1 truncate text-[14.5px] font-semibold text-fg-0">{item.label}</span>
                {item.group && (
                  <span className="shrink-0 rounded-[6px] bg-bg-3 px-[9px] py-1 text-[11px] font-bold text-fg-2">
                    {item.group}
                  </span>
                )}
              </button>
            )
          })}
        </div>

        {/* Footer */}
        <div className="flex items-center gap-3.5 border-t border-bg-3 bg-bg-2 px-4 py-2 text-[11px] text-fg-2">
          <span className="flex items-center gap-1.5">
            <kbd className="rounded-[5px] bg-bg-3 px-1.5 py-px font-semibold">↑</kbd>
            <kbd className="rounded-[5px] bg-bg-3 px-1.5 py-px font-semibold">↓</kbd>
            {t('palette.navigate_hint')}
          </span>
          <span className="flex items-center gap-1.5">
            <kbd className="rounded-[5px] bg-bg-3 px-1.5 py-px font-semibold">↵</kbd>
            {t('palette.open_hint')}
          </span>
          <span className="ml-auto text-fg-3">
            {t('palette.results', { count: items.length })}
          </span>
        </div>
      </div>
    </div>
  )
}