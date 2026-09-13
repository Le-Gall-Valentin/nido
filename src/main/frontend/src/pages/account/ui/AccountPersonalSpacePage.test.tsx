import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpaceApiProvider, type ISpaceApi, type SpaceSummary } from '@/entities/space'
import { SpacesApiProvider, type ISpacesApi } from '@/features/space-switcher'
import type { User } from '@/entities/user'
import { AccountPersonalSpacePage } from './AccountPersonalSpacePage'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const mockUseAuth = vi.fn()
vi.mock('@/features/auth', () => ({
  useAuth: (...args: unknown[]) => mockUseAuth(...args),
}))

const BASE_USER: User = {
  id: 'u-1', username: 'alice', email: 'alice@test.com',
  role: 'USER', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}

const PERSONAL: SpaceSummary = {
  id: 'personal-1', type: 'PERSONAL', name: 'Alice', accent: '#c17a5c', glyph: '🏡',
  myRole: 'OWNER', memberCount: 1, timezone: 'Europe/Paris',
}

/** Only the two methods this page can reach — the rest of the port is nothing to do with it. */
function fakeSpaceApi(overrides: Partial<ISpaceApi> = {}): ISpaceApi {
  return { updateSpace: vi.fn().mockResolvedValue(undefined), ...overrides } as unknown as ISpaceApi
}

function fakeSpacesApi(): ISpacesApi {
  return { listMySpaces: vi.fn().mockResolvedValue([PERSONAL]), getSpace: vi.fn() }
}

function setup(spaceApi: ISpaceApi = fakeSpaceApi()) {
  render(
    <QueryClientProvider client={createTestQueryClient()}>
      <SpacesApiProvider api={fakeSpacesApi()}>
        <SpaceApiProvider api={spaceApi}>
          <AccountPersonalSpacePage />
        </SpaceApiProvider>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
  return spaceApi
}

beforeEach(() => {
  mockUseAuth.mockReset()
  mockUseAuth.mockImplementation((selector: (s: { user: User | null }) => unknown) => selector({ user: BASE_USER }))
  localStorage.clear()
})

describe('AccountPersonalSpacePage', () => {
  it('renders nothing at all without a signed-in user', () => {
    mockUseAuth.mockImplementation((selector: (s: { user: User | null }) => unknown) => selector({ user: null }))

    const { container } = render(
      <QueryClientProvider client={createTestQueryClient()}>
        <SpacesApiProvider api={fakeSpacesApi()}>
          <SpaceApiProvider api={fakeSpaceApi()}>
            <AccountPersonalSpacePage />
          </SpaceApiProvider>
        </SpacesApiProvider>
      </QueryClientProvider>
    )

    expect(container.firstChild).toBeNull()
  })

  it('saves a new timezone through the space port', async () => {
    // The page's own wiring, which no test covered: the section and the suggestion were tested with a
    // stub onSave, so nothing checked what onSave actually did.
    const api = setup()
    await screen.findByText('pages.personal_space.title')

    fireEvent.change(await screen.findByRole('combobox'), { target: { value: 'America/Toronto' } })
    fireEvent.click(screen.getByText('personal_space.submit'))

    await waitFor(() => expect(api.updateSpace).toHaveBeenCalledWith('personal-1', { timezone: 'America/Toronto' }))
  })

  it('shows the space again with the timezone it was given', async () => {
    // What the user checks after saving. The list has to be re-read, or the form keeps showing the
    // old zone and the suggestion banner keeps offering the change that was just made.
    const listMySpaces = vi.fn()
      .mockResolvedValueOnce([PERSONAL])
      .mockResolvedValue([{ ...PERSONAL, timezone: 'America/Toronto' }])
    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <SpacesApiProvider api={{ listMySpaces, getSpace: vi.fn() }}>
          <SpaceApiProvider api={fakeSpaceApi()}>
            <AccountPersonalSpacePage />
          </SpaceApiProvider>
        </SpacesApiProvider>
      </QueryClientProvider>
    )
    await screen.findByText('pages.personal_space.title')

    fireEvent.change(await screen.findByRole('combobox'), { target: { value: 'America/Toronto' } })
    fireEvent.click(screen.getByText('personal_space.submit'))

    await waitFor(() => expect(listMySpaces).toHaveBeenCalledTimes(2))
  })
})
