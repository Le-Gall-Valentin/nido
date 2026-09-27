import type { ReactNode } from 'react'
import type { LucideIcon } from 'lucide-react'
import { CardList } from './CardRow'

interface CardGroupProps {
  title: string
  tone?: 'default' | 'danger'
  icon?: LucideIcon
  /** No top margin — for groups laid side by side in a grid, so their titles line up. */
  flush?: boolean
  children: ReactNode
}

/** A titled sub-list inside a card: "En retard", "Cette semaine", "Budgets à surveiller"… */
export function CardGroup({ title, tone = 'default', icon: Icon, flush = false, children }: CardGroupProps) {
  return (
    <div className={flush ? '' : 'mt-2.5 first:mt-0.5'}>
      {/* Headings get the display face from index.css; a group title is body text, so it takes the body face back. */}
      <h3 style={{ fontFamily: 'var(--font-family-sans)', letterSpacing: 'normal' }}
        className={`flex items-center gap-1.5 py-1 text-[12.5px] font-semibold ${tone === 'danger' ? 'text-status-red' : 'text-fg-3'}`}>
        {Icon && <Icon className="size-3.5" aria-hidden="true" />}{title}
      </h3>
      <CardList>{children}</CardList>
    </div>
  )
}
