import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button, Input, Textarea, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { SpaceMember } from '@/entities/space'
import { ColorSwatches, isEventColor, type EventInput, type RecurringEventSeries } from '@/entities/calendar'
import type { Recurrence } from '../lib/seriesInput'
import { toRecurrence } from '../lib/recurrenceDraft'
import { eventFormProblem } from '../lib/eventFormProblem'
import { useRecurrenceDraft } from '../model/useRecurrenceDraft'
import { ParticipantPicker } from './ParticipantPicker'
import { RecurrenceFields } from './RecurrenceFields'

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
  /** The series being edited: its day rule is said from the anchor saving keeps, which its start shown may not be. */
  editedSeries?: RecurringEventSeries | null
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

/**
 * Pure presentation: it validates what it can see and hands a well-formed input upward. The
 * mutations live in EventFormPanel, which is what lets this be tested without a query client.
 */
export function EventFormModal({
  initial, mode, initialRecurrence = null, editedSeries = null, notice = null, members, isPersonal, submitError, isPending,
  onSubmit, onCancel,
}: EventFormModalProps) {
  const { t } = useTranslation('calendar')
  const [form, setForm] = useState<EventInput>(initial)
  const [recurring, setRecurring] = useState(mode === 'edit-series')
  const recurrence = useRecurrenceDraft(initialRecurrence, initial.startDate)
  const [error, setError] = useState<string | null>(null)

  const patch = (changes: Partial<EventInput>) => setForm((current) => ({ ...current, ...changes }))

  const toggleAllDay = (allDay: boolean) => {
    // The two halves must stay consistent or the backend rejects the whole thing: an all-day
    // event carries no times, a timed one carries both.
    patch(allDay
      ? { allDay, startTime: null, endTime: null }
      : { allDay, startTime: form.startTime ?? '09:00', endTime: form.endTime ?? '10:00' })
  }

  const changeStart = (startDate: string) => patch({ startDate, endDate: form.endDate || startDate })

  const submit = () => {
    const sent = recurring ? toRecurrence(recurrence.draft, form.startDate) : null
    const problem = eventFormProblem(form, sent)
    if (problem) return setError(t(problem.key, problem.values))
    setError(null)
    onSubmit({ ...form, title: form.title.trim() }, sent)
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
            onChange={(e) => changeStart(e.target.value)} />
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
          <RecurrenceFields recurrence={recurrence} startDate={form.startDate} editedSeries={editedSeries} notice={notice} />
        )}

        <ColorSwatches label={t('form.color')} value={isEventColor(form.color) ? form.color : null}
          onChange={(color) => patch({ color })} deselectable />

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
