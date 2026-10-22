import { createInstance } from 'i18next'
import { describe, it, expect } from 'vitest'
import en from '../locales/en.json'
import fr from '../locales/fr.json'
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

describe('a field that holds no date', () => {
  // A date input says '' while one of its parts is cleared; nothing read from it may throw while it is rendered.
  it('has no day', () => {
    expect(weekdayOf('')).toBeNull()
    expect(weekdayOf('2026-02-30')).toBeNull()
  })

  it('gives the same day alone', () => {
    expect(possibleDayRules('')).toEqual(['SAME_DAY'])
  })

  it('says no day rule, for a yearly series as for a monthly one', () => {
    for (const rule of ['SAME_DAY', 'WEEKDAY_OF_MONTH', 'LAST_WEEKDAY_OF_MONTH', 'LAST_DAY_OF_MONTH'] as const) {
      expect(dayRulePhrase(t, rule, 'YEARLY', '', 'fr')).toBe('')
      expect(dayRulePhrase(t, rule, 'MONTHLY', '', 'fr')).toBe('')
    }
  })

  it('leaves the frequency alone in a sentence, with nothing trailing', () => {
    expect(describeRecurrence(t, {
      intervalType: 'YEARLY', intervalCount: 1, weekdays: [], dayRule: 'SAME_DAY', anchorDate: '',
    }, 'fr')).toBe('rule.every.YEARLY{"count":1}')
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

describe('wording of every rule', () => {
  it('says each day rule of a monthly series, at every rank', () => {
    expect(dayRulePhrase(t, 'SAME_DAY', 'MONTHLY', '2026-10-14', 'fr')).toBe('rule.day{"day":14}')
    expect(dayRulePhrase(t, 'WEEKDAY_OF_MONTH', 'MONTHLY', '2026-10-01', 'fr'))
      .toBe('rule.weekday_of_month{"rank":"rule.rank.1","weekday":"jeudi"}')
    expect(dayRulePhrase(t, 'WEEKDAY_OF_MONTH', 'MONTHLY', '2026-10-20', 'fr'))
      .toBe('rule.weekday_of_month{"rank":"rule.rank.3","weekday":"mardi"}')
    expect(dayRulePhrase(t, 'WEEKDAY_OF_MONTH', 'MONTHLY', '2026-10-28', 'fr'))
      .toBe('rule.weekday_of_month{"rank":"rule.rank.4","weekday":"mercredi"}')
    expect(dayRulePhrase(t, 'LAST_WEEKDAY_OF_MONTH', 'MONTHLY', '2026-10-30', 'fr')).toBe('rule.last_weekday{"weekday":"vendredi"}')
    expect(dayRulePhrase(t, 'LAST_DAY_OF_MONTH', 'MONTHLY', '2026-10-31', 'fr')).toBe('rule.last_day')
  })

  it('says each day rule of a yearly series with its month', () => {
    expect(dayRulePhrase(t, 'SAME_DAY', 'YEARLY', '2026-10-14', 'fr')).toBe('rule.day_of_month{"day":14,"month":"octobre"}')
    expect(dayRulePhrase(t, 'WEEKDAY_OF_MONTH', 'YEARLY', '2026-11-26', 'fr'))
      .toBe('rule.weekday_of_month_in{"rank":"rule.rank.4","weekday":"jeudi","ofMonth":"rule.of_month{\\"month\\":\\"novembre\\"}"}')
    expect(dayRulePhrase(t, 'LAST_DAY_OF_MONTH', 'YEARLY', '2027-02-28', 'fr'))
      .toBe('rule.last_day_in{"ofMonth":"rule.of_month{\\"month\\":\\"février\\"}"}')
  })

  it('names days and months in English for an English reader', () => {
    expect(dayRulePhrase(t, 'WEEKDAY_OF_MONTH', 'YEARLY', '2026-11-26', 'en'))
      .toBe('rule.weekday_of_month_in{"rank":"rule.rank.4","weekday":"Thursday","ofMonth":"rule.of_month{\\"month\\":\\"November\\"}"}')
    expect(describeRecurrence(t, {
      intervalType: 'WEEKLY', intervalCount: 1, weekdays: ['MONDAY', 'THURSDAY'], dayRule: 'SAME_DAY', anchorDate: '2026-01-05',
    }, 'en')).toBe('rule.every.WEEKLY{"count":1} rule.on_days{"days":"rule.weekday_item{\\"weekday\\":\\"Monday\\"} and rule.weekday_item{\\"weekday\\":\\"Thursday\\"}"}')
  })
})

/** The sentences as a reader sees them: the real translations, through a real i18next and the browser's Intl. */
async function realT(language: 'en' | 'fr') {
  const instance = createInstance()
  await instance.init({
    lng: language, fallbackLng: false, ns: ['calendar'], defaultNS: 'calendar',
    resources: { en: { calendar: en }, fr: { calendar: fr } },
    interpolation: { escapeValue: false }, initImmediate: false,
  })
  return instance.getFixedT(null, 'calendar') as unknown as (key: string, options?: Record<string, unknown>) => string
}

describe('the sentences a reader sees', () => {
  it('reads in French', async () => {
    const fr = await realT('fr')
    expect(describeRecurrence(fr, {
      intervalType: 'WEEKLY', intervalCount: 2, weekdays: ['THURSDAY', 'MONDAY'], dayRule: 'SAME_DAY', anchorDate: '2026-01-05',
    }, 'fr')).toBe('Toutes les 2 semaines le lundi et le jeudi')
    expect(describeRecurrence(fr, {
      intervalType: 'MONTHLY', intervalCount: 1, weekdays: [], dayRule: 'WEEKDAY_OF_MONTH', anchorDate: '2026-10-13',
    }, 'fr')).toBe('Tous les mois le 2ᵉ mardi')
    expect(describeRecurrence(fr, {
      intervalType: 'YEARLY', intervalCount: 1, weekdays: [], dayRule: 'LAST_WEEKDAY_OF_MONTH', anchorDate: '2026-10-30',
    }, 'fr')).toBe('Tous les ans le dernier vendredi d\'octobre')
    expect(describeRecurrence(fr, {
      intervalType: 'DAILY', intervalCount: 1, weekdays: [], dayRule: 'SAME_DAY', anchorDate: '2026-10-30',
    }, 'fr')).toBe('Tous les jours')
  })

  it('says the first of a month « le 1ᵉʳ » in French', async () => {
    const fr = await realT('fr')
    expect(describeRecurrence(fr, {
      intervalType: 'MONTHLY', intervalCount: 1, weekdays: [], dayRule: 'SAME_DAY', anchorDate: '2026-10-01',
    }, 'fr')).toBe('Tous les mois le 1ᵉʳ')
    expect(describeRecurrence(fr, {
      intervalType: 'YEARLY', intervalCount: 1, weekdays: [], dayRule: 'SAME_DAY', anchorDate: '2026-10-01',
    }, 'fr')).toBe('Tous les ans le 1ᵉʳ octobre')
    expect(describeRecurrence(fr, {
      intervalType: 'MONTHLY', intervalCount: 1, weekdays: [], dayRule: 'SAME_DAY', anchorDate: '2026-10-02',
    }, 'fr')).toBe('Tous les mois le 2')
  })

  it('reads in English', async () => {
    const en = await realT('en')
    expect(describeRecurrence(en, {
      intervalType: 'WEEKLY', intervalCount: 2, weekdays: ['THURSDAY', 'MONDAY'], dayRule: 'SAME_DAY', anchorDate: '2026-01-05',
    }, 'en')).toBe('Every 2 weeks on Monday and Thursday')
    expect(describeRecurrence(en, {
      intervalType: 'YEARLY', intervalCount: 1, weekdays: [], dayRule: 'LAST_WEEKDAY_OF_MONTH', anchorDate: '2026-10-30',
    }, 'en')).toBe('Every year on the last Friday of October')
    expect(describeRecurrence(en, {
      intervalType: 'MONTHLY', intervalCount: 3, weekdays: [], dayRule: 'SAME_DAY', anchorDate: '2026-10-14',
    }, 'en')).toBe('Every 3 months on day 14')
  })

  it('says the first of a month as any other day in English', async () => {
    const en = await realT('en')
    expect(describeRecurrence(en, {
      intervalType: 'MONTHLY', intervalCount: 1, weekdays: [], dayRule: 'SAME_DAY', anchorDate: '2026-10-01',
    }, 'en')).toBe('Every month on day 1')
    expect(describeRecurrence(en, {
      intervalType: 'YEARLY', intervalCount: 1, weekdays: [], dayRule: 'SAME_DAY', anchorDate: '2026-10-01',
    }, 'en')).toBe('Every year on October 1')
  })
})
