import { describe, it, expect, vi } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { CalendarApiProvider, type CalendarApi, type CalendarOccurrence } from '@/entities/calendar'
import { renderWithQuery } from '@/shared/test'
import { DeleteEventPanel } from './DeleteEventPanel'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

const dentist: CalendarOccurrence = {
  source: 'EVENT', sourceId: 'e1', seriesId: null, originalDate: null, materialized: true,
  title: 'Dentiste', description: null, location: null, allDay: false,
  startDate: '2026-10-06', startTime: '09:00:00', endDate: '2026-10-06', endTime: '10:00:00',
  color: null, participantIds: [],
}
const piano: CalendarOccurrence = {
  ...dentist, sourceId: 'series-1:2026-10-13', seriesId: 'series-1', originalDate: '2026-10-13',
  materialized: false, title: 'Piano', startDate: '2026-10-13',
}

function renderPanel(occurrence: CalendarOccurrence, fails = false) {
  const outcome = () => (fails ? Promise.reject(new Error('down')) : Promise.resolve())
  const api = { deleteEvent: vi.fn(outcome), excludeOccurrence: vi.fn(outcome), listOccurrences: vi.fn().mockResolvedValue([]) }
  const onClose = vi.fn()
  renderWithQuery(
    <CalendarApiProvider api={api as unknown as CalendarApi}>
      <DeleteEventPanel spaceId="space-1" occurrence={occurrence} onClose={onClose} />
    </CalendarApiProvider>)
  return { api, onClose }
}

describe('DeleteEventPanel', () => {
  it('deletes a single event, with the generic warning', async () => {
    const { api, onClose } = renderPanel(dentist)
    expect(screen.getByText('delete_confirm.message')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'delete_confirm.confirm' }))
    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(api.deleteEvent).toHaveBeenCalledWith('space-1', 'e1')
    expect(api.excludeOccurrence).not.toHaveBeenCalled()
  })

  it('cancels an occurrence still to come on its slot, since it has no event to delete', async () => {
    const { api, onClose } = renderPanel(piano)
    expect(screen.getByText('delete.occurrence_message')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'delete_confirm.confirm' }))
    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(api.excludeOccurrence).toHaveBeenCalledWith('space-1', 'series-1', '2026-10-13')
    expect(api.deleteEvent).not.toHaveBeenCalled()
  })

  it('deletes an occurrence edited on its own as the event it became', async () => {
    const { api, onClose } = renderPanel({ ...piano, sourceId: 'e7', materialized: true })
    expect(screen.getByText('delete.occurrence_message')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'delete_confirm.confirm' }))
    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(api.deleteEvent).toHaveBeenCalledWith('space-1', 'e7')
  })

  it('says so and stays open when the deletion fails', async () => {
    const { onClose } = renderPanel(dentist, true)
    fireEvent.click(screen.getByRole('button', { name: 'delete_confirm.confirm' }))
    expect(await screen.findByText('delete.failed')).toBeTruthy()
    expect(onClose).not.toHaveBeenCalled()
  })
})
