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

/**
 * What a space shows of its sources and followed calendars, kept per space in localStorage. The page stays mounted
 * when another space is chosen: what is held belongs to one space, and is read again for the next — never carried
 * over, nor written under the other's key. `feedIds`: the calendars the space follows, once known; a calendar no
 * longer followed is dropped from what is kept.
 */
export function useCalendarFilters(spaceId: string, feedIds?: string[]): CalendarFilters {
  const [sources, setSources] = useState(() => ({ spaceId, disabled: readDisabled(spaceId) }))
  const [feeds, setFeeds] = useState(() => ({ spaceId, disabled: readDisabledFeeds(spaceId) }))
  // Another space: what this one hid is read, as React would on a first render.
  if (sources.spaceId !== spaceId) setSources({ spaceId, disabled: readDisabled(spaceId) })
  if (feeds.spaceId !== spaceId) setFeeds({ spaceId, disabled: readDisabledFeeds(spaceId) })
  const disabled = sources.spaceId === spaceId ? sources.disabled : readDisabled(spaceId)
  const disabledFeeds = feeds.spaceId === spaceId ? feeds.disabled : readDisabledFeeds(spaceId)

  const toggle = useCallback((source: CalendarSourceType) => {
    setSources((current) => {
      const next = new Set(current.spaceId === spaceId ? current.disabled : readDisabled(spaceId))
      if (next.has(source)) next.delete(source)
      else next.add(source)
      writeDisabled(spaceId, next)
      return { spaceId, disabled: next }
    })
  }, [spaceId])

  const isEnabled = useCallback((source: CalendarSourceType) => !disabled.has(source), [disabled])

  const toggleFeed = useCallback((feedId: string) => {
    setFeeds((current) => {
      const next = new Set(current.spaceId === spaceId ? current.disabled : readDisabledFeeds(spaceId))
      if (next.has(feedId)) next.delete(feedId)
      else next.add(feedId)
      const kept = feedIds ? new Set([...next].filter((id) => feedIds.includes(id))) : next
      writeDisabledFeeds(spaceId, kept)
      return { spaceId, disabled: kept }
    })
  }, [spaceId, feedIds])

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
