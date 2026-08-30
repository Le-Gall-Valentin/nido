import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ArrowRightLeft } from 'lucide-react'
import { Alert, Dialog, Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import { SpaceAvatar } from './SpaceAvatar'
import type { SpaceSummary } from '../model/types'

export type TransferOperation = 'copy' | 'move'

export type TransferDestination = Pick<SpaceSummary, 'id' | 'name' | 'accent' | 'glyph'>

interface TransferDialogProps {
  itemName: string
  operation: TransferOperation
  destinations: TransferDestination[]
  onClose: () => void
  onConfirm: (destinationId: string) => Promise<void>
}

/**
 * Generic "copy/move this item to another context" modal — the reusable
 * half of the cross-context transfer mechanism (see
 * docs/superpowers/specs/2026-08-29-cross-context-transfer-design.md).
 * Entirely driven by props: it knows nothing about recipes or any other
 * item kind, only that something named `itemName` is being sent to one of
 * `destinations`. Every future item type that supports transfer reuses this
 * component as-is — only the page wiring (which spaces are offered, which
 * API call runs) differs per item type.
 */
export function TransferDialog({ itemName, operation, destinations, onClose, onConfirm }: TransferDialogProps) {
  const { t } = useTranslation('common')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [hasError, setHasError] = useState(false)
  const pendingRef = useRef(false)

  async function handleSubmit() {
    if (pendingRef.current || !selectedId) return
    pendingRef.current = true
    setIsLoading(true)
    setHasError(false)
    try {
      await onConfirm(selectedId)
      onClose()
    } catch {
      setHasError(true)
    } finally {
      pendingRef.current = false
      setIsLoading(false)
    }
  }

  const titleKey = operation === 'copy' ? 'transfer.copy_title' : 'transfer.move_title'
  const submitKey = operation === 'copy' ? 'transfer.copy_submit' : 'transfer.move_submit'
  const title = t(titleKey, { name: itemName })

  return (
    <Dialog open onClose={onClose} title={title}>
      <h3 className="mb-4 text-[19px] font-semibold text-fg-0">{title}</h3>

      {destinations.length === 0 ? (
        <p className="mb-5 text-sm text-fg-3">{t('transfer.no_destination')}</p>
      ) : (
        <ul className="mb-5 flex flex-col gap-1.5">
          {destinations.map((destination) => (
            <li key={destination.id}>
              <button
                type="button"
                onClick={() => setSelectedId(destination.id)}
                aria-pressed={selectedId === destination.id}
                className={`flex w-full items-center gap-[11px] rounded-[10px] border-[1.5px] p-2.5 text-left transition-colors ${
                  selectedId === destination.id ? 'border-accent bg-accent-dim' : 'border-border hover:bg-bg-2'
                }`}
              >
                <SpaceAvatar space={destination} size="sm" />
                <span className="min-w-0 flex-1 truncate text-sm font-medium text-fg-0">{destination.name}</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      {hasError && <Alert variant="error" className="mb-4">{t('transfer.error')}</Alert>}

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={onClose} disabled={isLoading}>
          {t('transfer.cancel')}
        </Button>
        <Button
          onClick={() => { void handleSubmit() }}
          isLoading={isLoading}
          disabled={!selectedId}
          style={CTA_BUTTON_STYLE}
        >
          <ArrowRightLeft className="size-4" />
          {t(submitKey)}
        </Button>
      </div>
    </Dialog>
  )
}
