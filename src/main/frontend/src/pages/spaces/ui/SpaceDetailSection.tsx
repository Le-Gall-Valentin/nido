import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { LogOut, Trash2, UserPlus } from 'lucide-react'
import { useAuth } from '@/features/auth'
import { Alert, Button, Spinner, CTA_BUTTON_STYLE } from '@/shared/ui'
import { SpaceAvatar, SpaceRolePill, canManageSpace, isOwner, isPersonal, type SpaceMember } from '@/entities/space'
import { useSpaceDetail } from '../model/useSpaceDetail'
import { useSpaceMembers } from '../model/useSpaceMembers'
import { useSpaceInvitations } from '../model/useSpaceInvitations'
import {
  useChangeMemberRole,
  useRemoveMember,
  useTransferOwnership,
  useLeaveSpace,
  useDeleteSpace,
  useInviteMember,
  useRevokeInvitation,
} from '../model/useSpaceMutations'
import { mapSpaceErrorToKey } from '../lib/mapSpaceErrorToKey'
import type { AssignableSpaceRole } from '../model/ISpacesPageApi'
import { MemberList } from './MemberList'
import { InvitationList } from './InvitationList'
import { InviteMemberModal } from './InviteMemberModal'
import { LeaveSpaceModal } from './LeaveSpaceModal'
import { DeleteSpaceModal } from './DeleteSpaceModal'
import { TransferOwnershipModal } from './TransferOwnershipModal'

interface SpaceDetailSectionProps {
  spaceId: string
  /** Called after leaving the space succeeds; navigation is this component's caller's job. */
  onLeft: () => void
  /** Called after deleting the space succeeds; navigation is this component's caller's job. */
  onDeleted: () => void
}

