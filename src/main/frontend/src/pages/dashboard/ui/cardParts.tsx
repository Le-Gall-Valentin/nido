import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Link } from 'react-router-dom'
import type { LucideIcon } from 'lucide-react'
import { CTA_BUTTON_STYLE } from '@/shared/ui'

const TINT_TONE = {
  neutral: 'bg-bg-3 text-fg-2',
  red: 'bg-status-red-dim text-status-red',
  orange: 'bg-status-orange-dim text-status-orange',
  blue: 'bg-status-blue-dim text-status-blue',
  green: 'bg-status-green-dim text-status-green',
} as const

/** A tinted icon tile at the start of a row. The colour is the message: red urgent, orange watch, blue for information. */
export function Tint({ icon: Icon, tone }: { icon: LucideIcon; tone: keyof typeof TINT_TONE }) {
  return (
    <span className={`grid size-8 shrink-0 place-items-center rounded-[10px] ${TINT_TONE[tone]}`}>
      <Icon className="size-[17px]" aria-hidden="true" />
    </span>
  )
}

export function Counter({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'danger' }) {
  return (
    <span className={`rounded-full px-2 py-0.5 text-[11.5px] font-bold ${tone === 'danger' ? 'bg-status-red-dim text-status-red' : 'bg-bg-3 text-fg-2'}`}>
      {children}
    </span>
  )
}

const SMALL_BASE = 'inline-flex items-center justify-center gap-1.5 whitespace-nowrap rounded-lg border-[1.5px] px-2.5 py-1 text-[12.5px] font-semibold disabled:opacity-50'
const SMALL_PLAIN = 'border-border bg-bg-1 text-fg-2 hover:bg-bg-2'

/** The small in-row action. `primary` wears the app's CTA colours, which stay deep green in dark mode. */
export function SmallButton({ primary = false, className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { primary?: boolean }) {
  return (
    <button type="button" {...props}
      className={`${SMALL_BASE} ${primary ? 'border-transparent' : SMALL_PLAIN} ${className}`}
      style={primary ? CTA_BUTTON_STYLE : undefined} />
  )
}

export function SmallLink({ to, children }: { to: string; children: ReactNode }) {
  return <Link to={to} className={`${SMALL_BASE} ${SMALL_PLAIN}`}>{children}</Link>
}
