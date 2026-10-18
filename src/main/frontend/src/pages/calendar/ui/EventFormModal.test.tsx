import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import type { EventInput } from '@/entities/calendar'
import { EventFormModal, type EventFormModalProps } from './EventFormModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

const initial: EventInput = {
  title: 'Piano', description: null, location: null, allDay: false,
  startDate: '2026-10-02', startTime: '18:00', endDate: '2026-10-02', endTime: '19:00',
  color: null, participantIds: [],
}

function renderForm(props: Partial<EventFormModalProps> = {}) {
  const onSubmit = vi.fn()
  render(<EventFormModal initial={initial} mode="create" members={[]} isPersonal
    onSubmit={onSubmit} onCancel={vi.fn()} {...props} />)
  return { onSubmit }
}

const save = () => fireEvent.click(screen.getByRole('button', { name: 'form.save' }))
const field = (label: string) => screen.getByLabelText(label) as HTMLInputElement

describe('EventFormModal — colour', () => {
  const swatches = () => screen.getAllByRole('button', { name: /^form\.color_name\./ })

  it('offers the six colours of events, and none of the sources\' colours', () => {
    renderForm()
    expect(swatches().map((swatch) => swatch.getAttribute('aria-label'))).toEqual([
      'form.color_name.event-violet', 'form.color_name.event-magenta', 'form.color_name.event-cyan',
      'form.color_name.event-graphite', 'form.color_name.event-indigo', 'form.color_name.event-brique',
    ])
    expect(swatches()[2].className).toContain('bg-event-cyan')
  })

  it('saves the colour picked', () => {
    const { onSubmit } = renderForm()
    fireEvent.click(screen.getByRole('button', { name: 'form.color_name.event-cyan' }))
    save()
    expect(onSubmit).toHaveBeenCalledWith({ ...initial, color: 'event-cyan' }, null)
  })
})

describe('EventFormModal — length', () => {
  it('refuses an event covering more days than the calendar allows', () => {
    const { onSubmit } = renderForm({ initial: { ...initial, allDay: true, startTime: null, endTime: null, endDate: '2027-10-03' } })

    save()

    expect(screen.getByRole('alert').textContent).toBe('form.too_long')
    expect(onSubmit).not.toHaveBeenCalled()
  })
})

