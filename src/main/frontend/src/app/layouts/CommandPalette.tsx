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
      className="fixed inset-0 z-[200] flex justify-center bg-black/55 pt-[10vh] backdrop-blur-[3px]"
      onClick={onClose}
    >
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-label={t('palette_dialog_label')}
        className="h-fit w-full max-w-[560px] overflow-hidden rounded-xl border border-border-2 bg-bg-1 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Input */}
        <div className="flex items-center gap-2.5 border-b border-border px-3.5 py-3">
          <Search size={14} className="shrink-0 text-fg-3" />
          <input
            autoFocus
            className="flex-1 bg-transparent text-sm text-fg-0 placeholder:text-fg-3 outline-none"
            placeholder={t('palette.placeholder')}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={onKey}
          />
          <kbd className="rounded border border-border-2 bg-bg-2 px-1 py-0.5 font-mono text-[10px] text-fg-2">
            {t('palette.escape_hint')}
          </kbd>
        </div>

        {/* Results */}
        <div className="max-h-[50vh] overflow-y-auto p-1.5">
          {items.length === 0 && (
            <div className="py-10 text-center text-xs text-fg-2">
              {t('palette.no_results', { query })}
            </div>
          )}
          {items.map((item, i) => {
            const Icon = item.icon
            return (
              <button
                key={item.id}
                type="button"
                className={`flex w-full items-center gap-3 rounded-md px-2.5 py-2 text-left text-[13px] transition-colors ${
                  i === safeSelected ? 'bg-bg-3' : 'hover:bg-bg-2'
                }`}
                onMouseEnter={() => setSelected(i)}
                onClick={() => go(item)}
              >
                <Icon size={14} className="shrink-0 text-fg-3" />
                <span className="min-w-0 flex-1 truncate font-medium text-fg-0">{item.label}</span>
                {item.group && (
                  <span className="shrink-0 rounded border border-border-2 bg-bg-3 px-1.5 py-0.5 font-mono text-[10px] text-fg-1">
                    {item.group}
                  </span>
                )}
              </button>
            )
          })}
        </div>

        {/* Footer */}
        <div className="flex items-center gap-3.5 border-t border-border bg-bg-2 px-3.5 py-2 text-[11px] text-fg-2">
          <span className="flex items-center gap-1.5">
            <kbd className="rounded border border-border-2 bg-bg-1 px-1 font-mono">↑</kbd>
            <kbd className="rounded border border-border-2 bg-bg-1 px-1 font-mono">↓</kbd>
            {t('palette.navigate_hint')}
          </span>
          <span className="flex items-center gap-1.5">
            <kbd className="rounded border border-border-2 bg-bg-1 px-1 font-mono">↵</kbd>
            {t('palette.open_hint')}
          </span>
          <span className="ml-auto font-mono text-fg-3">
            {t('palette.results', { count: items.length })}
          </span>
        </div>
      </div>
    </div>
  )
}