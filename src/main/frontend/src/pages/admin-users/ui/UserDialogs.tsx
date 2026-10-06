import type { AdminUser, User } from '@/entities/user'
import { useCreateUser, useDeleteUser, useResendInvitation, useResetTotp, useUpdateUserRole } from '@/entities/user'
import type { MailAvailability } from '@/entities/capabilities'
import { CreateUserModal } from './CreateUserModal'
import { EditUserRoleModal } from './EditUserRoleModal'
import { DeleteUserModal } from './DeleteUserModal'
import { ResetTotpModal } from './ResetTotpModal'
import { ResendInvitationModal } from './ResendInvitationModal'
import { DeactivateUserModal } from './DeactivateUserModal'

/** The one dialog the administration has open, if any, with the account it is about. */
export type UserDialog =
  | { kind: 'create' }
  | { kind: 'edit_role' | 'delete' | 'reset_totp' | 'resend' | 'deactivate'; user: AdminUser }
  | null

interface UserDialogsProps {
  dialog: UserDialog
  caller: User
  mail: MailAvailability
  onClose: () => void
  /** A new account is listed first: the page goes back to its start. */
  onCreated: () => void
  /** The page may have lost its last row. */
  onDeleted: () => void
  /** Shared with the row's switch, whose page it updates at once. */
  onDeactivate: (user: AdminUser) => Promise<void>
}

/** Opens the dialog the page asked for, and sends what it confirms. */
export function UserDialogs({ dialog, caller, mail, onClose, onCreated, onDeleted, onDeactivate }: UserDialogsProps) {
  const createUser = useCreateUser()
  const updateUserRole = useUpdateUserRole()
  const deleteUser = useDeleteUser()
  const resetTotp = useResetTotp()
  const resendInvitation = useResendInvitation()

  if (!dialog) return null
  switch (dialog.kind) {
    case 'create':
      return (
        <CreateUserModal
          caller={caller}
          onClose={onClose}
          onCreate={(username, email, role) => createUser.mutateAsync({ username, email, role })}
          onSuccess={onCreated}
        />
      )
    case 'edit_role':
      return (
        <EditUserRoleModal
          target={dialog.user}
          caller={caller}
          mail={mail}
          onClose={onClose}
          onUpdate={(id, role) => updateUserRole.mutateAsync({ id, role })}
          onSuccess={onClose}
        />
      )
    case 'delete':
      return <DeleteUserModal user={dialog.user} mail={mail} onClose={onClose} onDelete={deleteUser.mutateAsync} onSuccess={onDeleted} />
    case 'reset_totp':
      return <ResetTotpModal user={dialog.user} mail={mail} onClose={onClose} onReset={resetTotp.mutateAsync} onSuccess={onClose} />
    case 'resend':
      return <ResendInvitationModal user={dialog.user} mail={mail} onClose={onClose} onResend={resendInvitation.mutateAsync} />
    case 'deactivate':
      return <DeactivateUserModal user={dialog.user} mail={mail} onClose={onClose} onDeactivate={onDeactivate} onSuccess={onClose} />
  }
}
