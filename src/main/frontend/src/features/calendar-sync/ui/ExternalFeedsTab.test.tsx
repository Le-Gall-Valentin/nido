import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { CalendarApiProvider, CalendarSyncError, EVENT_DOT_CLASS, type CalendarApi, type ExternalFeed } from '@/entities/calendar'
import { ExternalFeedsTab } from './ExternalFeedsTab'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string, o?: Record<string, unknown>) => (o ? `${key}${JSON.stringify(o)}` : key), i18n: { language: 'fr' } }),
}))

const SCHOOL: ExternalFeed = {
  id: 'f1', name: 'Vacances scolaires', host: 'ecole.example', color: 'event-cyan',
  contentFetchedAt: '2026-10-11T10:00:00Z', lastAttemptAt: '2026-10-13T10:00:00Z', lastError: 'TIMEOUT',
}
const CLUB: ExternalFeed = { ...SCHOOL, id: 'f2', name: 'Club', host: 'club.example', lastError: null }

function renderTab(api: Partial<CalendarApi>, canWrite = true) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <CalendarApiProvider api={api as CalendarApi}><ExternalFeedsTab spaceId="s1" canWrite={canWrite} /></CalendarApiProvider>
    </QueryClientProvider>)
  return client
}

async function openForm(api: Partial<CalendarApi>) {
  const client = renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([]), ...api })
  fireEvent.click(await screen.findByRole('button', { name: 'feeds.follow' }))
  fireEvent.change(screen.getByLabelText('feeds.url'), { target: { value: 'https://x/a.ics' } })
  return client
}

