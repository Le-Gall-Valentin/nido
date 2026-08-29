import { render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { SpacesApiProvider } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import type { SpaceSummary } from '@/entities/space'
import { createTestQueryClient } from '@/shared/test'
import { GroupAccentStrip } from './GroupAccentStrip'

const PERSONAL: SpaceSummary = {
  id: 'personal-1', type: 'PERSONAL', name: 'Alice', accent: '#8a7d6b', glyph: '👤', myRole: 'OWNER', memberCount: 1,
}
const FAMILY: SpaceSummary = {
  id: 'space-2', type: 'SHARED', name: 'La Famille', accent: '#c17a5c', glyph: '🏡', myRole: 'ADMIN', memberCount: 4,
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
            <Route path="/s/:spaceId/*" element={<GroupAccentStrip />} />
            <Route path="*" element={<GroupAccentStrip />} />
          </Routes>
        </MemoryRouter>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
}

describe('GroupAccentStrip', () => {
  it('renders nothing outside a space route', () => {
    const { container } = renderAt('/account', [PERSONAL, FAMILY])
    expect(container.firstChild).toBeNull()
  })

  it('renders nothing while viewing the personal space', async () => {
    const { container } = renderAt('/s/personal-1/members', [PERSONAL, FAMILY])
    await waitFor(() => expect(container.firstChild).toBeNull())
  })

  it('renders the strip while viewing a shared group, using its accent', async () => {
    renderAt('/s/space-2/members', [PERSONAL, FAMILY])
    const strip = await screen.findByTestId('group-accent-strip')
    expect(strip.style.background).toBe('rgb(193, 122, 92)') // #c17a5c
  })
})
