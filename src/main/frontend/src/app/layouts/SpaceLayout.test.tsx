import { render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { SpacesApiProvider, activeSpaceStore } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import type { SpaceSummary } from '@/entities/space'
import { PERSONAL_ACCENT } from '@/entities/space'
import { createTestQueryClient } from '@/shared/test'
import { SpaceLayout } from './SpaceLayout'

const FAMILY: SpaceSummary = {
  id: 'space-2', type: 'SHARED', name: 'La Famille', accent: '#c17a5c', glyph: '🏡', myRole: 'ADMIN', memberCount: 4,
}
const ROGUE_ACCENT: SpaceSummary = {
  id: 'space-3', type: 'SHARED', name: 'Rogue', accent: 'javascript:alert(1)', glyph: '🏡', myRole: 'ADMIN', memberCount: 1,
}

function fakeApi(spaces: SpaceSummary[]): ISpacesApi {
  return {
    listMySpaces: vi.fn().mockResolvedValue(spaces),
    getSpace: vi.fn(),
  }
}

function renderAt(path: string, spaces: SpaceSummary[]) {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={fakeApi(spaces)}>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path="/s/:spaceId" element={<SpaceLayout />}>
              <Route index element={<div>child-content</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
}

beforeEach(() => {
  activeSpaceStore.setState({ lastSpaceId: null })
})

describe('SpaceLayout', () => {
  it('renders the outlet content', async () => {
    renderAt('/s/space-2', [FAMILY])
    expect(await screen.findByText('child-content')).toBeDefined()
  })

  it('remembers the URL context in the store', async () => {
    renderAt('/s/space-2', [FAMILY])
    await screen.findByText('child-content')
    expect(activeSpaceStore.getState().lastSpaceId).toBe('space-2')
  })

  it('sets --space-accent from the validated palette', async () => {
    renderAt('/s/space-2', [FAMILY])
    const root = screen.getByTestId('space-layout')
    await waitFor(() => expect(root.style.getPropertyValue('--space-accent')).toBe('#c17a5c'))
  })

  it('falls back to the personal accent when the API accent is outside the palette', async () => {
    renderAt('/s/space-3', [ROGUE_ACCENT])
    const root = screen.getByTestId('space-layout')
    await waitFor(() => expect(root.style.getPropertyValue('--space-accent')).toBe(PERSONAL_ACCENT))
  })
})
