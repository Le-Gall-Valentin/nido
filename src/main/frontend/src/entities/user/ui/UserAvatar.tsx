import type { UserRole } from '../model/types'
import { getInitials } from '../lib/getInitials'

const SUPER_ADMIN_GRADIENT = 'linear-gradient(135deg, #a78bfa, #818cf8)'
const DEFAULT_GRADIENT = 'linear-gradient(135deg, var(--color-accent), #818cf8)'

interface UserAvatarProps {
  username: string
  role: UserRole
  /** Tailwind classes controlling size and shape (e.g. "size-6 rounded-md text-[10px]"). */
  className?: string
}

export function UserAvatar({ username, role, className = 'size-6 rounded-md text-[10px]' }: UserAvatarProps) {
  const gradient = role === 'SUPER_ADMIN' ? SUPER_ADMIN_GRADIENT : DEFAULT_GRADIENT
  return (
    <div
      className={`shrink-0 flex items-center justify-center font-semibold text-white ${className}`}
      style={{ background: gradient }}
      aria-hidden="true"
    >
      {getInitials(username)}
    </div>
  )
}
