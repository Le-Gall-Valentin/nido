import { ChevronLeft, ChevronRight } from 'lucide-react'

interface PaginationProps {
  /** Zero-based current page. */
  page: number
  totalPages: number
  onPageChange: (page: number) => void
  /** Translated "Page X of Y" text. */
  pageLabel: string
  /** Translated accessible labels for the arrows. */
  prevLabel: string
  nextLabel: string
  /** Optional left-side summary (e.g. total count). */
  summary?: string
  /** Dims and disables the controls while a page transition is in flight. */
  isTransitioning?: boolean
}

const ARROW_CLASS =
  'size-7 flex items-center justify-center rounded-md border border-transparent text-fg-2 transition-colors ' +
  'disabled:opacity-40 disabled:cursor-not-allowed hover:enabled:border-border hover:enabled:bg-bg-2 hover:enabled:text-fg-0'

export function Pagination({
  page,
  totalPages,
  onPageChange,
  pageLabel,
  prevLabel,
  nextLabel,
  summary,
  isTransitioning = false,
}: PaginationProps) {
  return (
    <nav
      className={`flex items-center justify-between transition-opacity ${isTransitioning ? 'opacity-50 pointer-events-none' : ''}`}
    >
      <span className="text-xs text-fg-2">{summary}</span>
      <div className="flex items-center gap-1">
        <button
          type="button"
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          className={ARROW_CLASS}
          aria-label={prevLabel}
        >
          <ChevronLeft className="size-3.5" />
        </button>
        <span className="min-w-[5rem] text-center text-xs text-fg-1 tabular-nums">
          {pageLabel}
        </span>
        <button
          type="button"
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          className={ARROW_CLASS}
          aria-label={nextLabel}
        >
          <ChevronRight className="size-3.5" />
        </button>
      </div>
    </nav>
  )
}
