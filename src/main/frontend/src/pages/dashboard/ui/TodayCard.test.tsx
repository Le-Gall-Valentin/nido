import { screen, within } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { AgendaCard, AgendaEvent } from '@/entities/dashboard'
import { TodayCard } from './TodayCard'
import { renderWithActions } from './cardTestHarness'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

function event(id: string, title: string, startTime: string | null, endTime: string | null, extra: Partial<AgendaEvent> = {}): AgendaEvent {
  return { id, title, location: null, color: 'event-violet', startDate: '2026-09-26', endDate: '2026-09-26', startTime, endTime, participantIds: [], ...extra }
}

const CARD: AgendaCard = {
  allDay: [event('e-bday', 'Anniversaire de Léa', null, null, { color: 'event-cyan' })],
  timed: [
    event('e-market', 'Marché de la place', '10:30', '12:00'),
    event('e-doctor', 'Rendez-vous chez le pédiatre', '16:00:00', '16:30:00', { location: 'Cabinet du Dr Martin', participantIds: ['u-me', 'u-cam'] }),
    event('e-dinner', 'Dîner chez Paul', '19:30', null),
  ],
  dueToday: [{ id: 't-trash', title: 'Sortir les poubelles', dueDate: '2026-09-26', priority: 'MED', status: 'TODO', assigneeIds: ['u-me'], subtasksDone: 0, subtasksTotal: 0, recurring: true }],
  tomorrow: event('e-brunch', 'Brunch chez Mamie', '11:00', null, { startDate: '2026-09-27', endDate: '2026-09-27' }),
}

const NOW = { date: '2026-09-26', time: '14:32' }

describe('TodayCard', () => {
  it('draws the now line after what has started and dims what is over', () => {
    renderWithActions(<TodayCard card={CARD} date="2026-09-26" now={NOW} />)

    const rows = screen.getAllByRole('listitem')
    const nowAt = rows.findIndex((row) => row.getAttribute('aria-label') === 'today.now:{"time":"14:32"}')
    expect(nowAt).toBe(1)
    expect(within(rows[0]).getByText('Marché de la place').closest('li')?.className).toContain('opacity-45')
    expect(within(rows[2]).getByText('Rendez-vous chez le pédiatre').closest('li')?.className).not.toContain('opacity-45')
    expect(within(rows[2]).getByText('16:00')).toBeDefined()
    expect(within(rows[2]).getByText('Cabinet du Dr Martin')).toBeDefined()
  })

  it('draws no now line on a day that is not today — a page left open past midnight', () => {
    renderWithActions(<TodayCard card={CARD} date="2026-09-26" now={{ date: '2026-09-27', time: '00:10' }} />)

    expect(screen.queryByLabelText(/today\.now/)).toBeNull()
    expect(screen.getByText('Marché de la place').closest('li')?.className).not.toContain('opacity-45')
  })

  it('lists all-day events first, today\'s to-dos, and tomorrow\'s first event', () => {
    renderWithActions(<TodayCard card={CARD} date="2026-09-26" now={NOW} />)

    expect(screen.getByText('Anniversaire de Léa')).toBeDefined()
    expect(screen.getByText('today.all_day')).toBeDefined()
    expect(screen.getByText('today.due_today')).toBeDefined()
    expect(screen.getByText('Sortir les poubelles')).toBeDefined()
    expect(screen.getByText('today.tomorrow_at:{"title":"Brunch chez Mamie","time":"11:00"}')).toBeDefined()
    expect(screen.getByRole('link', { name: 'today.link' }).getAttribute('href')).toBe('/s/space-1/organisation/calendar')
  })

  it('says until when a multi-day event runs', () => {
    renderWithActions(<TodayCard card={{ ...CARD, allDay: [event('e-trip', 'Week-end à Annecy', null, null, { endDate: '2026-09-28' })] }} date="2026-09-26" now={NOW} />)
    expect(screen.getByText('today.until_day:{"date":"28 sept."}')).toBeDefined()
  })

  it('says until what time a timed event that began on an earlier day ends today', () => {
    // A night shift, 22:00 yesterday to 06:00 today: it is not "all day" today, it ends at 06:00.
    const shift = event('e-shift', 'Garde de nuit', '22:00:00', '06:00:00', { startDate: '2026-09-25', endDate: '2026-09-26' })
    renderWithActions(<TodayCard card={{ ...CARD, allDay: [shift] }} date="2026-09-26" now={NOW} />)

    expect(screen.getByText('today.until:{"time":"06:00"}')).toBeDefined()
    expect(screen.queryByText('today.all_day')).toBeNull()
  })

  it('puts an evening that runs past midnight on the timeline at its start, until tomorrow', () => {
    const party = event('e-party', 'Soirée chez Paul', '22:00:00', '01:00:00', { endDate: '2026-09-27' })
    renderWithActions(<TodayCard card={{ ...CARD, allDay: [], timed: [party] }} date="2026-09-26" now={{ date: '2026-09-26', time: '23:30' }} />)

    const row = screen.getByText('Soirée chez Paul').closest('li') as HTMLElement
    expect(within(row).getByText('22:00')).toBeDefined()
    expect(within(row).getByText('today.until_tomorrow:{"time":"01:00"}')).toBeDefined()
    // Still going at 23:30: not dimmed as over.
    expect(row.className).not.toContain('opacity-45')
  })

  it('says until which day a timed event that lasts several days runs', () => {
    const seminar = event('e-seminar', 'Séminaire', '09:00:00', '17:00:00', { endDate: '2026-09-28' })
    renderWithActions(<TodayCard card={{ ...CARD, allDay: [], timed: [seminar] }} date="2026-09-26" now={NOW} />)

    const row = screen.getByText('Séminaire').closest('li') as HTMLElement
    expect(within(row).getByText('today.until_day:{"date":"28 sept."}')).toBeDefined()
  })

  it('says so when nothing is planned', () => {
    renderWithActions(<TodayCard card={{ allDay: [], timed: [], dueToday: [], tomorrow: null }} date="2026-09-26" now={NOW} />)
    expect(screen.getByText('today.empty')).toBeDefined()
  })
})
