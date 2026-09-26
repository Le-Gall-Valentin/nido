import { describe, it, expect } from 'vitest'
import { formatDayRanges, groupConsecutiveDays } from './formatDayRanges'

describe('formatDayRanges', () => {
  it('groups consecutive dates into runs, across a month end', () => {
    expect(groupConsecutiveDays(['2026-09-30', '2026-10-01', '2026-10-03'])).toEqual([
      { from: '2026-09-30', to: '2026-10-01' },
      { from: '2026-10-03', to: '2026-10-03' },
    ])
  })

  it('names a single day and phrases a run with the given wording', () => {
    const range = (from: string, to: string) => `de ${from} à ${to}`
    expect(formatDayRanges(['2026-10-01'], 'fr-FR', range)).toBe('jeudi')
    expect(formatDayRanges(['2026-10-01', '2026-10-02', '2026-10-03', '2026-10-04'], 'fr-FR', range)).toBe('de jeudi à dimanche')
    expect(formatDayRanges(['2026-09-29', '2026-10-01', '2026-10-02'], 'fr-FR', range)).toBe('mardi, de jeudi à vendredi')
  })

  it('says nothing for no day', () => {
    expect(formatDayRanges([], 'fr-FR', (from, to) => `${from}-${to}`)).toBe('')
  })
})
