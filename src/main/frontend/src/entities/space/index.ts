export type {
  SpaceType,
  SpaceRole,
  InvitationStatus,
  SpaceSummary,
  SpaceDetail,
  SpaceMember,
  SpaceInvitation,
  ReceivedInvitation,
} from './model/types'
export { isPersonal } from './model/types'
export { SPACE_ACCENTS, SPACE_GLYPHS, PERSONAL_ACCENT, PERSONAL_GLYPH, safeAccent, safeGlyph } from './lib/spaceAppearance'
export { canManageSpace, canWrite, isOwner, rank } from './lib/spaceRole'
export type { SpaceAvatarSize } from './ui/SpaceAvatar'
export { SpaceAvatar } from './ui/SpaceAvatar'
export { SpaceRolePill } from './ui/SpaceRolePill'
export type { TransferOperation, TransferDestination } from './ui/TransferDialog'
export { TransferDialog } from './ui/TransferDialog'
export type { ISpaceMembersApi } from './model/ISpaceMembersApi'
export { SpaceMembersApiProvider, useSpaceMembersApi } from './model/spaceMembersApiContext'
export { spaceMembersKey, useSpaceMembers } from './model/useSpaceMembers'

export type { ISpaceApi, AssignableSpaceRole, CreateSpaceInput, UpdateSpaceInput } from './model/ISpaceApi'
export { SpaceApiProvider, useSpaceApi } from './model/spaceApiContext'
export { SPACES_QUERY_KEY } from './model/queryKeys'
export { spaceDetailKey, useSpaceDetail } from './model/useSpaceDetail'
export { spaceInvitationsKey, useSpaceInvitations } from './model/useSpaceInvitations'
export { RECEIVED_INVITATIONS_QUERY_KEY, useReceivedInvitations } from './model/useReceivedInvitations'
export { useHasPendingInvitations } from './model/useHasPendingInvitations'
export {
  useCreateSpace, useUpdateSpace, useDeleteSpace, useChangeMemberRole, useRemoveMember,
  useTransferOwnership, useLeaveSpace, useInviteMember, useRevokeInvitation, useAcceptInvitation,
} from './model/useSpaceMutations'
export { spaceApi } from './api/spaceApi'
export {
  SpaceNotAccessibleError, InsufficientRoleError, SelfManagementError, OwnerProtectedError,
  SpaceRoleAlreadyAssignedError, LastOwnerError, AlreadyMemberError, InvitationAlreadyPendingError,
  InvitationNotFoundError, MemberNotFoundError, InvitationNotPendingError, InvitationExpiredError,
  PersonalSpaceImmutableError, NoAccountForEmailError, InvalidAppearanceError, InvalidSpaceNameError,
  InvalidSpaceDescriptionError, OwnerRoleNotAssignableError,
} from './api/spaceApi'
