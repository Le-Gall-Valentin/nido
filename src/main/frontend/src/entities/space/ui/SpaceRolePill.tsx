import type { SpaceRole } from '../model/types'

const ROLE_PILL_CLASS: Record<SpaceRole, string> = {
  OWNER: 'bg-status-green-dim text-status-green',
  ADMIN: 'bg-status-orange-dim text-status-orange',
  MEMBER: 'bg-bg-3 text-fg-2',
  VIEWER: 'bg-bg-2 text-fg-3',
}

interface SpaceRolePillProps {
  role: SpaceRole
  /** Translated label; defaults to the raw role value. */
  label?: string
}

export function SpaceRolePill({ role, label }: SpaceRolePillProps) {
  return (
    <span
      className={`inline-flex items-center px-[9px] py-[3px] rounded-[6px] text-[11px] font-bold tracking-[0.02em] whitespace-nowrap ${ROLE_PILL_CLASS[role]}`}
    >
      {label ?? role}
    </span>
  )
}
