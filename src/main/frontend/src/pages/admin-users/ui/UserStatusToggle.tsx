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
        className={`relative inline-flex h-[26px] w-[44px] shrink-0 items-center rounded-full border-0 transition-colors
          ${user.isActive ? 'bg-accent' : 'bg-bg-4'}
          ${disabled ? 'opacity-40 cursor-not-allowed' : 'cursor-pointer'}`}
      >
        <span className={`pointer-events-none inline-block size-5 rounded-full bg-white shadow-[0_1px_2px_rgba(0,0,0,0.2)] transition-transform ${user.isActive ? 'translate-x-[21px]' : 'translate-x-[3px]'}`} />
      </button>
      <span className="text-[12.5px] text-fg-2">
        {user.isActive ? t('table.active') : t('table.inactive')}
      </span>
    </div>
  )
}
