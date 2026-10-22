import { describe, it, expect } from 'vitest'
import type { RecurringEventSeries } from '@/entities/calendar'
import { dayRuleChoices, draftOf, ruleInEffect, shownDays, toRecurrence, toggleDay, type RecurrenceDraft } from './recurrenceDraft'
import type { Recurrence } from './seriesInput'

/** Weekly from Monday 19 October 2026, on Mondays only. */
const monday = '2026-10-19'
const wednesday = '2026-10-21'
const thursday = '2026-10-22'
const friday = '2026-10-23'
const weekly: RecurrenceDraft = { intervalType: 'WEEKLY', intervalCount: 1, until: null, extraDays: [], dayRule: 'SAME_DAY' }

describe('draftOf', () => {
  it('starts a new series weekly on its start\'s day alone, with no end', () => {
    expect(draftOf(null, monday)).toEqual(weekly)
  })

  it('keeps of a series being edited the days beyond its start shown', () => {
    const recurrence: Recurrence = { intervalType: 'WEEKLY', intervalCount: 2, until: '2027-06-30', weekdays: ['MONDAY', 'THURSDAY'], dayRule: 'SAME_DAY' }
    expect(draftOf(recurrence, monday)).toEqual({ ...weekly, intervalCount: 2, until: '2027-06-30', extraDays: ['THURSDAY'] })
  })

  it('keeps every day while the start holds no date', () => {
    const recurrence: Recurrence = { intervalType: 'WEEKLY', intervalCount: 1, until: null, weekdays: ['MONDAY', 'THURSDAY'], dayRule: 'SAME_DAY' }
    expect(draftOf(recurrence, '').extraDays).toEqual(['MONDAY', 'THURSDAY'])
  })
})

describe('the days of a weekly series', () => {
  it('are the start\'s and the ones chosen, Monday first', () => {
    expect(shownDays(toggleDay(weekly, 'SUNDAY'), friday)).toEqual(['FRIDAY', 'SUNDAY'])
  })

  it('follow the start, the one chosen kept and the one the start left dropped', () => {
    const chosen = toggleDay(weekly, 'THURSDAY')
    expect(shownDays(chosen, monday)).toEqual(['MONDAY', 'THURSDAY'])
    expect(shownDays(chosen, wednesday)).toEqual(['WEDNESDAY', 'THURSDAY'])
    expect(shownDays(chosen, friday)).toEqual(['THURSDAY', 'FRIDAY'])
  })

  it('keep a day chosen that the start passed over', () => {
    const chosen = toggleDay(weekly, 'THURSDAY')
    expect(shownDays(chosen, thursday)).toEqual(['THURSDAY'])
    expect(shownDays(chosen, friday)).toEqual(['THURSDAY', 'FRIDAY'])
  })

  it('are only those chosen while the start holds no date', () => {
    expect(shownDays(toggleDay(weekly, 'THURSDAY'), '')).toEqual(['THURSDAY'])
  })

  it('lose a day chosen once it is chosen again', () => {
    expect(toggleDay(toggleDay(weekly, 'THURSDAY'), 'THURSDAY').extraDays).toEqual([])
  })
})

describe('ruleInEffect', () => {
  // The last Friday, chosen from Friday 30 October.
  const monthly: RecurrenceDraft = { ...weekly, intervalType: 'MONTHLY', dayRule: 'LAST_WEEKDAY_OF_MONTH' }

  it('is the rule chosen while the start can give it', () => {
    expect(ruleInEffect(monthly, '2026-11-27')).toBe('LAST_WEEKDAY_OF_MONTH')
  })

  it('is the same day while the start cannot give the rule chosen', () => {
    expect(ruleInEffect(monthly, '2026-10-02')).toBe('SAME_DAY')
  })

  it('is the rule chosen again once a date typed through reaches one that can give it', () => {
    // Typing "29" over the 30th passes through the 2nd: the rule is the draft's still, read off the start each time.
    expect(ruleInEffect(monthly, '2026-10-02')).toBe('SAME_DAY')
    expect(ruleInEffect(monthly, '2026-10-29')).toBe('LAST_WEEKDAY_OF_MONTH')
  })

  it('is the rule chosen while the start holds no date', () => {
    expect(ruleInEffect(monthly, '')).toBe('LAST_WEEKDAY_OF_MONTH')
  })
})

