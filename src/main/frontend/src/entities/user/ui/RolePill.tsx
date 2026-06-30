import type { UserRole } from '../model/types'

const ROLE_PILL_CLASS: Record<UserRole, string> = {
  SUPER_ADMIN: 'border-accent/20 bg-accent-dim text-accent',
  ADMIN: 'border-border-2 bg-bg-3 text-fg-2',
  USER: 'border-status-blue/20 bg-status-blue-dim text-status-blue',
}

interface RolePillProps {
  role: UserRole
  /** Translated label; defaults to the raw role value. */
  label?: string
}

export function RolePill({ role, label }: RolePillProps) {
  return (
    <span
      className={`inline-flex items-center px-[7px] py-[2px] rounded-full border text-[10px] font-semibold font-mono uppercase tracking-[0.04em] whitespace-nowrap ${ROLE_PILL_CLASS[role]}`}
    >
      {label ?? role}
    </span>
  )
}
