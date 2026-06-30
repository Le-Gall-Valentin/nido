import { useTranslation } from 'react-i18next'
import { Key, Pencil, Trash2 } from 'lucide-react'
import type { User, AdminUser } from '@/entities/user'
import { canDelete, canResetTotp, canEditRole } from '../lib/permissions'
import { permissionDenialTitle } from './permissionDenialTitle'

interface UserActionsProps {
  user: AdminUser
  currentUser: User
  onEditRole: (user: AdminUser) => void
  onResetTotp: (user: AdminUser) => void
  onDelete: (user: AdminUser) => void
  /** 'sm' for the dense desktop table, 'md' for touch-friendly card targets. */
  size?: 'sm' | 'md'
  className?: string
}

const BUTTON_SIZE = { sm: 'size-7', md: 'size-9' } as const

/** Reset 2FA / edit role / delete buttons shared by the table and card layouts; self-gating on permissions. */
export function UserActions({
  user,
  currentUser,
  onEditRole,
  onResetTotp,
  onDelete,
  size = 'sm',
  className = '',
}: UserActionsProps) {
  const { t } = useTranslation('adminUsers')
  const totpCheck = canResetTotp(currentUser, user)
  const editCheck = canEditRole(currentUser, user)
  const deleteCheck = canDelete(currentUser, user)

  const resetLabel = t('table.btn_reset_totp', { username: user.username })
  const editLabel = t('table.btn_edit', { username: user.username })
  const deleteLabel = t('table.btn_delete')

  return (
    <div className={`flex items-center gap-1 ${className}`}>
      <ActionButton
        label={resetLabel}
        title={permissionDenialTitle(totpCheck, t, resetLabel)}
        disabled={!totpCheck.ok}
        size={size}
        onClick={() => onResetTotp(user)}
      >
        <Key className="size-3.5" />
      </ActionButton>
      <ActionButton
        label={editLabel}
        title={permissionDenialTitle(editCheck, t, editLabel)}
        disabled={!editCheck.ok}
        size={size}
        onClick={() => onEditRole(user)}
      >
        <Pencil className="size-3.5" />
      </ActionButton>
      <ActionButton
        label={deleteLabel}
        title={permissionDenialTitle(deleteCheck, t, deleteLabel)}
        disabled={!deleteCheck.ok}
        size={size}
        onClick={() => onDelete(user)}
      >
        <Trash2 className="size-3.5" />
      </ActionButton>
    </div>
  )
}

interface ActionButtonProps {
  label: string
  title: string
  disabled: boolean
  size: 'sm' | 'md'
  onClick: () => void
  children: React.ReactNode
}

function ActionButton({ label, title, disabled, size, onClick, children }: ActionButtonProps) {
  return (
    <button
      aria-label={label}
      title={title}
      disabled={disabled}
      onClick={onClick}
      className={`${BUTTON_SIZE[size]} flex items-center justify-center rounded-md border border-transparent text-fg-2 transition-colors
        ${disabled
          ? 'opacity-40 cursor-not-allowed'
          : 'hover:border-border hover:bg-bg-2 hover:text-fg-0'
        }`}
    >
      {children}
    </button>
  )
}
