import { describe, it, expect, vi, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { CalendarApiProvider, CalendarSyncError, type CalendarApi, type SubscriptionLink } from '@/entities/calendar'
import { SubscriptionsTab } from './SubscriptionsTab'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string, o?: Record<string, unknown>) => (o ? `${key}${JSON.stringify(o)}` : key), i18n: { language: 'fr' } }),
}))

const IPHONE: SubscriptionLink = { id: 'l1', label: 'iPhone', sources: ['EVENT'], onlyMine: false, createdAt: '2026-10-01T10:00:00Z' }

function renderTab(api: Partial<CalendarApi>, isPersonal = false) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <CalendarApiProvider api={api as CalendarApi}><SubscriptionsTab spaceId="s1" isPersonal={isPersonal} /></CalendarApiProvider>
    </QueryClientProvider>)
  return client
}

const CREATED = { link: IPHONE, url: 'https://nido.test/ical/abc.ics', webcalUrl: 'webcal://nido.test/ical/abc.ics' }

async function openForm(api: Partial<CalendarApi>, isPersonal = false) {
  const client = renderTab({ listSubscriptionLinks: vi.fn().mockResolvedValue([]), ...api }, isPersonal)
  fireEvent.click(await screen.findByRole('button', { name: 'subscribe.new' }))
  return client
}

afterEach(() => { vi.restoreAllMocks() })

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
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'subscribe.new' }))
  })

  it('keeps the address out of the mutation cache once it is handed over', async () => {
    const client = await openForm({ createSubscriptionLink: vi.fn().mockResolvedValue(CREATED) })
    fireEvent.change(screen.getByLabelText('subscribe.label'), { target: { value: 'iPhone' } })
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.create' }))
    await screen.findByLabelText('subscribe.created_title')
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.done' }))
    await waitFor(() => expect(JSON.stringify(client.getMutationCache().getAll().map((m) => m.state.data) ?? [])).not.toContain('abc.ics'))
  })

  it('shows the webcal address too, and copies each address', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true })
    await openForm({ createSubscriptionLink: vi.fn().mockResolvedValue(CREATED) })
    fireEvent.change(screen.getByLabelText('subscribe.label'), { target: { value: 'iPhone' } })
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.create' }))
    expect(((await screen.findByLabelText('subscribe.webcal_title')) as HTMLInputElement).value).toBe('webcal://nido.test/ical/abc.ics')
    fireEvent.click(screen.getByRole('button', { name: /subscribe\.copy.*subscribe\.created_title/ }))
    await waitFor(() => expect(writeText).toHaveBeenCalledWith('https://nido.test/ical/abc.ics'))
    expect(await screen.findByText('subscribe.copied')).toBeTruthy()
  })

  it('says so when the address cannot be copied', async () => {
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: vi.fn().mockRejectedValue(new Error('no')) }, configurable: true })
    document.execCommand = vi.fn().mockReturnValue(false)
    await openForm({ createSubscriptionLink: vi.fn().mockResolvedValue(CREATED) })
    fireEvent.change(screen.getByLabelText('subscribe.label'), { target: { value: 'iPhone' } })
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.create' }))
    await screen.findByLabelText('subscribe.created_title')
    fireEvent.click(screen.getByRole('button', { name: /subscribe\.copy.*subscribe\.created_title/ }))
    expect((await screen.findByRole('alert')).textContent).toBe('subscribe.copy_failed')
  })

  it('limits the name to 60 characters', async () => {
    await openForm({})
    expect((screen.getByLabelText('subscribe.label') as HTMLInputElement).maxLength).toBe(60)
  })

  it('cannot create without a name or without a source', async () => {
    await openForm({})
    const create = screen.getByRole('button', { name: 'subscribe.create' }) as HTMLButtonElement
    expect(create.disabled).toBe(true)
    fireEvent.change(screen.getByLabelText('subscribe.label'), { target: { value: 'iPhone' } })
    expect(create.disabled).toBe(false)
    fireEvent.click(screen.getByLabelText('sources.EVENT'))
    expect(create.disabled).toBe(true)
  })

  it('offers "only mine" in a shared space', async () => {
    await openForm({})
    expect(screen.getByLabelText('only_mine')).toBeTruthy()
  })

  it('does not offer "only mine" in a personal space', async () => {
    await openForm({}, true)
    expect(screen.queryByLabelText('only_mine')).toBeNull()
  })

  it('says when the limit of addresses is reached', async () => {
    await openForm({ createSubscriptionLink: vi.fn().mockRejectedValue(new CalendarSyncError('too_many_links')) })
    fireEvent.change(screen.getByLabelText('subscribe.label'), { target: { value: 'iPhone' } })
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.create' }))
    expect((await screen.findByRole('alert')).textContent).toBe('errors.too_many_links')
  })

  it('forgets the error when the form is cancelled', async () => {
    await openForm({ createSubscriptionLink: vi.fn().mockRejectedValue(new CalendarSyncError('too_many_links')) })
    fireEvent.change(screen.getByLabelText('subscribe.label'), { target: { value: 'iPhone' } })
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.create' }))
    await screen.findByRole('alert')
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.cancel' }))
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.new' }))
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('words when an address was created and last read', async () => {
    renderTab({ listSubscriptionLinks: vi.fn().mockResolvedValue([{ ...IPHONE, lastReadAt: '2026-10-02T10:00:00Z' }]) })
    await screen.findByText('iPhone')
    expect(screen.getByText(/subscribe\.created_at\{"when":"[^"]+"\}/)).toBeTruthy()
    expect(screen.getByText(/subscribe\.last_read\{"when":"[^"]+"\}/)).toBeTruthy()
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

    fireEvent.click(await screen.findByRole('button', { name: /subscribe\.revoke_for/ }))
    expect(revokeSubscriptionLink).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.revoke_yes' }))
    await waitFor(() => expect(revokeSubscriptionLink).toHaveBeenCalledWith('s1', 'l1'))
    await waitFor(() => expect(screen.queryByRole('button', { name: 'subscribe.revoke_yes' })).toBeNull())
  })

  it('moves focus into the confirmation, and back to the button when it is cancelled', async () => {
    renderTab({ listSubscriptionLinks: vi.fn().mockResolvedValue([IPHONE]) })
    fireEvent.click(await screen.findByRole('button', { name: /subscribe\.revoke_for/ }))
    expect(document.activeElement?.hasAttribute('data-confirm')).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.cancel' }))
    expect(document.activeElement).toBe(screen.getByRole('button', { name: /subscribe\.revoke_for/ }))
  })

  it('says why an address could not be revoked, and forgets it on cancel', async () => {
    renderTab({
      listSubscriptionLinks: vi.fn().mockResolvedValue([IPHONE]),
      revokeSubscriptionLink: vi.fn().mockRejectedValue(new Error('boom')),
    })
    fireEvent.click(await screen.findByRole('button', { name: /subscribe\.revoke_for/ }))
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.revoke_yes' }))
    expect((await screen.findByRole('alert')).textContent).toBe('errors.server')
    fireEvent.click(screen.getByRole('button', { name: 'subscribe.cancel' }))
    fireEvent.click(screen.getByRole('button', { name: /subscribe\.revoke_for/ }))
    expect(screen.queryByRole('alert')).toBeNull()
  })
})
