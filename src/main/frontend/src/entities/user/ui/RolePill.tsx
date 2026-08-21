import type { UserRole } from '../model/types'

const ROLE_PILL_CLASS: Record<UserRole, string> = {
  SUPER_ADMIN: 'bg-status-green-dim text-status-green',
  ADMIN: 'bg-status-orange-dim text-status-orange',
  USER: 'bg-bg-3 text-fg-2',
}

interface RolePillProps {
  role: UserRole
  /** Translated label; defaults to the raw role value. */
  label?: string
}

export function RolePill({ role, label }: RolePillProps) {
  return (
    <span
      className={`inline-flex items-center px-[9px] py-[3px] rounded-[6px] text-[11px] font-bold tracking-[0.02em] whitespace-nowrap ${ROLE_PILL_CLASS[role]}`}
    >
      {label ?? role}
    </span>
  )
}
