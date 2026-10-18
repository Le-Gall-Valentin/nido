import { useTranslation } from 'react-i18next'
import { EVENT_DOT_CLASS, feedColor, type CalendarSourceType, type ExternalFeed } from '@/entities/calendar'
import { SOURCE_ORDER, dotClassForSource } from '../lib/sourceAppearance'

interface SourceFiltersProps {
  isEnabled: (source: CalendarSourceType) => boolean
  onToggle: (source: CalendarSourceType) => void
  /** The calendars the space follows, each filtered on its own. */
  feeds: ExternalFeed[]
  isFeedEnabled: (feedId: string) => boolean
  onToggleFeed: (feedId: string) => void
}

/**
 * Five toggles, one per source, then one per followed calendar. Real buttons with aria-pressed rather than styled divs,
 * so the on/off state is announced rather than only coloured — the colour is the whole affordance here.
 */
export function SourceFilters({ isEnabled, onToggle, feeds, isFeedEnabled, onToggleFeed }: SourceFiltersProps) {
  const { t } = useTranslation('calendar')
  return (
    <div role="group" aria-label={t('filters.label')} className="flex flex-wrap gap-1.5">
      {SOURCE_ORDER.map((source) => {
        const on = isEnabled(source)
        return (
          <button
            key={source}
            type="button"
            aria-pressed={on}
            onClick={() => onToggle(source)}
            className={`flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-semibold
              ${on ? 'border-border-2 bg-bg-1 text-fg-1' : 'border-border bg-transparent text-fg-4'}`}
          >
            <span className={`size-1.5 rounded-full ${on ? dotClassForSource(source) : 'bg-fg-4'}`} />
            {t(`source.${source}`)}
          </button>
        )
      })}
      {feeds.map((feed) => {
        const on = isFeedEnabled(feed.id)
        return (
          <button
            key={feed.id}
            type="button"
            aria-pressed={on}
            onClick={() => onToggleFeed(feed.id)}
            className={`flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-semibold
              ${on ? 'border-border-2 bg-bg-1 text-fg-1' : 'border-border bg-transparent text-fg-4'}`}
          >
            <span aria-hidden className={`size-1.5 rounded-full ${on ? EVENT_DOT_CLASS[feedColor(feed.color)] : 'bg-fg-4'}`} />
            {feed.name}
          </button>
        )
      })}
    </div>
  )
}
