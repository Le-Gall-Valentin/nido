import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle, Trash2 } from 'lucide-react'
import { Alert } from './Alert'
import { Dialog } from './Dialog'
import { Button } from './Button'

interface ConfirmDeleteModalProps {
  /** Names the thing being deleted — the one sentence only the caller can write. */
  title: string
  onConfirm: () => void
  onCancel: () => void
  isPending: boolean
  /** Overrides the generic warning, for a domain with something specific to say. */
  message?: string
  confirmLabel?: string
  cancelLabel?: string
  error?: string | null
  children?: ReactNode
}

/**
 * Generic delete-confirmation dialog: owns the layout, and now its own words.
 *
 * <p>"Cancel", "Delete", "This cannot be undone" and "Something went wrong" were written out in three
 * page namespaces — finance, tasks and kitchen each carried their own copy of the same five strings,
 * and every caller passed all four down. A shared component whose vocabulary lives in a page's
 * translations is the same shape of defect as a store importing the HTTP client: the thing that owns
 * the behaviour did not own what it says.
 *
 * <p>Callers now pass the title, because only they know what is being deleted, and override a message
 * only when their domain has something particular to warn about — as the kitchen does about a recipe's
 * ingredients. Mutation lifecycle and error mapping stay with the caller, since those do differ.
 */
export function ConfirmDeleteModal({
  title, onConfirm, onCancel, isPending, message, confirmLabel, cancelLabel, error = null, children,
}: ConfirmDeleteModalProps) {
  const { t } = useTranslation('common')
  return (
    <Dialog open onClose={onCancel} title={title}>
      <div className="mb-[15px] grid size-[46px] place-items-center rounded-[13px] bg-status-red-dim text-status-red">
        <AlertTriangle className="size-6" />
      </div>
      <div className="mb-5">
        <h3 className="mb-2 text-[19px] font-semibold text-fg-0">{title}</h3>
        <p className="text-sm leading-relaxed text-fg-2">{message ?? t('delete_confirm.message')}</p>
      </div>

      {children}

      {error && <Alert variant="error" className="mb-4">{error}</Alert>}

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={onCancel} disabled={isPending}>
          {cancelLabel ?? t('delete_confirm.cancel')}
        </Button>
        <Button
          onClick={onConfirm}
          isLoading={isPending}
          className="border-transparent font-semibold !text-bg-0 transition hover:brightness-90"
          style={{ background: 'var(--color-status-red)' }}
        >
          <Trash2 className="size-4" />
          {confirmLabel ?? t('delete_confirm.confirm')}
        </Button>
      </div>
    </Dialog>
  )
}
