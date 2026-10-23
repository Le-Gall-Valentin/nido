import { useTranslation } from 'react-i18next'
import { EVENT_DOT_CLASS, feedColor, type CalendarSourceType, type ExternalFeed } from '@/entities/calendar'
import { SOURCE_ORDER, dotClassForSource } from '../lib/sourceAppearance'
import { FilterChip } from './FilterChip'

interface SourceFiltersProps {
  isEnabled: (source: CalendarSourceType) => boolean
  onToggle: (source: CalendarSourceType) => void
  /** The calendars the space follows, each filtered on its own. */
  feeds: ExternalFeed[]
  isFeedEnabled: (feedId: string) => boolean
  onToggleFeed: (feedId: string) => void
}

/** One toggle per source, then one per followed calendar, in its own colour. */
export function SourceFilters({ isEnabled, onToggle, feeds, isFeedEnabled, onToggleFeed }: SourceFiltersProps) {
  const { t } = useTranslation('calendar')
  return (
    <div role="group" aria-label={t('filters.label')} className="flex flex-wrap gap-1.5">
      {SOURCE_ORDER.map((source) => (
        <FilterChip key={source} on={isEnabled(source)} dotClass={dotClassForSource(source)} label={t(`source.${source}`)}
          onToggle={() => onToggle(source)} />
      ))}
      {feeds.map((feed) => (
        <FilterChip key={feed.id} on={isFeedEnabled(feed.id)} dotClass={EVENT_DOT_CLASS[feedColor(feed.color)]} label={feed.name}
          onToggle={() => onToggleFeed(feed.id)} />
      ))}
    </div>
  )
}
