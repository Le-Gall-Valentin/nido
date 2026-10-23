import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Input } from '@/shared/ui'
import { feedColor, type EventColor, type ExternalFeed } from '@/entities/calendar'
import { BusyButton } from './BusyButton'
import { FeedColorPicker } from './FeedColorPicker'

/** Renames a followed calendar and changes its colour, in place of its row. */
export function FeedEditor({ feed, pending, error, onSave, onCancel }: {
  feed: ExternalFeed
  pending: boolean
  error: string | null
  onSave: (name: string, color: EventColor) => void
  onCancel: () => void
}) {
  const { t } = useTranslation('calendarSync')
  const [name, setName] = useState(feed.name)
  const [color, setColor] = useState<EventColor>(feedColor(feed.color))
  return (
    <li className="flex flex-col gap-3 rounded-lg bg-bg-2 px-3 py-2">
      <Input label={t('feeds.rename')} value={name} maxLength={100} data-rename onChange={(e) => setName(e.target.value)} />
      <FeedColorPicker value={color} onChange={setColor} feedName={feed.name} />
      {error && <p role="alert" className="text-sm text-status-red">{error}</p>}
      <div className="flex justify-end gap-2">
        {/* Not while the change is saved: it would be saved all the same, thought dropped. */}
        <Button type="button" disabled={pending} onClick={onCancel}>{t('feeds.cancel')}</Button>
        <BusyButton type="button" variant="primary" disabled={!name.trim()} busy={pending}
          onClick={() => onSave(name.trim(), color)}>
          {t('feeds.update')}
        </BusyButton>
      </div>
    </li>
  )
}
