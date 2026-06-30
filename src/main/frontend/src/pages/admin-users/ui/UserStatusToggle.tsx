import { useTranslation } from 'react-i18next'
import type { User, AdminUser } from '@/entities/user'
import { canActivate, canDeactivate } from '../lib/permissions'
import { permissionDenialTitle } from './permissionDenialTitle'

interface UserStatusToggleProps {
  user: AdminUser
  currentUser: User
  onToggle: (user: AdminUser) => void
  /** Disables the switch while this row's toggle request is in flight. */
  isPending?: boolean
}

/** Active/inactive switch shared by the table and card layouts; self-gating on permissions. */
export function UserStatusToggle({ user, currentUser, onToggle, isPending = false }: UserStatusToggleProps) {
  const { t } = useTranslation('adminUsers')
  const check = user.isActive
    ? canDeactivate(currentUser, user)
    : canActivate(currentUser, user)
  const label = user.isActive ? t('table.toggle_deactivate') : t('table.toggle_activate')
  const disabled = !check.ok || isPending

  return (
    <div className="flex items-center gap-2">
      <button
        role="switch"
        aria-checked={user.isActive}
        aria-label={label}
        title={permissionDenialTitle(check, t, label)}
        onClick={() => { if (!disabled) onToggle(user) }}
        disabled={disabled}
        className={`relative inline-flex h-4 w-7 shrink-0 items-center rounded-full border transition-colors
          ${user.isActive ? 'bg-status-green border-status-green/20' : 'bg-bg-4 border-border-2'}
          ${disabled ? 'opacity-40 cursor-not-allowed' : 'cursor-pointer'}`}
      >
        <span className={`pointer-events-none inline-block size-3 rounded-full bg-white shadow-sm transition-transform ${user.isActive ? 'translate-x-3.5' : 'translate-x-0.5'}`} />
      </button>
      <span className="text-xs text-fg-2">
        {user.isActive ? t('table.active') : t('table.inactive')}
      </span>
    </div>
  )
}
