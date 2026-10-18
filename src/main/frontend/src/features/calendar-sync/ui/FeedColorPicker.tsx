import { useTranslation } from 'react-i18next'
import { EVENT_COLORS, EVENT_DOT_CLASS, type EventColor } from '@/entities/calendar'

/**
 * The colours of events: a followed calendar wears one of them, never a source's. Given the name of the calendar
 * being edited, each button says whose colour it sets.
 */
export function FeedColorPicker({ value, onChange, feedName }: {
  value: EventColor
  onChange: (color: EventColor) => void
  feedName?: string
}) {
  const { t } = useTranslation('calendarSync')
  return (
    <div>
      <span className="mb-1 block text-xs font-semibold text-fg-2">{t('feeds.color')}</span>
      <div className="flex gap-1.5">
        {EVENT_COLORS.map((color) => (
          <button key={color} type="button" aria-pressed={value === color} onClick={() => onChange(color)}
            aria-label={feedName === undefined ? t(`colors.${color}`) : t('feeds.color_for', { color: t(`colors.${color}`), name: feedName })}
            className={`size-6 rounded-full border-2 ${value === color ? 'border-fg-1' : 'border-transparent'} ${EVENT_DOT_CLASS[color]}`} />
        ))}
      </div>
    </div>
  )
}
