import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { Input } from '@/shared/ui'
import { resolveLocale } from '@/shared/lib'
import type { DayRule, RecurrenceInterval, RecurringEventSeries } from '@/entities/calendar'
import { WEEKDAYS, weekdayOf, weekdayName, dayRulePhrase, capitalize } from '../lib/recurrenceRule'
import { dayRuleChoices, ruleInEffect, shownDays } from '../lib/recurrenceDraft'
import { MAX_INTERVAL_COUNT } from '../lib/scheduleLimits'
import type { RecurrenceDraftState } from '../model/useRecurrenceDraft'

interface RecurrenceFieldsProps {
  recurrence: RecurrenceDraftState
  /** The start date as its field holds it: '' while one of its parts is cleared. */
  startDate: string
  /** The series being edited, whose day rule is said from the anchor saving keeps — not always the start shown. */
  editedSeries: RecurringEventSeries | null
  /** Said under how the event repeats — what an edit to a series will leave as it was. */
  notice: string | null
}

const INTERVALS: RecurrenceInterval[] = ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY']
/** A select dressed as the Input component dresses its field, and its label as Input's own. */
const FIELD_LABEL = 'text-[13px] font-semibold text-fg-1'
const SELECT = 'rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] text-fg-0 outline-none '
  + 'focus:border-accent disabled:text-fg-2'

/** How an event repeats: the event above is the first occurrence, and this says when the next ones come. */
export function RecurrenceFields({ recurrence, startDate, editedSeries, notice }: RecurrenceFieldsProps) {
  const { t } = useTranslation('calendar')
  const locale = resolveLocale(i18next.language)
  const intervalTypeId = useId()
  const dayRuleId = useId()
  const { draft } = recurrence

  const startDay = weekdayOf(startDate)
  const days = shownDays(draft, startDate)
  const choices = dayRuleChoices(draft, startDate, editedSeries)
  // The rule chosen, or the same day while the start cannot give it: what the list shows, as toRecurrence sends it.
  const effectiveRule = ruleInEffect(draft, startDate)
  const ruleLabel = (rule: DayRule) => {
    const choice = choices.find((candidate) => candidate.rule === rule)
    return choice ? capitalize(dayRulePhrase(t, rule, draft.intervalType, choice.wordedFrom, locale)) : ''
  }
  // The list changes under the start field, both when the rule falls back and when it comes back: each change is said.
  const fallingBack = effectiveRule !== draft.dayRule
  const [said, setSaid] = useState<'nothing' | 'fell_back' | 'restored'>('nothing')
  const toSay = fallingBack ? 'fell_back' : said === 'fell_back' ? 'restored' : said
  if (toSay !== said) setSaid(toSay)

  return (
    <div className="flex flex-col gap-3 rounded-lg bg-bg-2 p-3">
      <div className="grid grid-cols-2 gap-3">
        <Input label={t('series.interval_count')} type="number" min={1} max={MAX_INTERVAL_COUNT} step={1}
          value={String(draft.intervalCount)} onChange={(e) => recurrence.setIntervalCount(Number(e.target.value))} />
        {/* Set like the Input beside it, so the two fields sit on one line. */}
        <div className="flex flex-col gap-1.5">
          <label htmlFor={intervalTypeId} className={FIELD_LABEL}>{t('series.interval_type')}</label>
          <select id={intervalTypeId} value={draft.intervalType} className={SELECT}
            onChange={(e) => recurrence.setIntervalType(e.target.value as RecurrenceInterval)}>
            {INTERVALS.map((interval) => (
              <option key={interval} value={interval}>{t(`series.interval.${interval}`)}</option>
            ))}
          </select>
        </div>
      </div>
      {draft.intervalType === 'WEEKLY' && (
        <div role="group" aria-label={t('series.weekdays')} className="flex flex-wrap gap-1.5">
          {WEEKDAYS.map((day) => {
            const isStart = day === startDay
            const on = days.includes(day)
            return (
              // The start day is always one of them: the series starts on its own first occurrence.
              <button key={day} type="button" aria-pressed={on} disabled={isStart}
                aria-label={weekdayName(day, locale)} onClick={() => recurrence.toggleDay(day)}
                className={`grid size-8 place-items-center rounded-full border text-xs font-semibold
                  ${on ? 'border-accent bg-accent text-on-accent' : 'border-border text-fg-2 hover:bg-bg-1'}
                  disabled:opacity-100`}>
                {weekdayName(day, locale, 'narrow')}
              </button>
            )
          })}
        </div>
      )}
      {(draft.intervalType === 'MONTHLY' || draft.intervalType === 'YEARLY') && (
        <div className="flex flex-col gap-1.5">
          <label htmlFor={dayRuleId} className={FIELD_LABEL}>{t('series.day_rule')}</label>
          {/* With no start date there is no day to read a rule from: the list waits for one, and says so. */}
          <select id={dayRuleId} value={effectiveRule} className={SELECT} disabled={choices.length === 0}
            onChange={(e) => recurrence.setDayRule(e.target.value as DayRule)}>
            {choices.length === 0
              ? <option value={draft.dayRule}>{t('series.day_rule_needs_start')}</option>
              : choices.map(({ rule }) => <option key={rule} value={rule}>{ruleLabel(rule)}</option>)}
          </select>
          {/* Said for whoever cannot see the list change. */}
          <p role="status" className="sr-only">
            {toSay === 'fell_back' ? t('series.day_rule_fell_back', { rule: ruleLabel(effectiveRule) })
              : toSay === 'restored' ? t('series.day_rule_restored', { rule: ruleLabel(effectiveRule) }) : ''}
          </p>
        </div>
      )}
      <Input label={t('series.until')} type="date" value={draft.until ?? ''}
        onChange={(e) => recurrence.setUntil(e.target.value || null)} />
      {notice && <p className="text-xs text-fg-2">{notice}</p>}
    </div>
  )
}
