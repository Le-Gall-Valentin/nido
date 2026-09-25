import { describe, it, expect, vi } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { CalendarApiProvider, type CalendarApi, type RecurringEventSeries } from '@/entities/calendar'
import { renderWithQuery } from '@/shared/test'
import { DeleteSeriesPanel } from './DeleteSeriesPanel'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

const piano: RecurringEventSeries = {
  id: 'series-1', title: 'Piano', description: null, location: null, allDay: false,
  startTime: '18:00:00', endTime: '19:00:00', durationDays: 0, color: null,
  intervalType: 'WEEKLY', intervalCount: 1, anchorDate: '2026-09-01', endDate: null,
  startsOn: null, firstDate: '2026-09-01', participantIds: [], createdBy: 'u1', createdAt: '2026-09-01T00:00:00Z',
}

function renderPanel(series: RecurringEventSeries, fails = false) {
  const api = {
    deleteRecurringEventSeries: vi.fn(() => (fails ? Promise.reject(new Error('down')) : Promise.resolve())),
    listOccurrences: vi.fn().mockResolvedValue([]),
  }
  const onClose = vi.fn()
  renderWithQuery(
    <CalendarApiProvider api={api as unknown as CalendarApi}>
      <DeleteSeriesPanel spaceId="space-1" series={series} today="2026-10-01" onClose={onClose} />
    </CalendarApiProvider>)
  return { api, onClose }
}

describe('DeleteSeriesPanel', () => {
  it('warns that a series already begun stops, its past kept', async () => {
    const { api, onClose } = renderPanel(piano)
    expect(screen.getByText('series.stop_message')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'delete_confirm.confirm' }))
    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(api.deleteRecurringEventSeries).toHaveBeenCalledWith('space-1', 'series-1')
  })

  it('warns that a series not begun yet goes whole', () => {
    renderPanel({ ...piano, anchorDate: '2026-11-01', firstDate: '2026-11-01' })
    expect(screen.getByText('series.delete_message')).toBeTruthy()
  })

  it('warns that a series already over goes whole', () => {
    renderPanel({ ...piano, endDate: '2026-09-20' })
    expect(screen.getByText('series.delete_message')).toBeTruthy()
  })

  it('says so and stays open when the deletion fails', async () => {
    const { onClose } = renderPanel(piano, true)
    fireEvent.click(screen.getByRole('button', { name: 'delete_confirm.confirm' }))
    expect(await screen.findByText('delete.failed')).toBeTruthy()
    expect(onClose).not.toHaveBeenCalled()
  })
})
