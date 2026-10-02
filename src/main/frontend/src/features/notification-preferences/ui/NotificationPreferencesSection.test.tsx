import { fireEvent, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'
import { renderWithQuery } from '@/shared/test'
import type { INotificationPreferencesApi } from '../model/INotificationPreferencesApi'
import type { NotificationPreferences } from '../model/types'
import { NOTIFICATION_PREFERENCES_QUERY_KEY } from '../model/useNotificationPreferences'
import { NotificationPreferencesSection } from './NotificationPreferencesSection'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const PREFERENCES: NotificationPreferences = {
  channels: [{ channel: 'email', enabled: true }],
  types: [{ type: 'space.invitation', enabled: true }],
}

function fakeApi(overrides: Partial<INotificationPreferencesApi> = {}): INotificationPreferencesApi {
  return {
    get: vi.fn().mockResolvedValue(PREFERENCES),
    setChannel: vi.fn().mockResolvedValue(undefined),
    setType: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }
}

// Re-queried after every step: a switch may be re-rendered, never trust a stale element.
const kindSwitch = () => screen.getByRole('switch', { name: 'type.space.invitation.label' }) as HTMLButtonElement
const mailSwitch = () => screen.getByRole('switch', { name: 'channel.email.label' }) as HTMLButtonElement

describe('NotificationPreferencesSection', () => {
  beforeEach(() => localStorage.clear())

  it('shows nothing while the choices load', () => {
    const { container } = renderWithQuery(
      <NotificationPreferencesSection api={fakeApi({ get: vi.fn(() => new Promise<NotificationPreferences>(() => {})) })} />)

    expect(container.firstChild).toBeNull()
  })

  it('shows nothing on an installation without a channel', async () => {
    const api = fakeApi({ get: vi.fn().mockResolvedValue({ channels: [], types: PREFERENCES.types }) })
    const { container, queryClient } = renderWithQuery(<NotificationPreferencesSection api={api} />)

    await waitFor(() => expect(queryClient.getQueryData(NOTIFICATION_PREFERENCES_QUERY_KEY)).toBeDefined())
    expect(container.firstChild).toBeNull()
  })

  it('lists the channel and every kind under its context, all on, and says security mails always go', async () => {
    renderWithQuery(<NotificationPreferencesSection api={fakeApi()} />)

    expect(await screen.findByRole('heading', { name: 'title' })).toBeDefined()
    expect(screen.getByText('channels_title')).toBeDefined()
    expect(screen.getByText('group.space')).toBeDefined()
    expect(mailSwitch().getAttribute('aria-checked')).toBe('true')
    expect(kindSwitch().getAttribute('aria-checked')).toBe('true')
    expect(screen.getByText('security_note')).toBeDefined()
    expect(screen.queryByText('no_channel')).toBeNull()
  })

  it('describes each switch by the line it sits on', async () => {
    renderWithQuery(<NotificationPreferencesSection api={fakeApi()} />)

    const control = await screen.findByRole('switch', { name: 'type.space.invitation.label' })
    const description = document.getElementById(control.getAttribute('aria-describedby') ?? '')
    expect(description?.textContent).toBe('type.space.invitation.description')
  })

  it('switches a kind off at once and tells the server', async () => {
    const api = fakeApi()
    renderWithQuery(<NotificationPreferencesSection api={api} />)

    fireEvent.click(await screen.findByRole('switch', { name: 'type.space.invitation.label' }))

    await waitFor(() => expect(kindSwitch().getAttribute('aria-checked')).toBe('false'))
    expect(api.setType).toHaveBeenCalledWith('space.invitation', false)
  })

  it('switches the mail channel off and tells the server', async () => {
    const api = fakeApi()
    renderWithQuery(<NotificationPreferencesSection api={api} />)

    fireEvent.click(await screen.findByRole('switch', { name: 'channel.email.label' }))

    await waitFor(() => expect(mailSwitch().getAttribute('aria-checked')).toBe('false'))
    expect(api.setChannel).toHaveBeenCalledWith('email', false)
  })

  it('says nothing will arrive when every channel is off, and keeps the kinds usable', async () => {
    const api = fakeApi({ get: vi.fn().mockResolvedValue({ ...PREFERENCES, channels: [{ channel: 'email', enabled: false }] }) })
    renderWithQuery(<NotificationPreferencesSection api={api} />)

    expect(await screen.findByText('no_channel')).toBeDefined()
    expect(kindSwitch().disabled).toBe(false)
  })

  it('holds a switch while its request is in flight, and only that one', async () => {
    const api = fakeApi({ setType: vi.fn(() => new Promise<void>(() => {})) })
    renderWithQuery(<NotificationPreferencesSection api={api} />)

    fireEvent.click(await screen.findByRole('switch', { name: 'type.space.invitation.label' }))

    await waitFor(() => expect(kindSwitch().disabled).toBe(true))
    expect(mailSwitch().disabled).toBe(false)
  })

  it.each([
    ['RateLimitError', new RateLimitError(), 'errors.rate_limit'],
    ['NetworkError', new NetworkError(), 'errors.network'],
    ['ServerError', new ServerError(), 'errors.server'],
  ])('puts a switch back when the server refuses it, and says why (%s)', async (_name, failure, message) => {
    renderWithQuery(<NotificationPreferencesSection api={fakeApi({ setType: vi.fn().mockRejectedValue(failure) })} />)

    fireEvent.click(await screen.findByRole('switch', { name: 'type.space.invitation.label' }))

    expect(await screen.findByText(message)).toBeDefined()
    expect(kindSwitch().getAttribute('aria-checked')).toBe('true')
    expect(kindSwitch().disabled).toBe(false)
  })

  it('says when the choices cannot be loaded', async () => {
    renderWithQuery(<NotificationPreferencesSection api={fakeApi({ get: vi.fn().mockRejectedValue(new NetworkError()) })} />)

    expect(await screen.findByText('errors.load')).toBeDefined()
  })

  it('folds a context away and says how many of its kinds are on', async () => {
    renderWithQuery(<NotificationPreferencesSection api={fakeApi()} />)
    const header = await screen.findByRole('button', { name: 'group.space' })
    expect(header.getAttribute('aria-expanded')).toBe('true')

    fireEvent.click(header)

    expect(screen.getByRole('button', { name: /group\.space/ }).getAttribute('aria-expanded')).toBe('false')
    expect(screen.queryByRole('switch', { name: 'type.space.invitation.label' })).toBeNull()
    expect(screen.getByText('group_summary')).toBeDefined()
    expect(screen.getByRole('switch', { name: 'channel.email.label' })).toBeDefined()
  })

  it('remembers on this device which contexts are folded', async () => {
    const { unmount } = renderWithQuery(<NotificationPreferencesSection api={fakeApi()} />)
    fireEvent.click(await screen.findByRole('button', { name: 'group.space' }))
    unmount()

    renderWithQuery(<NotificationPreferencesSection api={fakeApi()} />)

    expect((await screen.findByRole('button', { name: /group\.space/ })).getAttribute('aria-expanded')).toBe('false')
  })

  it('never folds the channels', async () => {
    renderWithQuery(<NotificationPreferencesSection api={fakeApi()} />)

    await screen.findByRole('switch', { name: 'channel.email.label' })
    expect(screen.queryByRole('button', { name: 'channels_title' })).toBeNull()
  })
})
