import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE, Input } from '@/shared/ui'
import type { ExportableSource, SubscriptionLinkInput } from '@/entities/calendar'
import { OnlyMineToggle, SourceChecklist } from './SourceChecklist'

/** A new subscription address: what the device is called, and what it reads. */
export function NewLinkForm({ isPersonal, pending, error, onCreate, onCancel }: {
  isPersonal: boolean
  pending: boolean
  error: string | null
  onCreate: (input: SubscriptionLinkInput) => void
  onCancel: () => void
}) {
  const { t } = useTranslation('calendarSync')
  const [label, setLabel] = useState('')
  const [sources, setSources] = useState<ExportableSource[]>(['EVENT'])
  const [onlyMine, setOnlyMine] = useState(false)
  return (
    <>
      <Input label={t('subscribe.label')} placeholder={t('subscribe.label_placeholder')} value={label} maxLength={60}
        data-link-label onChange={(e) => setLabel(e.target.value)} />
      <SourceChecklist selected={sources} onChange={setSources} />
      {!isPersonal && <OnlyMineToggle checked={onlyMine} onChange={setOnlyMine} />}
      {error && <p role="alert" className="text-sm text-status-red">{error}</p>}
      <div className="flex justify-end gap-2">
        {/* Not while the address is being made: it would be made all the same, never shown, and count toward ten. */}
        <Button type="button" disabled={pending} onClick={onCancel}>{t('subscribe.cancel')}</Button>
        <Button type="button" style={CTA_BUTTON_STYLE} disabled={!label.trim() || sources.length === 0 || pending}
          onClick={() => onCreate({ label: label.trim(), sources, onlyMine })}>
          {t('subscribe.create')}
        </Button>
      </div>
    </>
  )
}
