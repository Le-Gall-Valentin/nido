import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Input } from '@/shared/ui'
import { FEED_FALLBACK_COLOR, MAX_FEED_ADDRESS_LENGTH, type EventColor, type ExternalFeedInput } from '@/entities/calendar'
import { FeedColorPicker } from './FeedColorPicker'

/**
 * The calendar to follow: its address, a name if not its own, a colour. Mounted anew for each follow, so a form never
 * opens with the last one's values — its colour included.
 */
export function FollowFeedForm({ error, onChange }: {
  error: string | null
  onChange: (input: ExternalFeedInput) => void
}) {
  const { t } = useTranslation('calendarSync')
  const [url, setUrl] = useState('')
  const [name, setName] = useState('')
  // The graphite of external sources until another is picked: a calendar followed in violet would pass for the space's own.
  const [color, setColor] = useState<EventColor>(FEED_FALLBACK_COLOR)
  const change = (next: { url?: string; name?: string; color?: EventColor }) => {
    const merged = { url, name, color, ...next }
    setUrl(merged.url)
    setName(merged.name)
    setColor(merged.color)
    onChange({ url: merged.url.trim(), name: merged.name.trim() || null, color: merged.color })
  }
  return (
    <>
      <Input label={t('feeds.url')} placeholder={t('feeds.url_placeholder')} value={url} inputMode="url" data-feed-url
        maxLength={MAX_FEED_ADDRESS_LENGTH} onChange={(e) => change({ url: e.target.value })} />
      <Input label={t('feeds.name')} value={name} maxLength={100} onChange={(e) => change({ name: e.target.value })} />
      <FeedColorPicker value={color} onChange={(next) => change({ color: next })} />
      {error && <p role="alert" className="text-sm text-status-red">{error}</p>}
    </>
  )
}