describe('EventFormModal — recurrence', () => {
  it('saves a plain event while the recurring box is left unticked', () => {
    const { onSubmit } = renderForm()
    expect(field('form.recurring').checked).toBe(false)
    expect(screen.queryByLabelText('series.interval_count')).toBeNull()

    save()

    expect(onSubmit).toHaveBeenCalledWith(initial, null)
  })

  it('unfolds how the event repeats once ticked, weekly and without end by default', () => {
    const { onSubmit } = renderForm()
    fireEvent.click(field('form.recurring'))
    expect(field('series.interval_count').value).toBe('1')
    expect(field('series.interval_type').value).toBe('WEEKLY')
    expect(field('series.until').value).toBe('')

    save()

    expect(onSubmit).toHaveBeenCalledWith(initial, { intervalType: 'WEEKLY', intervalCount: 1, until: null, weekdays: ['FRIDAY'], dayRule: 'SAME_DAY' })
  })

  it('hands up the repetition picked', () => {
    const { onSubmit } = renderForm()
    fireEvent.click(field('form.recurring'))
    fireEvent.change(field('series.interval_count'), { target: { value: '2' } })
    fireEvent.change(field('series.interval_type'), { target: { value: 'MONTHLY' } })
    fireEvent.change(field('series.until'), { target: { value: '2027-06-30' } })

    save()

    expect(onSubmit).toHaveBeenCalledWith(initial, { intervalType: 'MONTHLY', intervalCount: 2, until: '2027-06-30', weekdays: [], dayRule: 'SAME_DAY' })
  })

  it('refuses a repetition that stops before the event starts', () => {
    const { onSubmit } = renderForm()
    fireEvent.click(field('form.recurring'))
    fireEvent.change(field('series.until'), { target: { value: '2026-10-01' } })

    save()

    expect(screen.getByRole('alert').textContent).toBe('series.until_before_start')
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('refuses to repeat every zero weeks', () => {
    const { onSubmit } = renderForm()
    fireEvent.click(field('form.recurring'))
    fireEvent.change(field('series.interval_count'), { target: { value: '0' } })

    save()

    expect(screen.getByRole('alert').textContent).toBe('series.interval_invalid')
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('refuses an occurrence lasting longer than the time between two of them', () => {
    const { onSubmit } = renderForm({ initial: { ...initial, endDate: '2026-10-10' } })
    fireEvent.click(field('form.recurring'))

    save()

    expect(screen.getByRole('alert').textContent).toBe('series.occurrence_too_long')
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('offers no recurring box when editing an event, as in finance', () => {
    renderForm({ mode: 'edit' })
    expect(screen.queryByLabelText('form.recurring')).toBeNull()
    expect(screen.queryByLabelText('series.interval_count')).toBeNull()
  })

  it('shows how a series repeats when the series itself is edited, with no box to untick', () => {
    const { onSubmit } = renderForm({
      mode: 'edit-series', initialRecurrence: { intervalType: 'MONTHLY', intervalCount: 3, until: '2027-06-30', weekdays: [], dayRule: 'SAME_DAY' },
    })
    expect(screen.getByText('series.edit_title')).toBeTruthy()
    expect(screen.queryByLabelText('form.recurring')).toBeNull()
    expect(field('series.interval_count').value).toBe('3')
    expect(field('series.interval_type').value).toBe('MONTHLY')
    expect(field('series.until').value).toBe('2027-06-30')

    save()

    expect(onSubmit).toHaveBeenCalledWith(initial, { intervalType: 'MONTHLY', intervalCount: 3, until: '2027-06-30', weekdays: [], dayRule: 'SAME_DAY' })
  })
})

describe('EventFormModal — days of a series', () => {
  const recurring = () => fireEvent.click(screen.getByLabelText('form.recurring'))
  const intervalType = (value: string) =>
    fireEvent.change(screen.getByLabelText('series.interval_type'), { target: { value } })

  it('repeats weekly on the start day, which cannot be unticked, and on the days ticked', () => {
    const { onSubmit } = renderForm()
    recurring()
    const friday = screen.getByRole('button', { name: /vendredi|friday/i })
    expect(friday.getAttribute('aria-pressed')).toBe('true')
    expect((friday as HTMLButtonElement).disabled).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: /^(lundi|monday)$/i }))
    save()
    expect(onSubmit).toHaveBeenCalledWith(initial,
      { intervalType: 'WEEKLY', intervalCount: 1, until: null, weekdays: ['MONDAY', 'FRIDAY'], dayRule: 'SAME_DAY' })
  })

  it('offers only the day rules the start date can give, and sends the one picked', () => {
    // Friday 30 October 2026: the 30th, the last Friday — not a 2nd anything, not the last day.
    const { onSubmit } = renderForm({ initial: { ...initial, startDate: '2026-10-30', endDate: '2026-10-30' } })
    recurring()
    intervalType('MONTHLY')
    const rule = screen.getByLabelText('series.day_rule') as HTMLSelectElement
    expect([...rule.options].map((option) => option.value)).toEqual(['SAME_DAY', 'LAST_WEEKDAY_OF_MONTH'])
    fireEvent.change(rule, { target: { value: 'LAST_WEEKDAY_OF_MONTH' } })
    save()
    expect(onSubmit).toHaveBeenCalledWith(expect.anything(),
      { intervalType: 'MONTHLY', intervalCount: 1, until: null, weekdays: [], dayRule: 'LAST_WEEKDAY_OF_MONTH' })
  })

  it('falls back to the same day when the start moves to a date the rule cannot fit', () => {
    renderForm({ initial: { ...initial, startDate: '2026-10-30', endDate: '2026-10-30' } })
    recurring()
    intervalType('MONTHLY')
    fireEvent.change(screen.getByLabelText('series.day_rule'), { target: { value: 'LAST_WEEKDAY_OF_MONTH' } })
    fireEvent.change(field('form.start_date'), { target: { value: '2026-10-13' } })
    expect((screen.getByLabelText('series.day_rule') as HTMLSelectElement).value).toBe('SAME_DAY')
  })

  it('moves the only day of a weekly series with its start, rather than adding the new one', () => {
    // Opened on Friday 2 October, moved to Wednesday 7 October before ticking "repeat": Wednesdays only.
    const { onSubmit } = renderForm()
    fireEvent.change(field('form.start_date'), { target: { value: '2026-10-07' } })
    fireEvent.change(field('form.end_date'), { target: { value: '2026-10-07' } })
    recurring()
    expect(screen.getByRole('button', { name: /^(vendredi|friday)$/i }).getAttribute('aria-pressed')).toBe('false')
    save()
    expect(onSubmit).toHaveBeenCalledWith(expect.anything(),
      { intervalType: 'WEEKLY', intervalCount: 1, until: null, weekdays: ['WEDNESDAY'], dayRule: 'SAME_DAY' })
  })

  it('moves the one day of a series being edited with its start', () => {
    const { onSubmit } = renderForm({
      mode: 'edit-series',
      initial: { ...initial, startDate: '2026-10-05', endDate: '2026-10-05' },
      initialRecurrence: { intervalType: 'WEEKLY', intervalCount: 1, until: null, weekdays: ['MONDAY'], dayRule: 'SAME_DAY' },
    })
    fireEvent.change(field('form.start_date'), { target: { value: '2026-10-07' } })
    fireEvent.change(field('form.end_date'), { target: { value: '2026-10-07' } })
    save()
    expect(onSubmit).toHaveBeenCalledWith(expect.anything(),
      { intervalType: 'WEEKLY', intervalCount: 1, until: null, weekdays: ['WEDNESDAY'], dayRule: 'SAME_DAY' })
  })

  it('keeps the days chosen and adds the new start day when a series repeats on several', () => {
    const { onSubmit } = renderForm({
      mode: 'edit-series',
      initial: { ...initial, startDate: '2026-10-05', endDate: '2026-10-05' },
      initialRecurrence: { intervalType: 'WEEKLY', intervalCount: 1, until: null, weekdays: ['MONDAY', 'THURSDAY'], dayRule: 'SAME_DAY' },
    })
    fireEvent.change(field('form.start_date'), { target: { value: '2026-10-06' } })
    fireEvent.change(field('form.end_date'), { target: { value: '2026-10-06' } })
    save()
    expect(onSubmit).toHaveBeenCalledWith(expect.anything(),
      { intervalType: 'WEEKLY', intervalCount: 1, until: null, weekdays: ['MONDAY', 'TUESDAY', 'THURSDAY'], dayRule: 'SAME_DAY' })
  })

  it('refuses an occurrence running into the next day chosen', () => {
    const { onSubmit } = renderForm({ initial: { ...initial, allDay: true, startTime: null, endTime: null, endDate: '2026-10-05' } })
    recurring()
    fireEvent.click(screen.getByRole('button', { name: /^(lundi|monday)$/i }))
    save()
    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByRole('alert').textContent).toBe('series.occurrence_too_long')
  })
})
