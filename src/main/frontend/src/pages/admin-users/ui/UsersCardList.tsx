import { useTranslation } from 'react-i18next'
import type { User, AdminUser } from '@/entities/user'
import { RolePill, UserAvatar } from '@/entities/user'
import { formatUserDate } from '../lib/formatUserDate'
import { UserStatusToggle } from './UserStatusToggle'
import { UserActions } from './UserActions'
import { TotpBadge } from './TotpBadge'
import type { UserRowCallbacks } from './userRowCallbacks'

interface UsersCardListProps extends UserRowCallbacks {
  users: AdminUser[]
  isLoading: boolean
  currentUser: User
  /** Id of the user whose active/inactive toggle is currently in flight. */
  pendingToggleId?: string | null
}

/** Mobile counterpart of UsersTable: one stacked card per user, no horizontal scroll. */
export function UsersCardList({
  users,
  isLoading,
  currentUser,
  pendingToggleId,
  onToggleActive,
  onEditRole,
  onResetTotp,
  onDelete,
}: UsersCardListProps) {
  const { t, i18n } = useTranslation('adminUsers')

  if (isLoading) {
    return (
      <div className="flex flex-col gap-2.5">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="rounded-lg border border-border bg-bg-1 p-3.5">
            <div className="flex items-center gap-3">
              <div className="size-9 shrink-0 rounded-md bg-bg-3 animate-pulse" />
              <div className="flex-1 space-y-2">
                <div className="h-3.5 w-28 bg-bg-3 animate-pulse rounded" />
                <div className="h-3 w-40 bg-bg-3 animate-pulse rounded" />
              </div>
            </div>
          </div>
        ))}
      </div>
    )
  }

  if (users.length === 0) {
    return (
      <div className="rounded-lg border border-border bg-bg-1 px-4 py-8 text-center text-sm text-fg-2">
        {t('table.empty')}
      </div>
    )
  }

  return (
    <ul className="flex flex-col gap-2.5">
      {users.map(user => (
        <li key={user.id}>
          <UserCard
            user={user}
            currentUser={currentUser}
            youLabel={t('table.you')}
            roleLabel={t(`user.role.${user.role}`, { ns: 'shell' })}
            createdLabel={t('table.col_created')}
            createdDate={formatUserDate(user.createdAt, i18n.language)}
            isToggling={user.id === pendingToggleId}
            onToggleActive={onToggleActive}
            onEditRole={onEditRole}
            onResetTotp={onResetTotp}
            onDelete={onDelete}
          />
        </li>
      ))}
    </ul>
  )
}

interface UserCardProps extends UserRowCallbacks {
  user: AdminUser
  currentUser: User
  youLabel: string
  roleLabel: string
  createdLabel: string
  createdDate: string
  isToggling: boolean
}

function UserCard({
  user,
  currentUser,
  youLabel,
  roleLabel,
  createdLabel,
  createdDate,
  isToggling,
  onToggleActive,
  onEditRole,
  onResetTotp,
  onDelete,
}: UserCardProps) {
  const isMe = user.id === currentUser.id

  return (
    <div className={`rounded-lg border border-border bg-bg-1 p-3.5 ${isMe ? 'ring-1 ring-accent/20' : ''}`}>
      {/* Identity */}
      <div className="flex items-start gap-3">
        <UserAvatar username={user.username} role={user.role} className="size-9 rounded-md text-xs" />
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span className="font-medium text-fg-0 truncate">{user.username}</span>
            {isMe && (
              <span className="shrink-0 text-[10px] px-1.5 py-0.5 rounded bg-bg-3 text-fg-2 font-mono">
                {youLabel}
              </span>
            )}
          </div>
          <div className="font-mono text-xs text-fg-2 truncate">{user.email}</div>
        </div>
        <RolePill role={user.role} label={roleLabel} />
      </div>

      {/* Status / 2FA / created */}
      <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 border-t border-border pt-3">
        <UserStatusToggle user={user} currentUser={currentUser} onToggle={onToggleActive} isPending={isToggling} />
        <TotpBadge enabled={user.totpEnabled} />
        <span className="font-mono text-[11px] text-fg-3">
          {createdLabel} · {createdDate}
        </span>
      </div>

      {/* Actions */}
      <div className="mt-3 flex justify-end">
        <UserActions
          user={user}
          currentUser={currentUser}
          onEditRole={onEditRole}
          onResetTotp={onResetTotp}
          onDelete={onDelete}
          size="md"
        />
      </div>
    </div>
  )
}
