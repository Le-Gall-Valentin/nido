import { useTranslation } from 'react-i18next'
import type { User, AdminUser } from '@/entities/user'
import { RolePill, UserAvatar } from '@/entities/user'
import { formatUserDate } from '../lib/formatUserDate'
import { UserStatusToggle } from './UserStatusToggle'
import { UserActions } from './UserActions'
import { TotpBadge } from './TotpBadge'
import type { UserRowCallbacks } from './userRowCallbacks'

interface UsersTableProps extends UserRowCallbacks {
  users: AdminUser[]
  isLoading: boolean
  currentUser: User
  /** Id of the user whose active/inactive toggle is currently in flight. */
  pendingToggleId?: string | null
}

export function UsersTable({
  users,
  isLoading,
  currentUser,
  pendingToggleId,
  onToggleActive,
  onEditRole,
  onResetTotp,
  onDelete,
}: UsersTableProps) {
  const { t, i18n } = useTranslation('adminUsers')

  if (isLoading) {
    return (
      <div className="rounded-md border border-border bg-bg-1 overflow-hidden">
        <div className="divide-y divide-border">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="flex items-center gap-4 px-4 py-3">
              <div className="size-6 shrink-0 rounded-md bg-bg-3 animate-pulse" />
              <div className="h-3.5 w-28 bg-bg-3 animate-pulse rounded" />
              <div className="h-3 w-36 bg-bg-3 animate-pulse rounded ml-auto" />
            </div>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="rounded-md border border-border bg-bg-1 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border bg-bg-2">
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_user')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_email')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_role')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_status')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_totp')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_created')}</th>
              <th className="px-4 py-2.5" />
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {users.length === 0 && (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-sm text-fg-2">
                  {t('table.empty')}
                </td>
              </tr>
            )}
            {users.map(user => (
              <UserRow
                key={user.id}
                user={user}
                currentUser={currentUser}
                youLabel={t('table.you')}
                roleLabel={t(`user.role.${user.role}`, { ns: 'shell' })}
                createdDate={formatUserDate(user.createdAt, i18n.language)}
                isToggling={user.id === pendingToggleId}
                onToggleActive={onToggleActive}
                onEditRole={onEditRole}
                onResetTotp={onResetTotp}
                onDelete={onDelete}
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

interface RowProps extends UserRowCallbacks {
  user: AdminUser
  currentUser: User
  youLabel: string
  roleLabel: string
  createdDate: string
  isToggling: boolean
}

function UserRow({ user, currentUser, youLabel, roleLabel, createdDate, isToggling, onToggleActive, onEditRole, onResetTotp, onDelete }: RowProps) {
  const isMe = user.id === currentUser.id

  return (
    <tr className={isMe ? 'bg-accent/[0.02]' : undefined}>
      <td className="px-4 py-3">
        <div className="flex items-center gap-2">
          <UserAvatar username={user.username} role={user.role} />
          <span className="font-medium text-fg-0">{user.username}</span>
          {isMe && (
            <span className="text-[10px] px-1.5 py-0.5 rounded bg-bg-3 text-fg-2 font-mono">
              {youLabel}
            </span>
          )}
        </div>
      </td>

      <td className="px-4 py-3 font-mono text-xs text-fg-2">{user.email}</td>

      <td className="px-4 py-3">
        <RolePill role={user.role} label={roleLabel} />
      </td>

      <td className="px-4 py-3">
        <UserStatusToggle user={user} currentUser={currentUser} onToggle={onToggleActive} isPending={isToggling} />
      </td>

      <td className="px-4 py-3">
        <TotpBadge enabled={user.totpEnabled} />
      </td>

      <td className="px-4 py-3 font-mono text-[11px] text-fg-2">{createdDate}</td>

      <td className="px-4 py-3">
        <UserActions
          user={user}
          currentUser={currentUser}
          onEditRole={onEditRole}
          onResetTotp={onResetTotp}
          onDelete={onDelete}
          className="justify-end"
        />
      </td>
    </tr>
  )
}
