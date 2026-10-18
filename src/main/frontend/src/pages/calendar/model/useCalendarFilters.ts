import { useCallback, useMemo, useState } from 'react'
import type { CalendarOccurrence, CalendarSourceType } from '@/entities/calendar'
import { SOURCE_ORDER } from '../lib/sourceAppearance'

const storageKey = (spaceId: string) => `nido.calendar.filters.${spaceId}`

/**
 * localStorage throws outright in a private window and returns nothing when site data is cleared.
 * A filter preference must never be able to break the whole page, so every access is guarded and
 * every failure falls back to "everything visible".
 */
function readDisabled(spaceId: string): Set<CalendarSourceType> {
  try {
    const raw = localStorage.getItem(storageKey(spaceId))
    if (!raw) return new Set()
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) return new Set()
    return new Set(parsed.filter((s): s is CalendarSourceType =>
      typeof s === 'string' && (SOURCE_ORDER as string[]).includes(s)))
  } catch {
    return new Set()
  }
}

function writeDisabled(spaceId: string, disabled: Set<CalendarSourceType>): void {
  try {
    localStorage.setItem(storageKey(spaceId), JSON.stringify([...disabled]))
  } catch {
    // A preference we cannot persist is still a preference for this session.
  }
}

const feedsKey = (spaceId: string) => `nido.calendar.feeds.${spaceId}`

function readDisabledFeeds(spaceId: string): Set<string> {
  try {
    const parsed: unknown = JSON.parse(localStorage.getItem(feedsKey(spaceId)) ?? '[]')
    return new Set(Array.isArray(parsed) ? parsed.filter((id): id is string => typeof id === 'string') : [])
  } catch {
    return new Set()
  }
}

function writeDisabledFeeds(spaceId: string, disabled: Set<string>): void {
  try {
    localStorage.setItem(feedsKey(spaceId), JSON.stringify([...disabled]))
  } catch {
    // A preference we cannot persist is still a preference for this session.
  }
}

export interface CalendarFilters {
  enabled: Set<CalendarSourceType>
  isEnabled: (source: CalendarSourceType) => boolean
  toggle: (source: CalendarSourceType) => void
  /** A followed calendar is filtered on its own, not with the others of its kind. */
  isFeedEnabled: (feedId: string) => boolean
  toggleFeed: (feedId: string) => void
  filter: (occurrences: CalendarOccurrence[]) => CalendarOccurrence[]
}

export function useCalendarFilters(spaceId: string): CalendarFilters {
  const [disabled, setDisabled] = useState<Set<CalendarSourceType>>(() => readDisabled(spaceId))

  const toggle = useCallback((source: CalendarSourceType) => {
    setDisabled((current) => {
      const next = new Set(current)
      if (next.has(source)) next.delete(source)
      else next.add(source)
      writeDisabled(spaceId, next)
      return next
    })
  }, [spaceId])

  const isEnabled = useCallback((source: CalendarSourceType) => !disabled.has(source), [disabled])

  const [disabledFeeds, setDisabledFeeds] = useState<Set<string>>(() => readDisabledFeeds(spaceId))

  const toggleFeed = useCallback((feedId: string) => {
    setDisabledFeeds((current) => {
      const next = new Set(current)
      if (next.has(feedId)) next.delete(feedId)
      else next.add(feedId)
      writeDisabledFeeds(spaceId, next)
      return next
    })
  }, [spaceId])

  const isFeedEnabled = useCallback((feedId: string) => !disabledFeeds.has(feedId), [disabledFeeds])

  const filter = useCallback(
    (occurrences: CalendarOccurrence[]) => occurrences.filter((o) => o.source === 'EXTERNAL'
      ? !disabledFeeds.has(o.externalFeedId ?? '')
      : !disabled.has(o.source)),
    [disabled, disabledFeeds])

  const enabled = useMemo(
    () => new Set(SOURCE_ORDER.filter((s) => !disabled.has(s))),
    [disabled])

  return useMemo(
    () => ({ enabled, isEnabled, toggle, isFeedEnabled, toggleFeed, filter }),
    [enabled, isEnabled, toggle, isFeedEnabled, toggleFeed, filter])
}
