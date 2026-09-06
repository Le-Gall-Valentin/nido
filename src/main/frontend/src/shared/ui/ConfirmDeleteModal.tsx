import type { ReactNode } from 'react'
import { AlertTriangle, Trash2 } from 'lucide-react'
import { Alert } from './Alert'
import { Dialog } from './Dialog'
import { Button } from './Button'

interface ConfirmDeleteModalProps {
  title: string
  message: string
  confirmLabel: string
  cancelLabel: string
  onConfirm: () => void
  onCancel: () => void
  isPending: boolean
  error?: string | null
  children?: ReactNode
}

/**
 * Generic delete-confirmation dialog: owns only the layout (icon, title,
 * message, error alert, cancel/confirm buttons). Mutation lifecycle and
 * error-mapping stay with the caller, since those differ per domain.
 */
export function ConfirmDeleteModal({
  title, message, confirmLabel, cancelLabel, onConfirm, onCancel, isPending, error = null, children,
}: ConfirmDeleteModalProps) {
  return (
    <Dialog open onClose={onCancel} title={title}>
      <div className="mb-[15px] grid size-[46px] place-items-center rounded-[13px] bg-status-red-dim text-status-red">
        <AlertTriangle className="size-6" />
      </div>
      <div className="mb-5">
        <h3 className="mb-2 text-[19px] font-semibold text-fg-0">{title}</h3>
        <p className="text-sm leading-relaxed text-fg-2">{message}</p>
      </div>

      {children}

      {error && <Alert variant="error" className="mb-4">{error}</Alert>}

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={onCancel} disabled={isPending}>{cancelLabel}</Button>
        <Button
          onClick={onConfirm}
          isLoading={isPending}
          className="border-transparent font-semibold !text-bg-0 transition hover:brightness-90"
          style={{ background: 'var(--color-status-red)' }}
        >
          <Trash2 className="size-4" />
          {confirmLabel}
        </Button>
      </div>
    </Dialog>
  )
}
