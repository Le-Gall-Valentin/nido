import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Dialog, Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { User, AdminUser } from '@/entities/user'
import { assignableRoles } from '../lib/permissions'
import { mapApiErrorToKey } from '../lib/mapApiErrorToKey'

interface EditUserRoleModalProps {
  target: AdminUser
  caller: User
  onClose: () => void
  onUpdate: (id: string, role: 'USER' | 'ADMIN') => Promise<void>
  onSuccess: (newRole: 'USER' | 'ADMIN') => void
}

export function EditUserRoleModal({ target, caller, onClose, onUpdate, onSuccess }: EditUserRoleModalProps) {
  const { t } = useTranslation('adminUsers')

  const availableRoles = assignableRoles(caller.role)

  // A SUPER_ADMIN is never a manageable target (backend canManage rejects it);
  // the modal must stay non-actionable for one even if it is ever rendered,
  // rather than coercing the role to USER and enabling a demotion.
  const isEditableTarget = target.role !== 'SUPER_ADMIN'
  const initialRole: 'USER' | 'ADMIN' = target.role === 'SUPER_ADMIN' ? 'USER' : target.role

  const [role, setRole] = useState<'USER' | 'ADMIN'>(initialRole)
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const pendingRef = useRef(false)

  // Backend returns 409 RoleAlreadyAssigned on a no-op — block it upfront.
  const submitDisabled = !isEditableTarget || role === target.role

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (submitDisabled || pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await onUpdate(target.id, role)
      onSuccess(role)
    } catch (error) {
      setErrorKey(mapApiErrorToKey(error, 'edit_role'))
    } finally {
      pendingRef.current = false
      setIsLoading(false)
    }
  }

  function handleClose() {
    setErrorKey(null)
    onClose()
  }

  return (
    <Dialog open onClose={handleClose} title={t('edit_role.title', { username: target.username })}>
      <div className="mb-5">
        <h3 className="text-base font-semibold text-fg-0">
          {t('edit_role.title', { username: target.username })}
        </h3>
      </div>
      <form onSubmit={handleSubmit}>
        <div className="mb-1 flex flex-col gap-2">
          <label htmlFor="edit-role" className="text-xs font-medium text-fg-1">
            {t('edit_role.role')}
          </label>
          <select
            id="edit-role"
            value={role}
            onChange={e => setRole(e.target.value as 'USER' | 'ADMIN')}
            disabled={isLoading}
            className="w-full rounded-lg border border-border bg-bg-1 px-3.5 py-3 text-sm text-fg-0 outline-none transition-all hover:border-border-2 focus:border-accent focus:bg-bg-2 focus:shadow-[0_0_0_3px_var(--color-accent-ring)] disabled:opacity-50"
          >
            {availableRoles.map(r => (
              <option key={r} value={r}>{t(`user.role.${r}`, { ns: 'shell' })}</option>
            ))}
          </select>
        </div>

        {errorKey && (
          <Alert variant="error" className="mt-3">{t(errorKey)}</Alert>
        )}

        <div className="flex justify-end gap-2 mt-5">
          <Button type="button" onClick={handleClose} disabled={isLoading}>
            {t('edit_role.cancel')}
          </Button>
          <Button
            type="submit"
            disabled={submitDisabled}
            isLoading={isLoading}
            className="border-transparent font-semibold"
            style={CTA_BUTTON_STYLE}
          >
            {t('edit_role.submit')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
