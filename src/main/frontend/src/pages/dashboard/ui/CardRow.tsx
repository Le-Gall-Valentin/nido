import type { ReactNode } from 'react'

interface CardRowProps {
  /** Fixed-width start: a time, a date, a checkbox, an icon tile. */
  lead?: ReactNode
  title: ReactNode
  meta?: ReactNode
  /** Full-width content under the title and meta: a progress bar. */
  extra?: ReactNode
  /** End of the row: an amount, avatars, an action. */
  trail?: ReactNode
  /** An event that is over. */
  muted?: boolean
  /** False for the row right after the now line. */
  divider?: boolean
}

/** One row of any block: the same height, spacing and dividers everywhere on the page. */
export function CardRow({ lead, title, meta, extra, trail, muted = false, divider = true }: CardRowProps) {
  return (
    <li className={`flex min-h-[46px] items-center gap-3 py-[7px] ${divider ? 'border-t border-border first:border-t-0' : ''} ${muted ? 'opacity-45' : ''}`}>
      {lead}
      <div className="min-w-0 flex-1">
        <div className="truncate text-sm font-medium text-fg-0">{title}</div>
        {meta && <div className="mt-0.5 flex flex-wrap items-center gap-x-2.5 gap-y-0.5 text-[12.5px] text-fg-3">{meta}</div>}
        {extra}
      </div>
      {trail && <div className="flex shrink-0 items-center gap-2">{trail}</div>}
    </li>
  )
}

export function CardList({ children }: { children: ReactNode }) {
  return <ul className="m-0 list-none p-0">{children}</ul>
}

/** The fixed-width time or date column. */
export function RowLead({ children, className = 'w-[50px]' }: { children: ReactNode; className?: string }) {
  return <span className={`shrink-0 text-[13px] font-semibold tabular-nums text-fg-2 ${className}`}>{children}</span>
}
