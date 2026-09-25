import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button, Input, Textarea, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { SpaceMember } from '@/entities/space'
import type { EventInput, RecurrenceInterval } from '@/entities/calendar'
import { DOT_CLASS, EVENT_COLORS } from '../lib/sourceAppearance'
import type { Recurrence } from '../lib/seriesInput'
import { MAX_EVENT_DAYS, coversTooManyDays, occurrenceOutlastsInterval } from '../lib/scheduleLimits'
import { ParticipantPicker } from './ParticipantPicker'

export interface EventFormModalProps {
  /** Pre-filled values. When creating, these are the blanks the caller wants (e.g. the clicked day). */
  initial: EventInput
  /**
   * Chooses the title, and whether the event may repeat: a new event may be made recurring, an
   * existing one may not — as in finance. A series being edited always shows how it repeats.
   */
  mode: 'create' | 'edit' | 'edit-series'
  /** How the series being edited repeats. */
  initialRecurrence?: Recurrence | null
  /** Said under how the event repeats — what an edit to a series will leave as it was. */
  notice?: string | null
  members: SpaceMember[]
  /** Hides the participant picker: a personal context has nobody to invite. */
  isPersonal: boolean
  submitError?: string | null
  isPending?: boolean
  /** The event, and how it repeats when it is a series — null for a plain event. */
  onSubmit: (input: EventInput, recurrence: Recurrence | null) => void
  onCancel: () => void
}

const INTERVALS: RecurrenceInterval[] = ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY']
const NO_RECURRENCE_YET: Recurrence = { intervalType: 'WEEKLY', intervalCount: 1, until: null }

/**
 * Pure presentation: it validates what it can see and hands a well-formed input upward. The
 * mutations live in EventFormPanel, which is what lets this be tested without a query client.
 */
