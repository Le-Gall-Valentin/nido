import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { CalendarApiProvider, CalendarSyncError, type CalendarApi, type SubscriptionLink } from '@/entities/calendar'
import { SubscriptionsTab } from './SubscriptionsTab'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string, o?: Record<string, unknown>) => (o ? `${key}${JSON.stringify(o)}` : key), i18n: { language: 'fr' } }),
}))

const IPHONE: SubscriptionLink = { id: 'l1', label: 'iPhone', sources: ['EVENT'], onlyMine: false, createdAt: '2026-10-01T10:00:00Z' }

function renderTab(api: Partial<CalendarApi>) {
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <CalendarApiProvider api={api as CalendarApi}><SubscriptionsTab spaceId="s1" isPersonal={false} /></CalendarApiProvider>
    </QueryClientProvider>)
}

describe('SubscriptionsTab', () => {
  it('lists the reader\'s own addresses, said read or not yet', async () => {
    renderTab({ listSubscriptionLinks: vi.fn().mockResolvedValue([IPHONE]) })
    expect(await screen.findByText('iPhone')).toBeTruthy()
    expect(screen.getByText(/subscribe\.never_read/)).toBeTruthy()
  })

  it('creates an address and shows it once, with its webcal link', async () => {
    const createSubscriptionLink = vi.fn().mockResolvedValue({
      link: IPHONE, url: 'https://nido.test/ical/abc.ics', webcalUrl: 'webcal://nido.test/ical/abc.ics',
    })
    renderTab({ listSubscriptionLinks: vi.fn().mockResolvedValue([]), createSubscriptionLink })

    fireEvent.click(await screen.findByRole('button', { name: 'subscribe.new' }))
    fireEvent.change(screen.getByLabelText('subscribe.label'), { target: { value: 'iPhone' } })
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.create' }))

    expect(((await screen.findByLabelText('subscribe.created_title')) as HTMLInputElement).value).toBe('https://nido.test/ical/abc.ics')
    expect(screen.getByRole('link', { name: 'subscribe.open' }).getAttribute('href')).toBe('webcal://nido.test/ical/abc.ics')
    expect(createSubscriptionLink).toHaveBeenCalledWith('s1', { label: 'iPhone', sources: ['EVENT'], onlyMine: false })

    fireEvent.click(screen.getByRole('button', { name: 'subscribe.done' }))
    expect(screen.queryByLabelText('subscribe.created_title')).toBeNull()
  })

  it('says why an address could not be made', async () => {
    renderTab({
      listSubscriptionLinks: vi.fn().mockResolvedValue([]),
      createSubscriptionLink: vi.fn().mockRejectedValue(new CalendarSyncError('public_url_missing')),
    })
    fireEvent.click(await screen.findByRole('button', { name: 'subscribe.new' }))
    fireEvent.change(screen.getByLabelText('subscribe.label'), { target: { value: 'iPhone' } })
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.create' }))
    expect((await screen.findByRole('alert')).textContent).toBe('errors.public_url_missing')
  })

  it('revokes an address once confirmed', async () => {
    const revokeSubscriptionLink = vi.fn().mockResolvedValue(undefined)
    renderTab({ listSubscriptionLinks: vi.fn().mockResolvedValue([IPHONE]), revokeSubscriptionLink })

    fireEvent.click(await screen.findByRole('button', { name: 'subscribe.revoke' }))
    expect(revokeSubscriptionLink).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.revoke_yes' }))
    await waitFor(() => expect(revokeSubscriptionLink).toHaveBeenCalledWith('s1', 'l1'))
  })
})
