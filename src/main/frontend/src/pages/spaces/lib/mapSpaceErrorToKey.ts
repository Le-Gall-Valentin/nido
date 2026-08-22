import { NetworkError, RateLimitError } from '@/shared/lib'
import { SpaceNotAccessibleError } from '@/features/space-switcher'
import {
  InsufficientRoleError,
  SelfManagementError,
  OwnerProtectedError,
  SpaceRoleAlreadyAssignedError,
  LastOwnerError,
  AlreadyMemberError,
  InvitationAlreadyPendingError,
  InvitationNotFoundError,
  InvitationNotPendingError,
  InvitationExpiredError,
  PersonalSpaceImmutableError,
  NoAccountForEmailError,
} from '../api/spacesPageApi'

/**
 * Maps an API error to a translation key under the given namespace prefix.
 * Every space error namespace shares the same suffixes, so no technical
 * message ever reaches the user.
 */
export function mapSpaceErrorToKey(error: unknown, prefix: string): string {
  if (error instanceof SpaceNotAccessibleError) return `${prefix}.error.not_accessible`
  if (error instanceof InsufficientRoleError) return `${prefix}.error.insufficient_role`
  if (error instanceof OwnerProtectedError) return `${prefix}.error.owner_protected`
  if (error instanceof SelfManagementError) return `${prefix}.error.self_management`
  if (error instanceof SpaceRoleAlreadyAssignedError) return `${prefix}.error.already_assigned`
  if (error instanceof LastOwnerError) return `${prefix}.error.last_owner`
  if (error instanceof PersonalSpaceImmutableError) return `${prefix}.error.personal_immutable`
  if (error instanceof NoAccountForEmailError) return `${prefix}.error.no_account`
  if (error instanceof AlreadyMemberError) return `${prefix}.error.already_member`
  if (error instanceof InvitationAlreadyPendingError) return `${prefix}.error.invitation_pending`
  if (error instanceof InvitationNotFoundError) return `${prefix}.error.invitation_not_found`
  if (error instanceof InvitationNotPendingError) return `${prefix}.error.invitation_not_pending`
  if (error instanceof InvitationExpiredError) return `${prefix}.error.invitation_expired`
  if (error instanceof RateLimitError) return `${prefix}.error.rate_limit`
  if (error instanceof NetworkError) return `${prefix}.error.network`
  return `${prefix}.error.server`
}
