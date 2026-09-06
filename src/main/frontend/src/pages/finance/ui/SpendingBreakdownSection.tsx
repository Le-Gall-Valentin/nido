import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { Category, CategoryAmount } from '@/entities/finance'
import { formatAmount } from '../lib/formatAmount'

/** A hand-drawn SVG donut (no charting library in this app) with a colored ring segment per category. */
function BreakdownDonut({ breakdown, categoryById }: { breakdown: CategoryAmount[]; categoryById: Map<string, Category> }) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [hovered, setHovered] = useState<{ categoryId: string; x: number; y: number } | null>(null)
  const total = breakdown.reduce((sum, row) => sum + row.amount, 0)
  const radius = 52
  const circumference = 2 * Math.PI * radius
  let offset = 0

  function handlePointerMove(categoryId: string, e: React.MouseEvent) {
    const rect = containerRef.current?.getBoundingClientRect()
    if (!rect) return
    setHovered({ categoryId, x: e.clientX - rect.left, y: e.clientY - rect.top })
  }

  const hoveredRow = hovered ? breakdown.find((row) => row.categoryId === hovered.categoryId) : null
  const hoveredCategory = hoveredRow ? categoryById.get(hoveredRow.categoryId) : null
  const hoveredPercent = hoveredRow && total > 0 ? Math.round((hoveredRow.amount / total) * 100) : 0

  return (
    <div ref={containerRef} className="relative shrink-0">
      <svg width={128} height={128} viewBox="0 0 128 128" className="-rotate-90">
        <circle cx={64} cy={64} r={radius} fill="none" stroke="var(--color-bg-3)" strokeWidth={20} />
        {total > 0 && breakdown.map((row) => {
          const category = categoryById.get(row.categoryId)
          const dash = (row.amount / total) * circumference
          const segmentOffset = offset
          offset += dash
          const percent = Math.round((row.amount / total) * 100)
          return (
            <circle key={row.categoryId} cx={64} cy={64} r={radius} fill="none"
              stroke={category?.color ?? 'var(--color-fg-3)'} strokeWidth={20}
              strokeDasharray={`${dash} ${circumference - dash}`} strokeDashoffset={-segmentOffset}
              role="img" aria-label={`${category?.label ?? row.categoryId}: ${percent}%, ${formatAmount(row.amount)}`}
              onMouseEnter={(e) => handlePointerMove(row.categoryId, e)}
              onMouseMove={(e) => handlePointerMove(row.categoryId, e)}
              onMouseLeave={() => setHovered(null)} />
          )
        })}
      </svg>
      {hovered && hoveredRow && (
        <div role="tooltip"
          className="pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-[calc(100%+10px)] whitespace-nowrap rounded-[8px] border border-border bg-bg-1 px-2.5 py-1.5 text-xs shadow-[0_4px_16px_rgba(44,42,38,0.16)]"
          style={{ left: hovered.x, top: hovered.y }}>
          <p className="flex items-center gap-1.5 font-medium text-fg-0">
            <span className="size-2 shrink-0 rounded-[2px]" style={{ backgroundColor: hoveredCategory?.color }} />
            {hoveredCategory?.label ?? hoveredRow.categoryId}
          </p>
          <p className="text-fg-3">{hoveredPercent}% · {formatAmount(hoveredRow.amount)}</p>
        </div>
      )}
    </div>
  )
}

interface SpendingBreakdownSectionProps {
  breakdown: CategoryAmount[]
  categoryById: Map<string, Category>
  onSelectCategory: (categoryId: string) => void
}

export function SpendingBreakdownSection({ breakdown, categoryById, onSelectCategory }: SpendingBreakdownSectionProps) {
  const { t } = useTranslation('finance')
  const total = breakdown.reduce((sum, row) => sum + row.amount, 0)

  return (
    <section className="flex flex-col rounded-2xl border border-border bg-bg-1 p-4">
      <h2 className="mb-3 text-[15px] font-semibold text-fg-0">{t('breakdown.title')}</h2>
      <div className="flex flex-1 items-center justify-center">
        {breakdown.length === 0 ? (
          <p className="text-sm text-fg-3">{t('breakdown.empty')}</p>
        ) : (
          <div className="flex w-full flex-col items-center gap-4 sm:flex-row sm:gap-6">
            <BreakdownDonut breakdown={breakdown} categoryById={categoryById} />
            <ul className="w-full space-y-2 sm:w-auto sm:flex-1">
              {breakdown.map((row) => {
                const category = categoryById.get(row.categoryId)
                const percent = total > 0 ? Math.round((row.amount / total) * 100) : 0
                return (
                  <li key={row.categoryId}>
                    <button type="button" onClick={() => onSelectCategory(row.categoryId)}
                      className="flex w-full items-center gap-2 rounded-lg text-left text-sm transition-colors hover:bg-bg-2">
                      <span className="size-2.5 shrink-0 rounded-[3px]" style={{ backgroundColor: category?.color }} />
                      <span className="flex-1 truncate text-fg-1">{category?.label ?? row.categoryId}</span>
                      <span className="text-fg-3">{percent}%</span>
                      <span className="w-20 text-right font-medium text-fg-0">{formatAmount(row.amount)}</span>
                    </button>
                  </li>
                )
              })}
            </ul>
          </div>
        )}
      </div>
    </section>
  )
}