export function SpaceDetailSection({ spaceId, onLeft, onDeleted }: SpaceDetailSectionProps) {
  const { t } = useTranslation('spaces')
  const currentUser = useAuth((s) => s.user)

  const { data: space, isPending: detailPending, isError: detailError, error: detailErrorObj } = useSpaceDetail(spaceId)
  const myRole = space?.myRole
  // The personal space is immutable at the domain level regardless of role:
  // no invite, leave or delete action is ever valid on it, even though its
  // sole member is trivially its OWNER.
  const shared = space ? !isPersonal(space) : false
  const manager = shared && !!myRole && canManageSpace(myRole)
  const owner = shared && !!myRole && isOwner(myRole)

  const { data: members, isPending: membersPending, isError: membersError } = useSpaceMembers(spaceId)
  const { data: invitations, isPending: invitationsPending, isError: invitationsError } = useSpaceInvitations(spaceId, manager)

  const changeMemberRole = useChangeMemberRole(spaceId)
  const removeMember = useRemoveMember(spaceId)
  const transferOwnership = useTransferOwnership(spaceId)
  const leaveSpace = useLeaveSpace(spaceId)
  const deleteSpace = useDeleteSpace(spaceId)
  const inviteMember = useInviteMember(spaceId)
  const revokeInvitation = useRevokeInvitation(spaceId)

  const [actionErrorKey, setActionErrorKey] = useState<string | null>(null)
  const [inviteOpen, setInviteOpen] = useState(false)
  const [leaveOpen, setLeaveOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [transferTarget, setTransferTarget] = useState<SpaceMember | null>(null)

  function handleChangeRole(member: SpaceMember, role: AssignableSpaceRole) {
    setActionErrorKey(null)
    changeMemberRole.mutate(
      { userId: member.userId, role },
      { onError: (error) => setActionErrorKey(mapSpaceErrorToKey(error, 'members')) }
    )
  }

  function handleRemove(member: SpaceMember) {
    setActionErrorKey(null)
    removeMember.mutate(member.userId, {
      onError: (error) => setActionErrorKey(mapSpaceErrorToKey(error, 'members')),
    })
  }

  function handleRevoke(invitationId: string) {
    setActionErrorKey(null)
    revokeInvitation.mutate(invitationId, {
      onError: (error) => setActionErrorKey(mapSpaceErrorToKey(error, 'invitations')),
    })
  }

  if (detailPending) return <Spinner label={t('loading')} fullscreen={false} />

  if (detailError || !space) {
    return <Alert variant="error">{t(mapSpaceErrorToKey(detailErrorObj, 'detail'))}</Alert>
  }

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div className="flex items-center gap-4">
          <SpaceAvatar space={space} size="lg" />
          <div>
            <h1 className="text-[26px] font-semibold tracking-tight text-fg-0">{space.name}</h1>
            {space.description && <p className="mt-0.5 text-sm text-fg-2">{space.description}</p>}
            {myRole && (
              <div className="mt-1.5">
                <SpaceRolePill role={myRole} label={t(`role.${myRole}`)} />
              </div>
            )}
          </div>
        </div>

        <div className="flex shrink-0 flex-wrap gap-2">
          {manager && (
            <Button
              onClick={() => setInviteOpen(true)}
              className="border-transparent font-semibold"
              style={CTA_BUTTON_STYLE}
            >
              <UserPlus className="size-4" />
              {t('actions.invite')}
            </Button>
          )}
          {shared && !owner && (
            <Button onClick={() => setLeaveOpen(true)}>
              <LogOut className="size-4" />
              {t('actions.leave')}
            </Button>
          )}
          {owner && (
            <Button onClick={() => setDeleteOpen(true)} className="text-status-red">
              <Trash2 className="size-4" />
              {t('actions.delete')}
            </Button>
          )}
        </div>
      </div>

      {actionErrorKey && (
        <Alert variant="error" className="mb-4" onDismiss={() => setActionErrorKey(null)} dismissLabel={t('close')}>
          {t(actionErrorKey)}
        </Alert>
      )}

      <section className="mb-6">
        <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-[0.05em] text-fg-3">
          {t('members.title')}
        </h2>
        {membersPending && <Spinner label={t('loading')} fullscreen={false} />}
        {membersError && <Alert variant="error">{t('members.load_error')}</Alert>}
        {!membersPending && !membersError && myRole && (
          <MemberList
            members={members ?? []}
            currentUserId={currentUser?.id ?? ''}
            myRole={myRole}
            pendingChangeRoleUserId={changeMemberRole.isPending ? changeMemberRole.variables?.userId : null}
            pendingRemoveUserId={removeMember.isPending ? removeMember.variables : null}
            onChangeRole={handleChangeRole}
            onRemove={handleRemove}
            onTransfer={setTransferTarget}
          />
        )}
      </section>

      {manager && (
        <section>
          <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-[0.05em] text-fg-3">
            {t('invitations.title')}
          </h2>
          {invitationsPending && <Spinner label={t('loading')} fullscreen={false} />}
          {invitationsError && <Alert variant="error">{t('invitations.load_error')}</Alert>}
          {!invitationsPending && !invitationsError && (
            <InvitationList
              invitations={invitations ?? []}
              pendingRevokeId={revokeInvitation.isPending ? revokeInvitation.variables : null}
              onRevoke={(invitation) => handleRevoke(invitation.id)}
            />
          )}
        </section>
      )}

      {inviteOpen && (
        <InviteMemberModal
          onClose={() => setInviteOpen(false)}
          onInvite={(email, role) => inviteMember.mutateAsync({ email, role })}
          onSuccess={() => setInviteOpen(false)}
        />
      )}

      {leaveOpen && (
        <LeaveSpaceModal
          spaceName={space.name}
          onClose={() => setLeaveOpen(false)}
          onLeave={() => leaveSpace.mutateAsync()}
          onSuccess={() => { setLeaveOpen(false); onLeft() }}
        />
      )}

      {deleteOpen && (
        <DeleteSpaceModal
          spaceName={space.name}
          onClose={() => setDeleteOpen(false)}
          onDelete={() => deleteSpace.mutateAsync()}
          onSuccess={() => { setDeleteOpen(false); onDeleted() }}
        />
      )}

      {transferTarget && (
        <TransferOwnershipModal
          target={transferTarget}
          onClose={() => setTransferTarget(null)}
          onTransfer={(userId) => transferOwnership.mutateAsync(userId)}
          onSuccess={() => setTransferTarget(null)}
        />
      )}
    </div>
  )
}