export function EventFormModal({
  initial, mode, initialRecurrence = null, notice = null, members, isPersonal, submitError, isPending, onSubmit, onCancel,
}: EventFormModalProps) {
  const { t } = useTranslation('calendar')
  const [form, setForm] = useState<EventInput>(initial)
  const [recurring, setRecurring] = useState(mode === 'edit-series')
  const [recurrence, setRecurrence] = useState<Recurrence>(initialRecurrence ?? NO_RECURRENCE_YET)
  const [error, setError] = useState<string | null>(null)

  const patch = (changes: Partial<EventInput>) => setForm((current) => ({ ...current, ...changes }))

  const toggleAllDay = (allDay: boolean) => {
    // The two halves must stay consistent or the backend rejects the whole thing: an all-day
    // event carries no times, a timed one carries both.
    patch(allDay
      ? { allDay, startTime: null, endTime: null }
      : { allDay, startTime: form.startTime ?? '09:00', endTime: form.endTime ?? '10:00' })
  }

  const submit = () => {
    if (!form.title.trim()) return setError(t('form.title_required'))
    if (!form.startDate || !form.endDate) return setError(t('form.dates_required'))
    if (form.endDate < form.startDate) return setError(t('form.end_before_start'))
    if (!form.allDay && (!form.startTime || !form.endTime)) return setError(t('form.times_required'))
    if (!form.allDay && form.startDate === form.endDate
        && form.endTime && form.startTime && form.endTime < form.startTime) {
      // Only comparable on a single day — across days an "earlier" end time is an overnight event.
      return setError(t('form.end_before_start'))
    }
    if (coversTooManyDays(form)) return setError(t('form.too_long', { days: MAX_EVENT_DAYS }))
    if (recurring && !(recurrence.intervalCount >= 1)) return setError(t('series.interval_invalid'))
    if (recurring && occurrenceOutlastsInterval(form, recurrence)) return setError(t('series.occurrence_too_long'))
    if (recurring && recurrence.until && recurrence.until < form.startDate) return setError(t('series.until_before_start'))
    setError(null)
    onSubmit({ ...form, title: form.title.trim() }, recurring ? recurrence : null)
  }

  return (
    <Dialog open onClose={onCancel} title={t(mode === 'edit-series' ? 'series.edit_title' : `form.${mode}_title`)} maxWidth="max-w-lg">
      <div className="flex flex-col gap-3">
        <Input label={t('form.title')} value={form.title} autoFocus
          onChange={(e) => patch({ title: e.target.value })} />

        <Textarea label={t('form.description')} value={form.description ?? ''} rows={2}
          onChange={(e) => patch({ description: e.target.value || null })} />

        <Input label={t('form.location')} value={form.location ?? ''}
          onChange={(e) => patch({ location: e.target.value || null })} />

        <label className="flex items-center gap-2 text-sm text-fg-1">
          <input type="checkbox" checked={form.allDay} onChange={(e) => toggleAllDay(e.target.checked)} />
          {t('form.all_day')}
        </label>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Input label={t('form.start_date')} type="date" value={form.startDate}
            onChange={(e) => patch({ startDate: e.target.value, endDate: form.endDate || e.target.value })} />
          <Input label={t('form.end_date')} type="date" value={form.endDate}
            onChange={(e) => patch({ endDate: e.target.value })} />
          {!form.allDay && (
            <>
              <Input label={t('form.start_time')} type="time" value={form.startTime ?? ''}
                onChange={(e) => patch({ startTime: e.target.value })} />
              <Input label={t('form.end_time')} type="time" value={form.endTime ?? ''}
                onChange={(e) => patch({ endTime: e.target.value })} />
            </>
          )}
        </div>

        {mode === 'create' && (
          <label className="flex items-center gap-2 text-sm text-fg-1">
            <input type="checkbox" checked={recurring} onChange={(e) => setRecurring(e.target.checked)} />
            {t('form.recurring')}
          </label>
        )}
        {recurring && (
          // The event above is the first occurrence; this says when the next ones come.
          <div className="flex flex-col gap-3 rounded-lg bg-bg-2 p-3">
            <div className="grid grid-cols-2 gap-3">
              <Input label={t('series.interval_count')} type="number" min={1} value={String(recurrence.intervalCount)}
                onChange={(e) => setRecurrence({ ...recurrence, intervalCount: Number(e.target.value) })} />
              {/* Set like the Input beside it, so the two fields sit on one line. */}
              <label className="flex flex-col gap-1.5">
                <span className="text-[13px] font-semibold text-fg-1">{t('series.interval_type')}</span>
                <select value={recurrence.intervalType}
                  onChange={(e) => setRecurrence({ ...recurrence, intervalType: e.target.value as RecurrenceInterval })}
                  className="rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] text-fg-0 outline-none focus:border-accent">
                  {INTERVALS.map((interval) => (
                    <option key={interval} value={interval}>{t(`series.interval.${interval}`)}</option>
                  ))}
                </select>
              </label>
            </div>
            <Input label={t('series.until')} type="date" value={recurrence.until ?? ''}
              onChange={(e) => setRecurrence({ ...recurrence, until: e.target.value || null })} />
            {notice && <p className="text-xs text-fg-2">{notice}</p>}
          </div>
        )}

        <div>
          <span className="mb-1 block text-xs font-semibold text-fg-2">{t('form.color')}</span>
          <div className="flex gap-1.5">
            {EVENT_COLORS.map((token) => (
              <button key={token} type="button" aria-label={t(`form.color_name.${token}`)}
                aria-pressed={form.color === token}
                onClick={() => patch({ color: form.color === token ? null : token })}
                className={`size-6 rounded-full border-2 ${form.color === token ? 'border-fg-1' : 'border-transparent'}
                  ${DOT_CLASS[token]}`} />
            ))}
          </div>
        </div>

        {!isPersonal && members.length > 0 && (
          <ParticipantPicker members={members} selected={form.participantIds}
            onChange={(participantIds) => patch({ participantIds })} />
        )}

        {(error ?? submitError) && (
          <p role="alert" className="text-sm text-status-red">{error ?? submitError}</p>
        )}

        <div className="mt-1 flex justify-end gap-2">
          <Button type="button" onClick={onCancel}>{t('form.cancel')}</Button>
          <Button type="button" style={CTA_BUTTON_STYLE} disabled={isPending} onClick={submit}>
            {t('form.save')}
          </Button>
        </div>
      </div>
    </Dialog>
  )
}
