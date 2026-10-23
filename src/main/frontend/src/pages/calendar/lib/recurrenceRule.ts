import type { DayRule, RecurrenceInterval, RecurringEventSeries, Weekday } from '@/entities/calendar'
import { isValidIso } from './calendarWindow'

type Translate = (key: string, options?: Record<string, unknown>) => string

/** Monday first, as the server counts weeks (WKST=MO). */
export const WEEKDAYS: readonly Weekday[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

function parts(date: string): { year: number; month: number; day: number } {
  const [year, month, day] = date.split('-').map(Number) as [number, number, number]
  return { year, month, day }
}

function lengthOfMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate()
}

/**
 * The day of a calendar date, read in UTC: a date has no zone, and the browser's must not move it. Null for a field
 * that holds no date: a date input says '' while one of its parts is cleared.
 */
export function weekdayOf(date: string): Weekday | null {
  if (!isValidIso(date)) return null
  const { year, month, day } = parts(date)
  return WEEKDAYS[(new Date(Date.UTC(year, month - 1, day)).getUTCDay() + 6) % 7] as Weekday
}

function rankOf(date: string): number {
  return Math.floor((parts(date).day - 1) / 7) + 1
}

/**
 * The day rules a series starting on `date` can have — the server's DayRule.fits, in its order. Only the same day
 * while there is no date to read the others from.
 */
export function possibleDayRules(date: string): DayRule[] {
  if (!isValidIso(date)) return ['SAME_DAY']
  const { year, month, day } = parts(date)
  const length = lengthOfMonth(year, month)
  const rules: DayRule[] = ['SAME_DAY']
  if (day <= 28) rules.push('WEEKDAY_OF_MONTH')
  if (day > length - 7) rules.push('LAST_WEEKDAY_OF_MONTH')
  if (day === length) rules.push('LAST_DAY_OF_MONTH')
  return rules
}

interface Rhythm {
  intervalType: RecurrenceInterval
  intervalCount: number
  weekdays: readonly Weekday[]
  dayRule: DayRule
}

/** The shortest time between two occurrences, in days — the server's own table (EventScheduleValidator). */
export function shortestIntervalDays({ intervalType, intervalCount, weekdays, dayRule }: Rhythm): number {
  switch (intervalType) {
    case 'DAILY':
      return intervalCount
    case 'WEEKLY': {
      const days = weekdays.map((day) => WEEKDAYS.indexOf(day) + 1).sort((a, b) => a - b)
      if (days.length === 0) return 7 * intervalCount
      let shortest = 7 * intervalCount - ((days[days.length - 1] as number) - (days[0] as number))
      for (let i = 1; i < days.length; i++) shortest = Math.min(shortest, (days[i] as number) - (days[i - 1] as number))
      return shortest
    }
    case 'MONTHLY':
      return 28 * intervalCount
    case 'YEARLY':
      return (dayRule === 'WEEKDAY_OF_MONTH' || dayRule === 'LAST_WEEKDAY_OF_MONTH' ? 364 : 365) * intervalCount
  }
}

export function weekdayName(day: Weekday, locale: string, style: 'long' | 'narrow' = 'long'): string {
  // 2024-01-01 was a Monday.
  return new Intl.DateTimeFormat(locale, { weekday: style, timeZone: 'UTC' })
    .format(Date.UTC(2024, 0, 1 + WEEKDAYS.indexOf(day)))
}

function monthName(date: string, locale: string): string {
  const { year, month } = parts(date)
  return new Intl.DateTimeFormat(locale, { month: 'long', timeZone: 'UTC' }).format(Date.UTC(year, month - 1, 1))
}

/** "de mai", "d'octobre": French elides before a vowel; English says "of" either way. */
function ofMonth(t: Translate, date: string, locale: string): string {
  const month = monthName(date, locale)
  return t(/^[aeiouâàéèêîôûh]/i.test(month) ? 'rule.of_month_elided' : 'rule.of_month', { month })
}

/**
 * How a monthly or yearly series picks its day, as a phrase to follow its frequency: "le 2ᵉ mardi". Nothing while
 * there is no date to read the day from.
 */
export function dayRulePhrase(t: Translate, rule: DayRule, intervalType: RecurrenceInterval, anchor: string, locale: string): string {
  const anchorDay = weekdayOf(anchor)
  if (!anchorDay) return ''
  const yearly = intervalType === 'YEARLY'
  const weekday = weekdayName(anchorDay, locale)
  const { day } = parts(anchor)
  // French writes the first of a month "le 1ᵉʳ"; a language with no such key falls back to the plain one.
  const context = day === 1 ? 'first' : undefined
  switch (rule) {
    case 'SAME_DAY':
      return yearly
        ? t('rule.day_of_month', { day, month: monthName(anchor, locale), context })
        : t('rule.day', { day, context })
    case 'WEEKDAY_OF_MONTH':
      return yearly
        ? t('rule.weekday_of_month_in', { rank: t(`rule.rank.${rankOf(anchor)}`), weekday, ofMonth: ofMonth(t, anchor, locale) })
        : t('rule.weekday_of_month', { rank: t(`rule.rank.${rankOf(anchor)}`), weekday })
    case 'LAST_WEEKDAY_OF_MONTH':
      return yearly
        ? t('rule.last_weekday_in', { weekday, ofMonth: ofMonth(t, anchor, locale) })
        : t('rule.last_weekday', { weekday })
    case 'LAST_DAY_OF_MONTH':
      return yearly ? t('rule.last_day_in', { ofMonth: ofMonth(t, anchor, locale) }) : t('rule.last_day')
  }
}

export interface DescribedRule extends Rhythm {
  anchorDate: string
}

/** The whole rule in one sentence: "Toutes les 2 semaines le lundi et le jeudi" — the frequency alone when no day can be said. */
export function describeRecurrence(t: Translate, rule: DescribedRule, locale: string): string {
  const every = t(`rule.every.${rule.intervalType}`, { count: rule.intervalCount })
  switch (rule.intervalType) {
    case 'DAILY':
      return every
    case 'WEEKLY': {
      const days = WEEKDAYS.filter((day) => rule.weekdays.includes(day))
        .map((day) => t('rule.weekday_item', { weekday: weekdayName(day, locale) }))
      if (days.length === 0) return every
      const list = new Intl.ListFormat(locale, { type: 'conjunction' }).format(days)
      return `${every} ${t('rule.on_days', { days: list })}`
    }
    case 'MONTHLY':
    case 'YEARLY': {
      const phrase = dayRulePhrase(t, rule.dayRule, rule.intervalType, rule.anchorDate, locale)
      return phrase ? `${every} ${phrase}` : every
    }
  }
}

/** How a stored series repeats, in one sentence. A weekly series that names no days repeats on its anchor's, as the server reads it. */
export function describeSeries(t: Translate, series: RecurringEventSeries, locale: string): string {
  const anchorDay = weekdayOf(series.anchorDate)
  return describeRecurrence(t, { ...series, weekdays: series.weekdays ?? (anchorDay ? [anchorDay] : []) }, locale)
}

export function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1)
}
