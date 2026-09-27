import { useTranslation } from 'react-i18next'
import { Dialog, Spinner } from '@/shared/ui'
import { useShoppingList } from '@/entities/kitchen'
import { ExportToShoppingListModal } from '@/features/export-menu-to-shopping-list'

interface ExportWeekDialogProps {
  spaceId: string
  from: string
  to: string
  onClose: () => void
}

/**
 * The menu page's "send to the shopping list", for the next seven days. Mounted only when asked for,
 * so the dashboard never computes a shopping list nobody opens; the modal itself is mounted only once
 * the lines are there, because it seeds its drafts from them on mount.
 */
export function ExportWeekDialog({ spaceId, from, to, onClose }: ExportWeekDialogProps) {
  const { t } = useTranslation('dashboard')
  const { data, isPending, isError } = useShoppingList(spaceId, from, to)

  if (isPending) {
    return (
      <Dialog open onClose={onClose} title={t('menu.send_week')} showCloseButton>
        <Spinner label={t('loading')} fullscreen={false} />
      </Dialog>
    )
  }
  if (isError || data.length === 0) {
    return (
      <Dialog open onClose={onClose} title={t('menu.send_week')} showCloseButton>
        <p className="pr-8 text-sm text-fg-2">{isError ? t('menu.export_failed') : t('menu.export_empty')}</p>
      </Dialog>
    )
  }
  return <ExportToShoppingListModal open onClose={onClose} spaceId={spaceId} shoppingList={data} onImported={onClose} />
}
