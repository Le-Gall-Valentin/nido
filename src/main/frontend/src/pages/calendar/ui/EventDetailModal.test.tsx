import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { EventDetailModal } from './EventDetailModal'
import type { CalendarOccurrence } from '@/entities/calendar'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

const concert: CalendarOccurrence = {
  source: 'EVENT', sourceId: 'e1', seriesId: null, originalDate: null, materialized: true,
  title: 'Concert', description: 'Apporter les billets.\nRendez-vous à l\'entrée nord.',
  location: 'Salle Pleyel', allDay: false, startDate: '2026-09-23', startTime: '20:00:00',
  endDate: '2026-09-23', endTime: '22:30:00', color: null, participantIds: [],
}

function renderDetail(occurrence: CalendarOccurrence, isPersonal = false) {
  return render(
    <EventDetailModal occurrence={occurrence} members={[]} currentUserId="u1" canWrite isPersonal={isPersonal}
      onEdit={vi.fn()} onDelete={vi.fn()} onCopy={vi.fn()} onMove={vi.fn()}
      onJoin={vi.fn()} onLeave={vi.fn()} onClose={vi.fn()} />)
}

describe('EventDetailModal', () => {
  it('shows where the event takes place', () => {
    renderDetail(concert)
    expect(screen.getByText('Salle Pleyel')).toBeTruthy()
  })

  it('shows the description in full, line breaks kept', () => {
    renderDetail(concert)
    const description = screen.getByTestId('event-description')
    expect(description.textContent).toBe('Apporter les billets.\nRendez-vous à l\'entrée nord.')
    expect(description.className).toContain('whitespace-pre-line')
  })

  it('names the day in full and gives the time span', () => {
    renderDetail(concert)
    expect(screen.getByText('Mercredi 23 septembre 2026 · 20:00 – 22:30')).toBeTruthy()
  })

  it('leaves out location and description when the event has neither', () => {
    renderDetail({ ...concert, location: null, description: null })
    expect(screen.queryByTestId('event-location')).toBeNull()
    expect(screen.queryByTestId('event-description')).toBeNull()
  })

  it('offers joining and shows who takes part in a shared context', () => {
    renderDetail(concert)
    expect(screen.getByText('detail.participants')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'detail.join' })).toBeTruthy()
  })

  it('says nothing of participants in a personal context — its owner always takes part', () => {
    renderDetail({ ...concert, participantIds: ['u1'] }, true)
    expect(screen.queryByText('detail.participants')).toBeNull()
    expect(screen.queryByRole('button', { name: 'detail.join' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'detail.leave' })).toBeNull()
  })
})
