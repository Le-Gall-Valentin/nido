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

    expect(onSubmit).toHaveBeenCalledWith(initial, { intervalType: 'WEEKLY', intervalCount: 1, until: null })
  })

  it('hands up the repetition picked', () => {
    const { onSubmit } = renderForm()
    fireEvent.click(field('form.recurring'))
    fireEvent.change(field('series.interval_count'), { target: { value: '2' } })
    fireEvent.change(field('series.interval_type'), { target: { value: 'MONTHLY' } })
    fireEvent.change(field('series.until'), { target: { value: '2027-06-30' } })

    save()

    expect(onSubmit).toHaveBeenCalledWith(initial, { intervalType: 'MONTHLY', intervalCount: 2, until: '2027-06-30' })
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
      mode: 'edit-series', initialRecurrence: { intervalType: 'MONTHLY', intervalCount: 3, until: '2027-06-30' },
    })
    expect(screen.getByText('series.edit_title')).toBeTruthy()
    expect(screen.queryByLabelText('form.recurring')).toBeNull()
    expect(field('series.interval_count').value).toBe('3')
    expect(field('series.interval_type').value).toBe('MONTHLY')
    expect(field('series.until').value).toBe('2027-06-30')

    save()

    expect(onSubmit).toHaveBeenCalledWith(initial, { intervalType: 'MONTHLY', intervalCount: 3, until: '2027-06-30' })
  })
})
