import { useTranslation } from 'react-i18next'
import { ColorSwatches, type EventColor } from '@/entities/calendar'

/**
 * The colours of events: a followed calendar wears one of them, never a source's. Given the name of the calendar
 * being edited, each colour says whose colour it sets.
 */
export function FeedColorPicker({ value, onChange, feedName }: {
  value: EventColor
  onChange: (color: EventColor) => void
  feedName?: string
}) {
  const { t } = useTranslation('calendarSync')
  return (
    <ColorSwatches label={t('feeds.color')} value={value} onChange={(color) => color && onChange(color)}
      describe={feedName === undefined ? undefined : (color) => t('feeds.color_for', { color, name: feedName })} />
  )
}
