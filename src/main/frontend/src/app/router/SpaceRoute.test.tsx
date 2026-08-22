import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SpacesApiProvider } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import type { SpaceSummary } from '@/entities/space'
import { createTestQueryClient } from '@/shared/test'
import { SpaceRoute } from './SpaceRoute'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const PERSONAL: SpaceSummary = {
  id: 'personal-1', type: 'PERSONAL', name: 'Alice', accent: '#8a7d6b', glyph: '👤', myRole: 'OWNER', memberCount: 1,
}
const FAMILY: SpaceSummary = {
  id: 'space-2', type: 'SHARED', name: 'La Famille', accent: '#c17a5c', glyph: '🏡', myRole: 'ADMIN', memberCount: 4,
}

function fakeApi(spaces: SpaceSummary[] | Promise<SpaceSummary[]> = [PERSONAL, FAMILY]): ISpacesApi {
  return {
    listMySpaces: vi.fn().mockImplementation(() => Promise.resolve(spaces)),
    getSpace: vi.fn(),
  }
}

function LocationDisplay() {
  const location = useLocation()
  return <div data-testid="location">{location.pathname}</div>
}

function renderAt(path: string, api: ISpacesApi, queryClient: QueryClient = createTestQueryClient()) {
  return render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={api}>
        <MemoryRouter initialEntries={[path]}>
          <LocationDisplay />
          <Routes>
            <Route
              path="/s/:spaceId"
              element={<SpaceRoute><div>scoped-content</div></SpaceRoute>}
            />
            <Route path="/account" element={<div>on-account</div>} />
          </Routes>
        </MemoryRouter>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
}

describe('SpaceRoute', () => {
  it('shows a waiting state while the space list loads, without redirecting', () => {
    const api = fakeApi(new Promise(() => {}) as unknown as SpaceSummary[])
    renderAt('/s/space-2', api)
    expect(screen.queryByText('scoped-content')).toBeNull()
    expect(screen.queryByText('on-account')).toBeNull()
    expect(screen.getByRole('status')).toBeDefined()
  })

  it('renders the children when the URL context is in the list', async () => {
    renderAt('/s/space-2', fakeApi())
    expect(await screen.findByText('scoped-content')).toBeDefined()
  })

  it('redirects to the personal space when the URL context is not in the list', async () => {
    renderAt('/s/ghost-space', fakeApi())
    await screen.findByText('scoped-content')
    expect(screen.getByTestId('location').textContent).toBe('/s/personal-1')
  })

  it('holds with the loader instead of redirecting while a background refetch might still reveal the context', async () => {
    // Regression: accepting an invitation invalidates ["spaces"], but the
    // cached (stale) list is still missing the new context while the
    // refetch is in flight — isLoading is already false by then. Ejecting
    // at that point is the bug; the guard must wait for the refetch.
    const queryClient = createTestQueryClient()
    queryClient.setQueryData(['spaces'], [PERSONAL])
    let resolveFetch!: (spaces: SpaceSummary[]) => void
    const api: ISpacesApi = {
      listMySpaces: vi.fn().mockImplementation(() => new Promise((resolve) => { resolveFetch = resolve })),
      getSpace: vi.fn(),
    }

    renderAt('/s/space-2', api, queryClient)

    expect(screen.queryByText('scoped-content')).toBeNull()
    expect(screen.queryByTestId('location')?.textContent).not.toBe('/s/personal-1')
    expect(screen.getByRole('status')).toBeDefined()

    resolveFetch([PERSONAL, FAMILY])
    expect(await screen.findByText('scoped-content')).toBeDefined()
  })

  it('holds position and shows a translated error instead of redirecting when the list fails to load', async () => {
    const api: ISpacesApi = {
      listMySpaces: vi.fn().mockRejectedValue(new Error('network down')),
      getSpace: vi.fn(),
    }
    renderAt('/s/space-2', api)

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toBe('error.spaces_unavailable')
    expect(screen.queryByText('scoped-content')).toBeNull()
    expect(screen.getByTestId('location').textContent).toBe('/s/space-2')
  })
})
