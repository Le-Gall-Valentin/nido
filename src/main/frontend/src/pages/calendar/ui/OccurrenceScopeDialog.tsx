import { useTranslation } from 'react-i18next'
import { Dialog } from '@/shared/ui'
import type { OccurrenceScope } from '../model/useCalendarDialog'

interface OccurrenceScopeDialogProps {
  /** What the caller is about to do — the wording differs for an edit and a deletion. */
  action: 'edit' | 'delete'
  onChoose: (scope: OccurrenceScope) => void
  onCancel: () => void
}

/**
 * "This one, or all of them?" — asked before any edit or deletion that lands on an occurrence of
 * a series.
 *
 * It exists because the two answers are genuinely different operations, not two shades of one:
 * one writes an exception for a single slot, the other rewrites the template every slot comes
 * from. Guessing between them silently is how a calendar loses a year of appointments.
 */
export function OccurrenceScopeDialog({ action, onChoose, onCancel }: OccurrenceScopeDialogProps) {
  const { t } = useTranslation('calendar')
  return (
    <Dialog open onClose={onCancel} title={t(`scope.${action}_title`)} showCloseButton>
      <p className="mb-3 pr-8 text-sm text-fg-1">{t(`scope.${action}_question`)}</p>
      <div className="flex flex-col gap-1">
        <button type="button" onClick={() => onChoose('occurrence')}
          className="rounded-md px-3 py-2 text-left text-sm text-fg-1 hover:bg-bg-2">
          {t('scope.this_occurrence')}
        </button>
        <button type="button" onClick={() => onChoose('series')}
          className="rounded-md px-3 py-2 text-left text-sm text-fg-1 hover:bg-bg-2">
          {t('scope.whole_series')}
        </button>
      </div>
    </Dialog>
  )
}
