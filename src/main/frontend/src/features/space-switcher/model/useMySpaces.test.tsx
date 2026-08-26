import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { SpacesApiProvider } from './spacesApiContext'
import { useMySpaces, SPACES_QUERY_KEY } from './useMySpaces'
import type { ISpacesApi } from './ISpacesApi'
import type { SpaceSummary } from '@/entities/space'
import { createTestQueryClient } from '@/shared/test'
import { QueryClientProvider } from '@tanstack/react-query'

const PERSONAL: SpaceSummary = {
  id: 'personal-1', type: 'PERSONAL', name: 'Alice', accent: '#8a7d6b', glyph: '👤', myRole: 'OWNER', memberCount: 1,
}

function fakeApi(overrides: Partial<ISpacesApi> = {}): ISpacesApi {
  return {
    listMySpaces: vi.fn().mockResolvedValue([PERSONAL]),
    getSpace: vi.fn(),
    ...overrides,
  }
}

function wrapperFor(api: ISpacesApi) {
  const queryClient = createTestQueryClient()
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={api}>{children}</SpacesApiProvider>
    </QueryClientProvider>
  )
}

describe('useMySpaces', () => {
  it('exposes the query key as the flat, unscoped "spaces" string', () => {
    expect(SPACES_QUERY_KEY).toBe('spaces')
  })

  it('fetches the caller spaces through the injected api', async () => {
    const api = fakeApi()
    const { result } = renderHook(() => useMySpaces(), { wrapper: wrapperFor(api) })

    await waitFor(() => expect(result.current.data).toEqual([PERSONAL]))
    expect(api.listMySpaces).toHaveBeenCalledOnce()
  })

  it('surfaces a rejection from the api', async () => {
    const api = fakeApi({ listMySpaces: vi.fn().mockRejectedValue(new Error('boom')) })
    const { result } = renderHook(() => useMySpaces(), { wrapper: wrapperFor(api) })

    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})
