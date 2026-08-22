import { NetworkError, RateLimitError } from '@/shared/lib'
import { SpaceNotAccessibleError } from '@/features/space-switcher'
import {
  InsufficientRoleError,
  SelfManagementError,
  OwnerProtectedError,
  SpaceRoleAlreadyAssignedError,
  LastOwnerError,
  MemberNotFoundError,
  AlreadyMemberError,
  InvitationAlreadyPendingError,
  InvitationNotFoundError,
  InvitationNotPendingError,
  InvitationExpiredError,
  PersonalSpaceImmutableError,
  NoAccountForEmailError,
} from '../api/spacesPageApi'

function suffixFor(error: unknown): string {
  if (error instanceof SpaceNotAccessibleError) return 'not_accessible'
  if (error instanceof MemberNotFoundError) return 'member_not_found'
  if (error instanceof InsufficientRoleError) return 'insufficient_role'
  if (error instanceof OwnerProtectedError) return 'owner_protected'
  if (error instanceof SelfManagementError) return 'self_management'
  if (error instanceof SpaceRoleAlreadyAssignedError) return 'already_assigned'
  if (error instanceof LastOwnerError) return 'last_owner'
  if (error instanceof PersonalSpaceImmutableError) return 'personal_immutable'
  if (error instanceof NoAccountForEmailError) return 'no_account'
  if (error instanceof AlreadyMemberError) return 'already_member'
  if (error instanceof InvitationAlreadyPendingError) return 'invitation_pending'
  if (error instanceof InvitationNotFoundError) return 'invitation_not_found'
  if (error instanceof InvitationNotPendingError) return 'invitation_not_pending'
  if (error instanceof InvitationExpiredError) return 'invitation_expired'
  if (error instanceof RateLimitError) return 'rate_limit'
  if (error instanceof NetworkError) return 'network'
  return 'server'
}

/**
 * Maps an API error to an ordered list of translation keys: an operation-
 * specific override under the given namespace prefix, falling back to the
 * shared `errors.<suffix>` group. i18next's `t()` accepts a key array and
 * resolves the first one that has a translation, so an operation only needs
 * its own `${prefix}.error.<suffix>` entry when its wording genuinely
 * differs from the neutral one — every other suffix resolves through
 * `errors.<suffix>`, so no technical message ever reaches the user.
 */
export function mapSpaceErrorToKey(error: unknown, prefix: string): string[] {
  const suffix = suffixFor(error)
  return [`${prefix}.error.${suffix}`, `errors.${suffix}`]
}
