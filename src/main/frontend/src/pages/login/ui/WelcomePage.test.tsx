import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { StoreApi } from 'zustand'
import * as auth from '@/features/auth'
import type { User } from '@/entities/user'
import type { IAccountInvitationApi } from '@/features/account-invitation'
import type { ICapabilitiesApi } from '@/entities/capabilities'
import { InvalidLinkError, NetworkError } from '@/shared/lib'
import { createTestQueryClient } from '@/shared/test'
import { WelcomePage } from './WelcomePage'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: object) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

interface FakeAuth {
  user: User | null
  isInitializing: boolean
  logout: () => Promise<void>
}

vi.mock('@/features/auth', async () => {
  const { create, useStore } = await import('zustand')
  const store = create<FakeAuth>((set) => ({
    user: null,
    isInitializing: false,
    logout: vi.fn(async () => { set({ user: null }) }),
  }))
  return {
    useAuth: <T,>(selector: (state: FakeAuth) => T) => useStore(store, selector),
    __store: store,
  }
})

const authStore = (auth as unknown as { __store: StoreApi<FakeAuth> }).__store
const jane: User = {
  id: 'u-1', username: 'jane', email: 'jane@test.com', role: 'USER', createdAt: '2026-01-01T00:00:00Z', totpEnabled: false,
}

beforeEach(() => {
  authStore.setState({ user: null, isInitializing: false })
})

function Where() {
  const location = useLocation()
  return <output data-testid="where">{JSON.stringify({ path: location.pathname, hash: location.hash, state: location.state })}</output>
}

function open(url: string, api: Partial<IAccountInvitationApi>, { mail = true } = {}) {
  const full: IAccountInvitationApi = {
    checkInvitation: vi.fn().mockResolvedValue('carol'), acceptInvitation: vi.fn().mockResolvedValue(undefined), ...api,
  }
  const capabilitiesApi: ICapabilitiesApi = { capabilities: vi.fn().mockResolvedValue({ passwordReset: mail, mail }) }
  render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter initialEntries={[url]}>
        <Routes>
          <Route path="/welcome" element={<><WelcomePage api={full} capabilitiesApi={capabilitiesApi} /><Where /></>} />
          <Route path="/login" element={<Where />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return full
}

const where = () => JSON.parse(screen.getByTestId('where').textContent ?? '{}') as { path: string; hash: string; state: unknown }
const type = (label: string, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } })

describe('WelcomePage', () => {
  it('checks the link at once with its token alone and takes it out of the address bar', async () => {
    const api = open('/welcome#token=abc&utm_source=chat', {})

    await waitFor(() => expect(screen.getByRole('heading', { level: 1 }).textContent)
      .toBe('welcome.title:{"username":"carol"}'))
    expect(api.checkInvitation).toHaveBeenCalledWith('abc')
    expect(where().hash).toBe('')
  })

  it('saves the first password, then opens the login page with the identifier to fill in', async () => {
    const api = open('/welcome#token=abc', {})
    await screen.findByLabelText('field.new_password')

    type('field.new_password', 'Welcome-Home-1')
    type('field.confirm', 'Welcome-Home-1')
    fireEvent.click(screen.getByRole('button', { name: 'action.save' }))

    await waitFor(() => expect(where().path).toBe('/login'))
    expect(api.acceptInvitation).toHaveBeenCalledWith('abc', 'Welcome-Home-1')
    expect(where().state).toEqual({ invitation: 'accepted', identifier: 'carol' })
  })

  it('says a refused link is over, and offers a new one by mail where forgot password exists', async () => {
    open('/welcome#token=abc', { checkInvitation: vi.fn().mockRejectedValue(new InvalidLinkError()) })

    expect(await screen.findByText('welcome.invalid.title')).not.toBeNull()
    expect(await screen.findByRole('link', { name: 'welcome.invalid.forgot' })).not.toBeNull()
  })

  it('offers no new link by mail without mail', async () => {
    open('/welcome#token=abc', { checkInvitation: vi.fn().mockRejectedValue(new InvalidLinkError()) }, { mail: false })

    await screen.findByText('welcome.invalid.title')
    await waitFor(() => expect(screen.queryByRole('link', { name: 'welcome.invalid.forgot' })).toBeNull())
  })

  it('says the link is over when it stops working while the password is typed', async () => {
    open('/welcome#token=abc', { acceptInvitation: vi.fn().mockRejectedValue(new InvalidLinkError()) })
    await screen.findByLabelText('field.new_password')

    type('field.new_password', 'Welcome-Home-1')
    type('field.confirm', 'Welcome-Home-1')
    fireEvent.click(screen.getByRole('button', { name: 'action.save' }))

    expect(await screen.findByText('welcome.invalid.title')).not.toBeNull()
  })

  it('asks someone signed in to sign out first, keeping the link', async () => {
    authStore.setState({ user: jane })
    open('/welcome#token=abc', {})

    expect(await screen.findByText('reset.signed_in.title')).not.toBeNull()
  })

  it('offers to try again when the server could not be asked', async () => {
    open('/welcome#token=abc', { checkInvitation: vi.fn().mockRejectedValue(new NetworkError()) })

    expect(await screen.findByText('welcome.unavailable')).not.toBeNull()
    expect(screen.getByRole('button', { name: 'reset.retry' })).not.toBeNull()
  })
})
