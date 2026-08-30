import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { SpacesApiProvider } from './spacesApiContext'
import { useWritableSpaces } from './useWritableSpaces'
import type { ISpacesApi } from './ISpacesApi'
import type { SpaceSummary } from '@/entities/space'
import { createTestQueryClient } from '@/shared/test'

const PERSONAL: SpaceSummary = {
  id: 'personal-1', type: 'PERSONAL', name: 'Alice', accent: '#8a7d6b', glyph: '👤', myRole: 'OWNER', memberCount: 1,
}
const FAMILY: SpaceSummary = {
  id: 'space-2', type: 'SHARED', name: 'La Famille', accent: '#c17a5c', glyph: '🏡', myRole: 'MEMBER', memberCount: 4,
}
const READ_ONLY: SpaceSummary = {
  id: 'space-3', type: 'SHARED', name: 'Amis', accent: '#4a7fa0', glyph: '🎉', myRole: 'VIEWER', memberCount: 3,
}

function fakeApi(spaces: SpaceSummary[]): ISpacesApi {
  return { listMySpaces: vi.fn().mockResolvedValue(spaces), getSpace: vi.fn() }
}

function wrapperFor(api: ISpacesApi) {
  const queryClient = createTestQueryClient()
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={api}>{children}</SpacesApiProvider>
    </QueryClientProvider>
  )
}

describe('useWritableSpaces', () => {
  it('excludes the current space and any space where the caller can only view', async () => {
    const api = fakeApi([PERSONAL, FAMILY, READ_ONLY])
    const { result } = renderHook(() => useWritableSpaces('personal-1'), { wrapper: wrapperFor(api) })

    await waitFor(() => expect(result.current.data).toEqual([FAMILY]))
  })

  it('returns every writable space when excludeSpaceId is undefined', async () => {
    const api = fakeApi([PERSONAL, FAMILY, READ_ONLY])
    const { result } = renderHook(() => useWritableSpaces(undefined), { wrapper: wrapperFor(api) })

    await waitFor(() => expect(result.current.data).toEqual([PERSONAL, FAMILY]))
  })

  it('returns an empty list while the underlying query has not resolved', () => {
    const api = fakeApi([PERSONAL])
    const { result } = renderHook(() => useWritableSpaces('personal-1'), { wrapper: wrapperFor(api) })

    expect(result.current.data).toEqual([])
  })
})
