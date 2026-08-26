import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import type { SpaceSummary, SpaceDetail } from '@/entities/space'
import type { ISpacesPageApi } from '../model/ISpacesPageApi'
import { SpacesPage } from './SpacesPage'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k), i18n: { language: 'en' } }),
}))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>()
  return { ...actual, useNavigate: () => mockNavigate }
})

const PERSONAL: SpaceSummary = { id: 'p-1', type: 'PERSONAL', name: 'Alice', accent: '#8a7d6b', glyph: '👤', myRole: 'OWNER', memberCount: 1 }
const SHARED: SpaceSummary = { id: 's-1', type: 'SHARED', name: 'Chez nous', accent: '#c17a5c', glyph: '🏡', myRole: 'ADMIN', memberCount: 2 }

function fakeSpacesApi(spaces: SpaceSummary[] = [PERSONAL, SHARED]): ISpacesApi {
  return { listMySpaces: vi.fn().mockResolvedValue(spaces), getSpace: vi.fn() }
}

function fakePageApi(overrides: Partial<ISpacesPageApi> = {}): ISpacesPageApi {
  return {
    getSpaceDetail: vi.fn(), listMembers: vi.fn(), listInvitations: vi.fn(),
    listReceivedInvitations: vi.fn().mockResolvedValue([]),
    createSpace: vi.fn(), updateSpace: vi.fn(), deleteSpace: vi.fn(),
    changeMemberRole: vi.fn(), removeMember: vi.fn(), transferOwnership: vi.fn(),
    leaveSpace: vi.fn(), inviteMember: vi.fn(), revokeInvitation: vi.fn(), acceptInvitation: vi.fn(),
    ...overrides,
  }
}

function renderPage(spacesApi: ISpacesApi, pageApi: ISpacesPageApi) {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={spacesApi}>
        <SpacesPage api={pageApi} />
      </SpacesApiProvider>
    </QueryClientProvider>
  )
}

beforeEach(() => vi.clearAllMocks())

describe('SpacesPage', () => {
  it('renders the page header', () => {
    renderPage(fakeSpacesApi(), fakePageApi())
    expect(screen.getByText('title')).toBeDefined()
  })

  it('lists my contexts once loaded', async () => {
    renderPage(fakeSpacesApi(), fakePageApi())
    expect(await screen.findByText('Chez nous')).toBeDefined()
    expect(screen.getByText('Alice')).toBeDefined()
  })

  it('navigates to a group members page when selected', async () => {
    renderPage(fakeSpacesApi(), fakePageApi())
    fireEvent.click(await screen.findByRole('button', { name: /Chez nous/ }))
    expect(mockNavigate).toHaveBeenCalledWith('/s/s-1/members')
  })

  it('creates a group and navigates to its members page', async () => {
    const created: SpaceDetail = { id: 's-2', type: 'SHARED', name: 'New group', description: null, accent: '#4a7fa0', glyph: '🌿', myRole: 'OWNER', memberCount: 1 }
    const pageApi = fakePageApi({ createSpace: vi.fn().mockResolvedValue(created) })
    renderPage(fakeSpacesApi(), pageApi)

    fireEvent.click(await screen.findByText('list.action_create'))
    const dialogs = screen.getAllByText('create.title')
    expect(dialogs.length).toBeGreaterThan(0)

    fireEvent.change(screen.getByLabelText('create.name'), { target: { value: 'New group' } })
    fireEvent.click(screen.getByText('create.submit'))

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/s/s-2/members'))
  })
})
