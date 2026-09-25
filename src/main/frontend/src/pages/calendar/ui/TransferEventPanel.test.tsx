import { describe, it, expect, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { CalendarApiProvider, type CalendarApi, type CalendarOccurrence } from '@/entities/calendar'
import { createTestQueryClient } from '@/shared/test'
import { TransferEventPanel } from './TransferEventPanel'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

vi.mock('@/features/space-switcher', () => ({
  useWritableSpaces: () => ({ data: [{ id: 'space-2', name: 'La Famille', accent: '#c17a5c', glyph: '🏡' }] }),
}))

const piano: CalendarOccurrence = {
  source: 'EVENT', sourceId: 'series-1:2026-01-13', seriesId: 'series-1', originalDate: '2026-01-13',
  materialized: false, title: 'Piano', description: null, location: null, allDay: false,
  startDate: '2026-01-13', startTime: '18:00:00', endDate: '2026-01-13', endTime: '19:00:00',
  color: null, participantIds: [],
}

function fakeApi() {
  return {
    copyEvent: vi.fn().mockResolvedValue({}),
    moveEvent: vi.fn().mockResolvedValue({}),
    copyOccurrence: vi.fn().mockResolvedValue({}),
    moveOccurrence: vi.fn().mockResolvedValue({}),
  }
}

async function send(occurrence: CalendarOccurrence, operation: 'copy' | 'move') {
  const api = fakeApi()
  const onClose = vi.fn()
  render(
    <QueryClientProvider client={createTestQueryClient()}>
      <CalendarApiProvider api={api as unknown as CalendarApi}>
        <TransferEventPanel spaceId="space-1" occurrence={occurrence} operation={operation} onClose={onClose} />
      </CalendarApiProvider>
    </QueryClientProvider>)
  fireEvent.click(screen.getByRole('button', { name: /La Famille/ }))
  fireEvent.click(screen.getByRole('button', { name: `${operation}_submit` }))
  await waitFor(() => expect(onClose).toHaveBeenCalled())
  return api
}

describe('TransferEventPanel', () => {
  it('copies an occurrence still to come by its series and its day, having no event of its own', async () => {
    const api = await send(piano, 'copy')
    expect(api.copyOccurrence).toHaveBeenCalledWith('space-1', 'series-1', '2026-01-13', 'space-2')
    expect(api.copyEvent).not.toHaveBeenCalled()
  })

  it('moves an occurrence still to come by its series and its day', async () => {
    const api = await send(piano, 'move')
    expect(api.moveOccurrence).toHaveBeenCalledWith('space-1', 'series-1', '2026-01-13', 'space-2')
    expect(api.moveEvent).not.toHaveBeenCalled()
  })

  it('sends an occurrence edited on its own as the event it became', async () => {
    const api = await send({ ...piano, sourceId: 'e7', materialized: true }, 'move')
    expect(api.moveEvent).toHaveBeenCalledWith('space-1', 'e7', 'space-2')
    expect(api.moveOccurrence).not.toHaveBeenCalled()
  })

  it('sends a single event as itself', async () => {
    const api = await send({ ...piano, sourceId: 'e8', seriesId: null, originalDate: null, materialized: true }, 'copy')
    expect(api.copyEvent).toHaveBeenCalledWith('space-1', 'e8', 'space-2')
  })
})