describe('ExternalFeedsTab', () => {
  it('lists each calendar with its host and why its last attempt failed', async () => {
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([SCHOOL]) })
    expect(await screen.findByText('Vacances scolaires')).toBeTruthy()
    expect(screen.getByText(/ecole\.example/)).toBeTruthy()
    expect(screen.getByText(/feeds\.failed_since.*feed_error\.TIMEOUT/)).toBeTruthy()
  })

  it('follows a calendar at an address, its colour picked, and announces it', async () => {
    const followExternalFeed = vi.fn().mockResolvedValue(SCHOOL)
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([]), followExternalFeed })

    fireEvent.click(await screen.findByRole('button', { name: 'feeds.follow' }))
    fireEvent.change(screen.getByLabelText('feeds.url'), { target: { value: 'webcal://ecole.example/v.ics' } })
    fireEvent.click(screen.getByRole('radio', { name: 'colors.event-indigo' }))
    fireEvent.click(screen.getByRole('button', { name: 'feeds.save' }))

    await waitFor(() => expect(followExternalFeed).toHaveBeenCalledWith('s1',
      { url: 'webcal://ecole.example/v.ics', name: null, color: 'event-indigo' }))
    await waitFor(() => expect(screen.getByRole('status').textContent).toContain('feeds.followed'))
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'feeds.follow' }))
  })

  it('keeps the address followed, which may hold a secret, out of the mutation cache once it is followed', async () => {
    const client = await openForm({ followExternalFeed: vi.fn().mockResolvedValue(SCHOOL) })
    fireEvent.change(screen.getByLabelText('feeds.url'), { target: { value: 'https://calendar.example/private-abc123/basic.ics' } })
    fireEvent.click(screen.getByRole('button', { name: 'feeds.save' }))

    await waitFor(() => expect(screen.getByRole('status').textContent).toContain('feeds.followed'))
    await waitFor(() => expect(JSON.stringify(client.getMutationCache().getAll().map((m) => m.state.variables))).not.toContain('private-abc123'))
  })

  it('says why an address could not be followed, and forgets it on cancel', async () => {
    await openForm({ followExternalFeed: vi.fn().mockRejectedValue(new CalendarSyncError('feed_unavailable', 'HTTP_404')) })
    fireEvent.click(screen.getByRole('button', { name: 'feeds.save' }))
    expect((await screen.findByRole('alert')).textContent).toContain('errors.feed_unavailable{"reason":"feed_error.HTTP{\\"status\\":\\"404\\"}"}')

    fireEvent.click(screen.getByRole('button', { name: 'feeds.cancel' }))
    fireEvent.click(screen.getByRole('button', { name: 'feeds.follow' }))
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('takes an address no longer than the server does', async () => {
    await openForm({})
    expect(screen.getByLabelText('feeds.url').getAttribute('maxlength')).toBe('2000')
  })

  it('shows a busy state and holds the button, focus kept on it, while the calendar downloads', async () => {
    let finish: (feed: ExternalFeed) => void = () => {}
    const followExternalFeed = vi.fn().mockReturnValue(new Promise<ExternalFeed>((resolve) => { finish = resolve }))
    await openForm({ followExternalFeed })
    const save = screen.getByRole('button', { name: 'feeds.save' }) as HTMLButtonElement
    save.focus()
    fireEvent.click(save)
    expect((await screen.findByText('feeds.saving')).closest('[role="status"]')).toBeTruthy()
    expect(save.getAttribute('aria-disabled')).toBe('true')
    expect(save.disabled).toBe(false)
    expect(document.activeElement).toBe(save)
    fireEvent.click(save)
    expect(followExternalFeed).toHaveBeenCalledTimes(1)
    finish(SCHOOL)
    await waitFor(() => expect(screen.queryByText('feeds.saving')).toBeNull())
  })

  it('refreshes a calendar, naming it on the button, and announces it', async () => {
    const refreshExternalFeed = vi.fn().mockResolvedValue({ ...SCHOOL, lastError: null })
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([SCHOOL, CLUB]), refreshExternalFeed })
    fireEvent.click(await screen.findByRole('button', { name: /feeds\.refresh_for.*Vacances scolaires/ }))
    await waitFor(() => expect(refreshExternalFeed).toHaveBeenCalledWith('s1', 'f1'))
    await waitFor(() => expect(screen.getByRole('status').textContent).toContain('feeds.refreshed'))
  })

  it('announces a refresh that could not read the calendar as such, with why', async () => {
    // The server answers 200 with the failure recorded: the calendar is not up to date.
    const refreshExternalFeed = vi.fn().mockResolvedValue({ ...SCHOOL, lastError: 'HTTP_404' })
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([SCHOOL, CLUB]), refreshExternalFeed })
    fireEvent.click(await screen.findByRole('button', { name: /feeds\.refresh_for.*Vacances scolaires/ }))
    await waitFor(() => expect(screen.getByRole('status').textContent)
      .toBe('feeds.refresh_failed{"name":"Vacances scolaires","reason":"feed_error.HTTP{\\"status\\":\\"404\\"}"}'))
  })

  it('follows a calendar in the graphite of external sources when no colour is picked', async () => {
    const followExternalFeed = vi.fn().mockResolvedValue(SCHOOL)
    await openForm({ followExternalFeed })
    expect(screen.getByRole('radio', { name: 'colors.event-graphite' }).getAttribute('aria-checked')).toBe('true')
    fireEvent.click(screen.getByRole('button', { name: 'feeds.save' }))
    await waitFor(() => expect(followExternalFeed).toHaveBeenCalledWith('s1', { url: 'https://x/a.ics', name: null, color: 'event-graphite' }))
  })

  it('says a refresh failed on the row it was asked on only', async () => {
    renderTab({
      listExternalFeeds: vi.fn().mockResolvedValue([SCHOOL, CLUB]),
      refreshExternalFeed: vi.fn().mockRejectedValue(new CalendarSyncError('refresh_too_soon')),
    })
    fireEvent.click(await screen.findByRole('button', { name: /feeds\.refresh_for.*Vacances/ }))
    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain('errors.refresh_too_soon')
    expect(screen.getByText('Club').closest('li')?.contains(alert)).toBe(false)
    fireEvent.click(screen.getByRole('button', { name: /feeds\.edit_for.*Club/ }))
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('renames a calendar, then puts focus back on its edit button', async () => {
    const updateExternalFeed = vi.fn().mockResolvedValue(SCHOOL)
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([SCHOOL]), updateExternalFeed })
    fireEvent.click(await screen.findByRole('button', { name: /feeds\.edit_for/ }))
    fireEvent.change(screen.getByLabelText('feeds.rename'), { target: { value: 'École' } })
    fireEvent.click(screen.getByRole('radio', { name: /colors\.event-indigo|feeds\.color_for.*event-indigo/ }))
    fireEvent.click(screen.getByRole('button', { name: 'feeds.update' }))
    await waitFor(() => expect(updateExternalFeed).toHaveBeenCalledWith('s1', 'f1', { name: 'École', color: 'event-indigo' }))
    await waitFor(() => expect(screen.getByRole('status').textContent).toContain('feeds.saved'))
    expect(document.activeElement).toBe(screen.getByRole('button', { name: /feeds\.edit_for/ }))
  })

  it('names the calendar on each colour button of its editor', async () => {
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([SCHOOL]) })
    fireEvent.click(await screen.findByRole('button', { name: /feeds\.edit_for/ }))
    expect(screen.getAllByRole('radio', { name: /feeds\.color_for.*Vacances scolaires/ }).length).toBeGreaterThan(1)
  })

  it('asks before it stops following, and announces it', async () => {
    const removeExternalFeed = vi.fn().mockResolvedValue(undefined)
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([SCHOOL]), removeExternalFeed })
    fireEvent.click(await screen.findByRole('button', { name: /feeds\.remove_for/ }))
    expect(removeExternalFeed).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'feeds.cancel' }))
    expect(document.activeElement).toBe(screen.getByRole('button', { name: /feeds\.remove_for/ }))

    fireEvent.click(screen.getByRole('button', { name: /feeds\.remove_for/ }))
    fireEvent.click(screen.getByRole('button', { name: 'feeds.remove_yes' }))
    await waitFor(() => expect(removeExternalFeed).toHaveBeenCalledWith('s1', 'f1'))
    await waitFor(() => expect(screen.getByRole('status').textContent).toContain('feeds.removed'))
    expect(document.activeElement).not.toBe(document.body)
  })

  it('keeps one status region across the form and the list, so the follow is announced', async () => {
    await openForm({ followExternalFeed: vi.fn().mockResolvedValue(SCHOOL) })
    const region = screen.getByRole('status')
    fireEvent.click(screen.getByRole('button', { name: 'feeds.save' }))
    await waitFor(() => expect(screen.getByRole('status').textContent).toContain('feeds.followed'))
    expect(screen.getByRole('status')).toBe(region)
  })

  it('cannot cancel a follow while the calendar downloads', async () => {
    await openForm({ followExternalFeed: vi.fn().mockReturnValue(new Promise(() => {})) })
    fireEvent.click(screen.getByRole('button', { name: 'feeds.save' }))
    await screen.findByText('feeds.saving')
    expect((screen.getByRole('button', { name: 'feeds.cancel' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('blocks a second refresh while one runs', async () => {
    const refreshExternalFeed = vi.fn().mockReturnValue(new Promise(() => {}))
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([SCHOOL, CLUB]), refreshExternalFeed })
    fireEvent.click(await screen.findByRole('button', { name: /feeds\.refresh_for.*Vacances/ }))
    const other = screen.getByRole('button', { name: /feeds\.refresh_for.*Club/ })
    await waitFor(() => expect(other.getAttribute('aria-disabled')).toBe('true'))
    fireEvent.click(other)
    expect(refreshExternalFeed).toHaveBeenCalledTimes(1)
  })

  it.each([['too_many_feeds'], ['invalid_feed_address']] as const)('says %s when the follow is refused', async (reason) => {
    await openForm({ followExternalFeed: vi.fn().mockRejectedValue(new CalendarSyncError(reason)) })
    fireEvent.click(screen.getByRole('button', { name: 'feeds.save' }))
    expect((await screen.findByRole('alert')).textContent).toBe(`errors.${reason}`)
  })

  it('gives the button back, focus still on it, when the follow is refused', async () => {
    await openForm({ followExternalFeed: vi.fn().mockRejectedValue(new CalendarSyncError('invalid_feed_address')) })
    const save = screen.getByRole('button', { name: 'feeds.save' })
    save.focus()
    fireEvent.click(save)
    await screen.findByRole('alert')
    expect(save.getAttribute('aria-disabled')).toBeNull()
    expect(document.activeElement).toBe(save)
  })

  it('shows a calendar of an unknown colour in graphite, in the list and in its editor', async () => {
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([{ ...SCHOOL, color: 'mauve' }]) })
    const dot = (await screen.findByText('Vacances scolaires')).closest('li')?.querySelector('span[aria-hidden]')
    expect(dot?.className).toContain(EVENT_DOT_CLASS['event-graphite'])
    fireEvent.click(screen.getByRole('button', { name: /feeds\.edit_for/ }))
    expect(screen.getByRole('radio', { name: /feeds\.color_for.*colors\.event-graphite/ }).getAttribute('aria-checked')).toBe('true')
  })

  it('shows a viewer the calendars, and nothing to change them', async () => {
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([SCHOOL]) }, false)
    expect(await screen.findByText('Vacances scolaires')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'feeds.follow' })).toBeNull()
    expect(screen.queryByRole('button', { name: /feeds\.refresh/ })).toBeNull()
  })

  it('says a list that could not be read, and reads it again on request, rather than follow nothing', async () => {
    const listExternalFeeds = vi.fn().mockRejectedValueOnce(new CalendarSyncError('server')).mockResolvedValue([CLUB])
    renderTab({ listExternalFeeds })

    expect((await screen.findByRole('alert')).textContent).toBe('errors.server')
    expect(screen.queryByText('feeds.empty')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'retry' }))

    expect(await screen.findByText('Club')).toBeTruthy()
  })

  it('lets no other action of a row start while one runs, and says the outcome of the one running', async () => {
    let refreshed: (feed: ExternalFeed) => void = () => {}
    const refreshExternalFeed = vi.fn(() => new Promise<ExternalFeed>((resolve) => { refreshed = resolve }))
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([SCHOOL, CLUB]), refreshExternalFeed })
    fireEvent.click(await screen.findByRole('button', { name: /feeds\.refresh_for.*Vacances scolaires/ }))
    await waitFor(() => expect(refreshExternalFeed).toHaveBeenCalledTimes(1))

    fireEvent.click(screen.getByRole('button', { name: /feeds\.edit_for.*Club/ }))
    fireEvent.click(screen.getByRole('button', { name: /feeds\.refresh_for.*Club/ }))

    expect(refreshExternalFeed).toHaveBeenCalledTimes(1)
    expect(screen.queryByLabelText('feeds.rename')).toBeNull()
    refreshed({ ...SCHOOL, lastError: null })
    expect(await screen.findByText(/feeds\.refreshed.*Vacances scolaires/)).toBeTruthy()
  })

  it('says a removal that failed, and keeps the calendar', async () => {
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([CLUB]),
      removeExternalFeed: vi.fn().mockRejectedValue(new CalendarSyncError('forbidden')) })
    fireEvent.click(await screen.findByRole('button', { name: /feeds\.remove_for.*Club/ }))
    fireEvent.click(screen.getByRole('button', { name: 'feeds.remove_yes' }))

    expect((await screen.findByRole('alert')).textContent).toBe('errors.forbidden')
    expect(screen.getByText('Club')).toBeTruthy()
  })

  it('says a change that failed, and keeps the form open with what was typed', async () => {
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([CLUB]),
      updateExternalFeed: vi.fn().mockRejectedValue(new CalendarSyncError('rate_limited')) })
    fireEvent.click(await screen.findByRole('button', { name: /feeds\.edit_for.*Club/ }))
    fireEvent.change(screen.getByLabelText('feeds.rename'), { target: { value: 'Club de foot' } })
    fireEvent.click(screen.getByRole('button', { name: 'feeds.update' }))

    expect((await screen.findByRole('alert')).textContent).toBe('errors.rate_limited')
    expect((screen.getByLabelText('feeds.rename') as HTMLInputElement).value).toBe('Club de foot')
  })

  it.each([
    ['the change is saved', 'updateExternalFeed', /feeds\.edit_for.*Club/, 'feeds.update'],
    ['the calendar is removed', 'removeExternalFeed', /feeds\.remove_for.*Club/, 'feeds.remove_yes'],
  ] as const)('holds the button, focus kept on it, while %s, and does it once', async (_, call, opener, label) => {
    const run = vi.fn().mockReturnValue(new Promise(() => {}))
    renderTab({ listExternalFeeds: vi.fn().mockResolvedValue([CLUB]), [call]: run })
    fireEvent.click(await screen.findByRole('button', { name: opener }))
    const button = screen.getByRole('button', { name: label }) as HTMLButtonElement
    button.focus()
    fireEvent.click(button)
    await waitFor(() => expect(button.getAttribute('aria-disabled')).toBe('true'))
    expect(button.disabled).toBe(false)
    expect(document.activeElement).toBe(button)
    fireEvent.click(button)
    expect(run).toHaveBeenCalledTimes(1)
  })

  it('opens the next follow in the colour of external sources, whatever the last one was in', async () => {
    const followExternalFeed = vi.fn().mockResolvedValue(CLUB)
    await openForm({ followExternalFeed })
    fireEvent.click(screen.getByRole('radio', { name: 'colors.event-indigo' }))
    fireEvent.click(screen.getByRole('button', { name: 'feeds.save' }))
    await waitFor(() => expect(followExternalFeed).toHaveBeenCalled())

    fireEvent.click(await screen.findByRole('button', { name: 'feeds.follow' }))

    expect(screen.getByRole('radio', { name: 'colors.event-graphite' }).getAttribute('aria-checked')).toBe('true')
  })
})
