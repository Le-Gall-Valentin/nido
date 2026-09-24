import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { RecurrenceInterval, RecurringEventSeries, RecurringEventSeriesInput } from '@/entities/calendar'
import type { SpaceMember } from '@/entities/space'
import { ParticipantPicker } from './ParticipantPicker'

const INTERVALS: RecurrenceInterval[] = ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY']

interface RecurringEventSeriesFormModalProps {
  series: RecurringEventSeries | null
  members: SpaceMember[]
  /** Who is creating: they take part in a new series by default. */
  currentUserId: string
  /** Hides the participant picker: in a personal context its owner always takes part. */
  isPersonal: boolean
  /** The day the calendar shows: a new series starts on it. */
  defaultDate: string
  submitError?: string | null
  isPending?: boolean
  onSubmit: (input: RecurringEventSeriesInput) => void
  onCancel: () => void
}

function blank(creatorId: string, anchorDate: string): RecurringEventSeriesInput {
  return {
    title: '', description: null, location: null, allDay: true,
    startTime: null, endTime: null, durationDays: 0, color: null,
    intervalType: 'WEEKLY', intervalCount: 1, anchorDate, endDate: null, participantIds: [creatorId],
  }
}

/** Spelled out rather than destructured: the fields the form writes are exactly these, and a
 *  rest-spread would silently carry a new server field into the request the day one is added. */
function toInput(series: RecurringEventSeries): RecurringEventSeriesInput {
  return {
    title: series.title, description: series.description, location: series.location,
    allDay: series.allDay, startTime: series.startTime, endTime: series.endTime,
    durationDays: series.durationDays, color: series.color,
    intervalType: series.intervalType, intervalCount: series.intervalCount,
    anchorDate: series.anchorDate, endDate: series.endDate,
    participantIds: series.participantIds,
  }
}

export function RecurringEventSeriesFormModal({
  series, members, currentUserId, isPersonal, defaultDate, submitError, isPending, onSubmit, onCancel,
}: RecurringEventSeriesFormModalProps) {
  const { t } = useTranslation('calendar')
  const [form, setForm] = useState<RecurringEventSeriesInput>(series ? toInput(series) : blank(currentUserId, defaultDate))
  const [error, setError] = useState<string | null>(null)

  const patch = (changes: Partial<RecurringEventSeriesInput>) =>
    setForm((current) => ({ ...current, ...changes }))

  const toggleAllDay = (allDay: boolean) => patch(allDay
    ? { allDay, startTime: null, endTime: null }
    : { allDay, startTime: form.startTime ?? '09:00', endTime: form.endTime ?? '10:00' })

  const submit = () => {
    if (!form.title.trim()) return setError(t('form.title_required'))
    if (!form.anchorDate) return setError(t('series.anchor_required'))
    if (form.intervalCount < 1) return setError(t('series.interval_invalid'))
    if (form.endDate && form.endDate < form.anchorDate) return setError(t('series.end_before_anchor'))
    if (!form.allDay && (!form.startTime || !form.endTime)) return setError(t('form.times_required'))
    setError(null)
    onSubmit({ ...form, title: form.title.trim() })
  }

  return (
    <Dialog open onClose={onCancel} title={t(`series.${series ? 'edit' : 'create'}_title`)} maxWidth="max-w-lg">
      <div className="flex flex-col gap-3">
        <Input label={t('form.title')} value={form.title} autoFocus
          onChange={(e) => patch({ title: e.target.value })} />

        <label className="flex items-center gap-2 text-sm text-fg-1">
          <input type="checkbox" checked={form.allDay} onChange={(e) => toggleAllDay(e.target.checked)} />
          {t('form.all_day')}
        </label>

        {!form.allDay && (
          <div className="grid grid-cols-2 gap-3">
            <Input label={t('form.start_time')} type="time" value={form.startTime ?? ''}
              onChange={(e) => patch({ startTime: e.target.value })} />
            <Input label={t('form.end_time')} type="time" value={form.endTime ?? ''}
              onChange={(e) => patch({ endTime: e.target.value })} />
          </div>
        )}

        <div className="grid grid-cols-2 gap-3">
          <Input label={t('series.interval_count')} type="number" min={1} value={String(form.intervalCount)}
            onChange={(e) => patch({ intervalCount: Number(e.target.value) })} />
          <label className="flex flex-col gap-1">
            <span className="text-xs font-semibold text-fg-2">{t('series.interval_type')}</span>
            <select value={form.intervalType}
              onChange={(e) => patch({ intervalType: e.target.value as RecurrenceInterval })}
              className="rounded-lg border border-border bg-bg-1 px-3 py-2 text-sm text-fg-1">
              {INTERVALS.map((interval) => (
                <option key={interval} value={interval}>{t(`series.interval.${interval}`)}</option>
              ))}
            </select>
          </label>
        </div>

        {/* Days past its start that each occurrence runs — 0 for a same-day event, 2 for a long
            weekend. It is how a recurring multi-day event is expressed. */}
        <Input label={t('series.duration_days')} type="number" min={0} value={String(form.durationDays)}
          onChange={(e) => patch({ durationDays: Number(e.target.value) })} />

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Input label={t('series.anchor_date')} type="date" value={form.anchorDate}
            onChange={(e) => patch({ anchorDate: e.target.value })} />
          <Input label={t('series.end_date')} type="date" value={form.endDate ?? ''}
            onChange={(e) => patch({ endDate: e.target.value || null })} />
        </div>

        {!isPersonal && members.length > 0 && (
          <ParticipantPicker members={members} selected={form.participantIds}
            onChange={(participantIds) => patch({ participantIds })} />
        )}

        {(error ?? submitError) && <p role="alert" className="text-sm text-status-red">{error ?? submitError}</p>}

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
