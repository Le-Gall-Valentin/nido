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
      <div className="rounded-2xl border border-border bg-bg-1 overflow-hidden">
        <div className="divide-y divide-bg-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="flex items-center gap-4 px-[18px] py-3.5">
              <div className="size-10 shrink-0 rounded-full bg-bg-3 animate-pulse" />
              <div className="h-3.5 w-28 bg-bg-3 animate-pulse rounded" />
              <div className="h-3 w-36 bg-bg-3 animate-pulse rounded ml-auto" />
            </div>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="rounded-2xl border border-border bg-bg-1 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full">
          {/* Le design n'affiche pas d'en-tête : on le garde pour l'accessibilité. */}
          <thead className="sr-only">
            <tr>
              <th>{t('table.col_user')}</th>
              <th>{t('table.col_role')}</th>
              <th>{t('table.col_status')}</th>
              <th>{t('table.col_totp')}</th>
              <th aria-hidden="true" />
            </tr>
          </thead>
          <tbody className="divide-y divide-bg-3">
            {users.length === 0 && (
              <tr>
                <td colSpan={5} className="px-[18px] py-8 text-center text-sm text-fg-2">
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
                meta={t('table.meta', { email: user.email, date: formatUserDate(user.createdAt, i18n.language) })}
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
  meta: string
  isToggling: boolean
}

function UserRow({ user, currentUser, youLabel, roleLabel, meta, isToggling, onToggleActive, onEditRole, onResetTotp, onDelete }: RowProps) {
  const isMe = user.id === currentUser.id

  return (
    <tr>
      <td className="py-3.5 pl-[18px] pr-3.5">
        <div className="flex items-center gap-3.5">
          <UserAvatar username={user.username} role={user.role} />
          <div className="min-w-0">
            <div className="text-[14.5px] font-semibold text-fg-0 truncate">
              {user.username}
              {isMe && (
                <span className="ml-1.5 text-xs font-medium text-fg-3">({youLabel})</span>
              )}
            </div>
            <div className="text-[12.5px] text-fg-3 truncate">{meta}</div>
          </div>
        </div>
      </td>

      <td className="px-3.5 py-3.5">
        <RolePill role={user.role} label={roleLabel} />
      </td>

      <td className="px-3.5 py-3.5">
        <UserStatusToggle user={user} currentUser={currentUser} onToggle={onToggleActive} isPending={isToggling} />
      </td>

      <td className="px-3.5 py-3.5">
        <TotpBadge enabled={user.totpEnabled} />
      </td>

      <td className="py-3.5 pl-3.5 pr-[18px]">
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
