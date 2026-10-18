import { describe, it, expect } from 'vitest'
import { CalendarSyncError, SYNC_REASONS } from '@/entities/calendar'
import fr from '../locales/fr.json'
import en from '../locales/en.json'
import { feedErrorText, ignoredReasonText, syncErrorMessage } from './syncWording'

const t = (key: string, options?: Record<string, unknown>) => (options ? `${key}${JSON.stringify(options)}` : key)

describe('syncWording', () => {
  it('says an http status as such', () => {
    expect(feedErrorText(t, 'HTTP_404')).toBe('feed_error.HTTP{"status":"404"}')
    expect(feedErrorText(t, 'TIMEOUT')).toBe('feed_error.TIMEOUT')
  })

  it('says why a calendar could not be followed', () => {
    expect(syncErrorMessage(t, new CalendarSyncError('feed_unavailable', 'TIMEOUT')))
      .toBe('errors.feed_unavailable{"reason":"feed_error.TIMEOUT"}')
  })

  it('falls back on a generic message for what it does not know', () => {
    expect(syncErrorMessage(t, new Error('x'))).toBe('errors.server')
    expect(syncErrorMessage(t, new CalendarSyncError('not_ics'))).toBe('errors.not_ics')
    expect(syncErrorMessage(t, new CalendarSyncError('something_new' as never))).toBe('errors.server')
  })

  it('words every reason a sync call can fail for, in both languages', () => {
    for (const locale of [fr, en]) {
      for (const reason of SYNC_REASONS.filter((r) => r !== 'feed_unavailable')) {
        expect(locale.errors).toHaveProperty(reason)
      }
      expect(locale.errors.feed_unavailable).toContain('{{reason}}')
    }
  })

  it('words the details of an ignored event, and never shows a raw key for another', () => {
    expect(ignoredReasonText(t, 'DUPLICATE_UID')).toBe('import.ignored_reason.DUPLICATE_UID')
    expect(ignoredReasonText(t, 'TOO_HEAVY')).toBe('import.ignored_reason.TOO_HEAVY')
    expect(ignoredReasonText(t, 'BAD_RULE')).toBe('import.ignored_reason.BAD_RULE')
    expect(ignoredReasonText(t, 'SOMETHING_NEW')).toBe('import.ignored_unknown')
    for (const locale of [fr, en]) {
      expect(Object.keys(locale.import.ignored_reason)).toEqual(
        expect.arrayContaining(['DUPLICATE_UID', 'TOO_HEAVY', 'BAD_RULE']))
    }
  })

  it('does not blame a size limit the followed calendar is not held to', () => {
    expect(fr.feed_error.TOO_LARGE).toBe('le calendrier est trop volumineux')
    expect(en.feed_error.TOO_LARGE).toBe('the calendar is too large')
    expect(fr.errors.file_too_large).toBe('Le fichier dépasse 5 Mo.')
    expect(en.errors.file_too_large).toBe('The file is larger than 5 MB.')
  })
})
