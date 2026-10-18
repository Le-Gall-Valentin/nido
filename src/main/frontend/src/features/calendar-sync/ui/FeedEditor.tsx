import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE, Input } from '@/shared/ui'
import { isEventColor, type EventColor, type ExternalFeed } from '@/entities/calendar'
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
  const [color, setColor] = useState<EventColor>(isEventColor(feed.color) ? feed.color : 'event-graphite')
  return (
    <li className="flex flex-col gap-3 rounded-lg bg-bg-2 px-3 py-2">
      <Input label={t('feeds.rename')} value={name} maxLength={100} data-rename onChange={(e) => setName(e.target.value)} />
      <FeedColorPicker value={color} onChange={setColor} feedName={feed.name} />
      {error && <p role="alert" className="text-sm text-status-red">{error}</p>}
      <div className="flex justify-end gap-2">
        <Button type="button" onClick={onCancel}>{t('feeds.cancel')}</Button>
        <Button type="button" style={CTA_BUTTON_STYLE} disabled={!name.trim() || pending} onClick={() => onSave(name.trim(), color)}>
          {t('feeds.update')}
        </Button>
      </div>
    </li>
  )
}
