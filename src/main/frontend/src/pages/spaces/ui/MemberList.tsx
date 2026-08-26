import { useTranslation } from 'react-i18next'
import { Crown, UserMinus, UserX } from 'lucide-react'
import { SpaceRolePill, canManageSpace, isOwner, type SpaceMember, type SpaceRole } from '@/entities/space'
import { getInitials } from '@/entities/user'
import { formatRelativeTime } from '@/shared/lib'
import type { AssignableSpaceRole } from '../model/ISpacesPageApi'

const ASSIGNABLE_ROLES: AssignableSpaceRole[] = ['ADMIN', 'MEMBER', 'VIEWER']

interface MemberListProps {
  members: SpaceMember[]
  currentUserId: string
  myRole: SpaceRole
  pendingChangeRoleUserId?: string | null
  pendingRemoveUserId?: string | null
  onChangeRole: (member: SpaceMember, role: AssignableSpaceRole) => void
  onRemove: (member: SpaceMember) => void
  onTransfer: (member: SpaceMember) => void
}

/**
 * A member whose account has been anonymised by a GDPR deletion arrives with
 * username and email both null — it renders as a deleted account, never as a
 * blank row or a crash.
 */
export function MemberList({
  members,
  currentUserId,
  myRole,
  pendingChangeRoleUserId,
  pendingRemoveUserId,
  onChangeRole,
  onRemove,
  onTransfer,
}: MemberListProps) {
  const { t, i18n } = useTranslation('spaces')
  const canManage = canManageSpace(myRole)
  const iAmOwner = isOwner(myRole)

  if (members.length === 0) {
    return <p className="text-sm text-fg-2">{t('members.empty')}</p>
  }

  return (
    <ul className="divide-y divide-bg-3 rounded-2xl border border-border bg-bg-1 overflow-hidden">
      {members.map((member) => {
        const isMe = member.userId === currentUserId
        const targetIsOwner = member.role === 'OWNER'
        const deleted = member.username == null
        const showManageActions = canManage && !isMe && !targetIsOwner
        // A transfer target must be able to hold ownership: not a VIEWER, per the backend.
        const showTransfer = iAmOwner && !isMe && !targetIsOwner && member.role !== 'VIEWER'
        const changingRole = pendingChangeRoleUserId === member.userId
        const removing = pendingRemoveUserId === member.userId

        return (
          <li key={member.userId} className="flex flex-wrap items-center gap-3.5 px-[18px] py-3.5">
            <div
              className={`flex size-10 shrink-0 items-center justify-center rounded-full text-[13px] font-semibold ${deleted ? 'bg-bg-3 text-fg-3' : 'text-white'}`}
              style={deleted ? undefined : { background: 'linear-gradient(135deg, var(--avatar-from), var(--avatar-to))' }}
              aria-hidden="true"
            >
              {deleted ? <UserX className="size-4" /> : getInitials(member.username ?? '')}
            </div>

            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-1.5 text-[14.5px] font-semibold text-fg-0">
                <span className={deleted ? 'italic font-medium text-fg-3' : 'truncate'}>
                  {deleted ? t('members.deleted_account') : member.username}
                </span>
                {isMe && <span className="text-xs font-medium text-fg-3">({t('members.you')})</span>}
              </div>
              <div className="text-[12.5px] text-fg-3 truncate">
                {t('members.joined', { time: formatRelativeTime(member.joinedAt, i18n.language) })}
                {!deleted && member.email && <> · {member.email}</>}
              </div>
            </div>

            {showManageActions ? (
              <select
                aria-label={t('members.role_select_label', { name: deleted ? t('members.deleted_account') : member.username })}
                value={member.role}
                disabled={changingRole}
                onChange={(e) => onChangeRole(member, e.target.value as AssignableSpaceRole)}
                className="rounded-[8px] border-[1.5px] border-border bg-bg-1 px-2.5 py-1.5 text-[13px] font-semibold text-fg-1 outline-none transition-all hover:border-border-2 focus:border-accent disabled:opacity-50"
              >
                {ASSIGNABLE_ROLES.map((role) => (
                  <option key={role} value={role}>{t(`space:role.${role}`)}</option>
                ))}
              </select>
            ) : (
              <SpaceRolePill role={member.role} label={t(`space:role.${member.role}`)} />
            )}

            <div className="flex shrink-0 items-center gap-1">
              {showTransfer && (
                <button
                  type="button"
                  aria-label={t('members.action_transfer', { name: deleted ? t('members.deleted_account') : member.username })}
                  onClick={() => onTransfer(member)}
                  className="flex size-8 items-center justify-center rounded-md text-fg-2 transition-colors hover:bg-bg-2 hover:text-status-orange"
                >
                  <Crown className="size-4" />
                </button>
              )}
              {showManageActions && (
                <button
                  type="button"
                  aria-label={t('members.action_remove', { name: deleted ? t('members.deleted_account') : member.username })}
                  disabled={removing}
                  onClick={() => onRemove(member)}
                  className="flex size-8 items-center justify-center rounded-md text-fg-2 transition-colors hover:bg-bg-2 hover:text-status-red disabled:opacity-50"
                >
                  <UserMinus className="size-4" />
                </button>
              )}
            </div>
          </li>
        )
      })}
    </ul>
  )
}
