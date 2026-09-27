import { useId, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ChevronRight, type LucideIcon } from 'lucide-react'

interface DashboardCardProps {
  icon: LucideIcon
  title: string
  tone?: 'default' | 'attention'
  /** Beside the title: a counter, the month. */
  aside?: ReactNode
  /** Right of the header, before the link: the tasks' "À moi / Tous" toggle. */
  controls?: ReactNode
  link?: { to: string; label: string }
  footer?: ReactNode
  children: ReactNode
}

/**
 * The one card anatomy of the dashboard: icon tile, title, optional aside and controls, a "see all"
 * link, the body, and a footer pinned to the bottom so two cards side by side end level. `@container`
 * lets a card rearrange its own content by its own width, not the viewport's.
 */
export function DashboardCard({ icon: Icon, title, tone = 'default', aside, controls, link, footer, children }: DashboardCardProps) {
  const titleId = useId()
  const attention = tone === 'attention'
  return (
    <section aria-labelledby={titleId}
      className={`@container flex min-w-0 flex-col rounded-2xl border bg-bg-1 px-3.5 pb-3 pt-3.5 md:px-[18px] md:pb-3.5 md:pt-4 ${attention ? 'border-status-red/30' : 'border-border'}`}>
      <header className="mb-1.5 flex min-h-[34px] items-center gap-2.5">
        <span className={`grid size-8 shrink-0 place-items-center rounded-[10px] ${attention ? 'bg-status-red-dim text-status-red' : 'bg-bg-3 text-fg-2'}`}>
          <Icon className="size-[17px]" aria-hidden="true" />
        </span>
        <h2 id={titleId} className="text-[16.5px] font-semibold text-fg-0">{title}</h2>
        {aside}
        {(controls || link) && (
          <div className="ml-auto flex items-center gap-1">
            {controls}
            {link && (
              <Link to={link.to}
                className="inline-flex items-center gap-px whitespace-nowrap rounded-lg py-1.5 pl-2 pr-1 text-[13px] font-semibold text-fg-2 hover:bg-bg-2">
                {link.label}<ChevronRight className="size-[15px]" aria-hidden="true" />
              </Link>
            )}
          </div>
        )}
      </header>
      {children}
      {footer && <div className="mt-auto flex items-center gap-2 pt-2">{footer}</div>}
    </section>
  )
}
