import { useTranslation } from 'react-i18next'
import { Switch } from '@/shared/ui'
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
      <Switch
        checked={user.isActive}
        aria-label={label}
        title={permissionDenialTitle(check, t, label)}
        disabled={disabled}
        onChange={() => onToggle(user)}
      />
      <span className="text-[12.5px] text-fg-2">
        {user.isActive ? t('table.active') : t('table.inactive')}
      </span>
    </div>
  )
}
