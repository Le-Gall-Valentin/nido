import { describe, it, expect } from 'vitest'
import {
  capitalize, dayRulePhrase, describeRecurrence, possibleDayRules, shortestIntervalDays, weekdayOf,
} from './recurrenceRule'

/** A `t` that shows what it was asked: the key, and the values it was given. */
const t = (key: string, options?: Record<string, unknown>) =>
  options ? `${key}${JSON.stringify(options)}` : key

describe('weekdayOf', () => {
  it('reads the day of a date the API sends, whatever the browser zone', () => {
    expect(weekdayOf('2026-10-13')).toBe('TUESDAY')
    expect(weekdayOf('2026-10-18')).toBe('SUNDAY')
  })
})

describe('possibleDayRules', () => {
  it('offers the rank only up to the fourth', () => {
    expect(possibleDayRules('2026-10-28')).toEqual(['SAME_DAY', 'WEEKDAY_OF_MONTH', 'LAST_WEEKDAY_OF_MONTH'])
    expect(possibleDayRules('2026-10-29')).toEqual(['SAME_DAY', 'LAST_WEEKDAY_OF_MONTH'])
  })

  it('offers the last day only on the last day', () => {
    expect(possibleDayRules('2026-10-31')).toEqual(['SAME_DAY', 'LAST_WEEKDAY_OF_MONTH', 'LAST_DAY_OF_MONTH'])
    expect(possibleDayRules('2026-10-13')).toEqual(['SAME_DAY', 'WEEKDAY_OF_MONTH'])
  })
})

describe('shortestIntervalDays', () => {
  it('counts between the two closest weekdays, across the end of the week too', () => {
    expect(shortestIntervalDays({ intervalType: 'WEEKLY', intervalCount: 1, weekdays: ['MONDAY', 'THURSDAY'], dayRule: 'SAME_DAY' })).toBe(3)
    expect(shortestIntervalDays({ intervalType: 'WEEKLY', intervalCount: 1, weekdays: ['MONDAY', 'SATURDAY'], dayRule: 'SAME_DAY' })).toBe(2)
    expect(shortestIntervalDays({ intervalType: 'WEEKLY', intervalCount: 2, weekdays: ['FRIDAY'], dayRule: 'SAME_DAY' })).toBe(14)
  })

  it('reckons a year of weekdays at 52 weeks', () => {
    expect(shortestIntervalDays({ intervalType: 'YEARLY', intervalCount: 1, weekdays: [], dayRule: 'WEEKDAY_OF_MONTH' })).toBe(364)
    expect(shortestIntervalDays({ intervalType: 'YEARLY', intervalCount: 1, weekdays: [], dayRule: 'SAME_DAY' })).toBe(365)
  })
})

describe('wording', () => {
  it('says a monthly rank with its weekday', () => {
    expect(dayRulePhrase(t, 'WEEKDAY_OF_MONTH', 'MONTHLY', '2026-10-13', 'fr'))
      .toBe('rule.weekday_of_month{"rank":"rule.rank.2","weekday":"mardi"}')
  })

  it('elides "de" before a month starting with a vowel, in French', () => {
    expect(dayRulePhrase(t, 'LAST_WEEKDAY_OF_MONTH', 'YEARLY', '2026-10-30', 'fr'))
      .toBe('rule.last_weekday_in{"weekday":"vendredi","ofMonth":"rule.of_month_elided{\\"month\\":\\"octobre\\"}"}')
    expect(dayRulePhrase(t, 'LAST_WEEKDAY_OF_MONTH', 'YEARLY', '2026-05-31', 'fr'))
      .toBe('rule.last_weekday_in{"weekday":"dimanche","ofMonth":"rule.of_month{\\"month\\":\\"mai\\"}"}')
  })

  it('names every weekday of a weekly series, in order', () => {
    expect(describeRecurrence(t, {
      intervalType: 'WEEKLY', intervalCount: 2, weekdays: ['THURSDAY', 'MONDAY'], dayRule: 'SAME_DAY', anchorDate: '2026-01-05',
    }, 'fr')).toBe('rule.every.WEEKLY{"count":2} rule.on_days{"days":"rule.weekday_item{\\"weekday\\":\\"lundi\\"} et rule.weekday_item{\\"weekday\\":\\"jeudi\\"}"}')
  })

  it('capitalizes an option label', () => {
    expect(capitalize('le dernier jour du mois')).toBe('Le dernier jour du mois')
  })
})
