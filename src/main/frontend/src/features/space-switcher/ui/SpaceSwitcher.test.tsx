import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { SpaceSwitcher } from './SpaceSwitcher'
import { SpacesApiProvider } from '../model/spacesApiContext'
import type { ISpacesApi } from '../model/ISpacesApi'
import type { SpaceSummary } from '@/entities/space'
import { createTestQueryClient } from '@/shared/test'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>()
  return { ...actual, useNavigate: () => mockNavigate }
})

const PERSONAL: SpaceSummary = {
  id: 'personal-1', type: 'PERSONAL', name: 'Alice', accent: '#8a7d6b', glyph: '👤', myRole: 'OWNER', memberCount: 1,
}
const FAMILY: SpaceSummary = {
  id: 'space-2', type: 'SHARED', name: 'La Famille', accent: '#c17a5c', glyph: '🏡', myRole: 'ADMIN', memberCount: 4,
}

function fakeApi(spaces: SpaceSummary[] = [PERSONAL, FAMILY]): ISpacesApi {
  return {
    listMySpaces: vi.fn().mockResolvedValue(spaces),
    getSpace: vi.fn(),
  }
}

function setup({ api = fakeApi(), initialEntries = ['/s/space-2'] }: { api?: ISpacesApi; initialEntries?: string[] } = {}) {
  const queryClient = createTestQueryClient()
  render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={api}>
        <MemoryRouter initialEntries={initialEntries}>
          <Routes>
            <Route path="/s/:spaceId" element={<SpaceSwitcher />} />
            <Route path="*" element={<SpaceSwitcher />} />
          </Routes>
        </MemoryRouter>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
  return { api }
}

beforeEach(() => vi.clearAllMocks())

describe('SpaceSwitcher — trigger', () => {
  it('shows the current context name and role', async () => {
    setup()
    expect(await screen.findByText('La Famille')).toBeDefined()
    expect(screen.getByText('switcher.role.admin')).toBeDefined()
  })

  it('is closed by default', async () => {
    setup()
    await screen.findByText('La Famille')
    expect(screen.queryByText('switcher.title')).toBeNull()
  })
})

describe('SpaceSwitcher — panel', () => {
  it('lists every context with the personal space first', async () => {
    setup()
    const trigger = await screen.findByRole('button', { expanded: false })
    fireEvent.click(trigger)
    const items = screen.getAllByRole('menuitem')
    expect(items).toHaveLength(2)
    expect(items[0].textContent).toContain('Alice')
    expect(items[1].textContent).toContain('La Famille')
  })

  it('shows the create-or-join action', async () => {
    setup()
    const trigger = await screen.findByRole('button', { expanded: false })
    fireEvent.click(trigger)
    expect(screen.getByText('switcher.create_action')).toBeDefined()
  })

  it('marks the current context with a check', async () => {
    setup()
    const trigger = await screen.findByRole('button', { expanded: false })
    fireEvent.click(trigger)
    const items = screen.getAllByRole('menuitem')
    const currentItem = items.find((item) => item.textContent?.includes('La Famille'))
    expect(currentItem?.querySelector('svg')).not.toBeNull()
  })

  it('navigates to the chosen context and remembers it', async () => {
    setup()
    const trigger = await screen.findByRole('button', { expanded: false })
    fireEvent.click(trigger)
    const items = screen.getAllByRole('menuitem')
    const personalItem = items.find((item) => item.textContent?.includes('Alice'))!
    fireEvent.click(personalItem)
    expect(mockNavigate).toHaveBeenCalledWith('/s/personal-1')
  })

  it('navigates to the groups page on create-or-join', async () => {
    setup()
    const trigger = await screen.findByRole('button', { expanded: false })
    fireEvent.click(trigger)
    fireEvent.click(screen.getByText('switcher.create_action'))
    expect(mockNavigate).toHaveBeenCalledWith('/spaces')
  })

  it('closes via the backdrop', async () => {
    setup()
    const trigger = await screen.findByRole('button', { expanded: false })
    fireEvent.click(trigger)
    fireEvent.click(screen.getByLabelText('switcher.close_label'))
    expect(screen.queryByText('switcher.title')).toBeNull()
  })

  it('closes on Escape', async () => {
    setup()
    const trigger = await screen.findByRole('button', { expanded: false })
    fireEvent.click(trigger)
    fireEvent.keyDown(document, { key: 'Escape' })
    await waitFor(() => expect(screen.queryByText('switcher.title')).toBeNull())
  })
})

describe('SpaceSwitcher — outside a scoped route', () => {
  it('falls back to the personal space when the URL carries no context', async () => {
    setup({ initialEntries: ['/account'] })
    expect(await screen.findByText('Alice')).toBeDefined()
  })
})