describe('toRecurrence', () => {
  it('sends the days of a weekly series, the start\'s among them, and the same day', () => {
    expect(toRecurrence({ ...toggleDay(weekly, 'THURSDAY'), dayRule: 'LAST_DAY_OF_MONTH' }, monday))
      .toEqual({ intervalType: 'WEEKLY', intervalCount: 1, until: null, weekdays: ['MONDAY', 'THURSDAY'], dayRule: 'SAME_DAY' })
  })

  it('sends the day rule of a monthly or yearly series, and no days', () => {
    expect(toRecurrence({ ...toggleDay(weekly, 'THURSDAY'), intervalType: 'YEARLY', dayRule: 'LAST_WEEKDAY_OF_MONTH' }, '2026-10-30'))
      .toEqual({ intervalType: 'YEARLY', intervalCount: 1, until: null, weekdays: [], dayRule: 'LAST_WEEKDAY_OF_MONTH' })
  })

  it('sends the rule in effect, the same day while the start cannot give the one chosen', () => {
    expect(toRecurrence({ ...weekly, intervalType: 'MONTHLY', dayRule: 'LAST_WEEKDAY_OF_MONTH' }, '2026-10-02').dayRule).toBe('SAME_DAY')
  })

  it('sends neither for a daily series', () => {
    expect(toRecurrence({ ...weekly, intervalType: 'DAILY', dayRule: 'LAST_DAY_OF_MONTH' }, monday))
      .toEqual({ intervalType: 'DAILY', intervalCount: 1, until: null, weekdays: [], dayRule: 'SAME_DAY' })
  })
})

describe('dayRuleChoices', () => {
  const monthly: RecurrenceDraft = { ...weekly, intervalType: 'MONTHLY' }

  it('offers the rules the start can give, each said from the start of a new series', () => {
    expect(dayRuleChoices(monthly, '2026-10-30')).toEqual([
      { rule: 'SAME_DAY', wordedFrom: '2026-10-30' },
      { rule: 'LAST_WEEKDAY_OF_MONTH', wordedFrom: '2026-10-30' },
    ])
  })

  it('offers none while the start holds no date', () => {
    expect(dayRuleChoices(monthly, '')).toEqual([])
  })

  describe('of a series being edited', () => {
    // Monthly on the 31st, carrying on an edit: first shown on 30 April, where a month of 30 days puts it.
    const series: RecurringEventSeries = {
      id: 's-1', title: 'Loyer', description: null, location: null, allDay: true, startTime: null, endTime: null,
      durationDays: 0, color: null, intervalType: 'MONTHLY', intervalCount: 1, weekdays: null, dayRule: 'SAME_DAY',
      anchorDate: '2026-01-31', endDate: null, startsOn: '2026-04-20', firstDate: '2026-04-30',
      participantIds: [], createdBy: 'u-1', createdAt: '2026-01-01T00:00:00Z',
    }

    it('says its own rule from the anchor it keeps, and another from the start shown, which it would anchor on', () => {
      expect(dayRuleChoices(monthly, '2026-04-30', series)).toEqual([
        { rule: 'SAME_DAY', wordedFrom: '2026-01-31' },
        { rule: 'LAST_WEEKDAY_OF_MONTH', wordedFrom: '2026-04-30' },
        { rule: 'LAST_DAY_OF_MONTH', wordedFrom: '2026-04-30' },
      ])
    })

    it('says every rule from a start that moved', () => {
      expect(dayRuleChoices(monthly, '2026-05-12', series).map(({ wordedFrom }) => wordedFrom))
        .toEqual(['2026-05-12', '2026-05-12'])
    })
  })
})
