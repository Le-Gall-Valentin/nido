import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import type { ReactNode } from 'react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider } from './spacesApiContext'
import { activeSpaceStore } from './activeSpaceStore'
import { useCurrentSpaceId } from './useCurrentSpaceId'
import type { ISpacesApi } from './ISpacesApi'
import type { SpaceSummary } from '@/entities/space'

// Node's own global `localStorage` shadows jsdom's real Storage in this test
// environment (see activeSpaceStore.test.ts for the same workaround).
class MemoryStorage implements Storage {
  private readonly map = new Map<string, string>()
  get length(): number { return this.map.size }
  clear(): void { this.map.clear() }
  getItem(key: string): string | null { return this.map.has(key) ? this.map.get(key)! : null }
  key(index: number): string | null { return Array.from(this.map.keys())[index] ?? null }
  removeItem(key: string): void { this.map.delete(key) }
  setItem(key: string, value: string): void { this.map.set(key, value) }
}

const PERSONAL: SpaceSummary = {
  id: 'personal-1', type: 'PERSONAL', name: 'Alice', accent: '#8a7d6b', glyph: '👤', myRole: 'OWNER', memberCount: 1,
}
const FAMILY: SpaceSummary = {
  id: 'space-2', type: 'SHARED', name: 'La Famille', accent: '#c17a5c', glyph: '🏡', myRole: 'ADMIN', memberCount: 4,
}

function fakeApi(spaces: SpaceSummary[] | Promise<never> = [PERSONAL, FAMILY]): ISpacesApi {
  return {
    listMySpaces: vi.fn().mockImplementation(() => (spaces instanceof Promise ? spaces : Promise.resolve(spaces))),
    getSpace: vi.fn(),
  }
}

function wrapperFor(api: ISpacesApi, path = '/') {
  const queryClient = createTestQueryClient()
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={api}>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path="/s/:spaceId/*" element={children} />
            <Route path="*" element={children} />
          </Routes>
        </MemoryRouter>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
}

beforeEach(() => {
  vi.stubGlobal('localStorage', new MemoryStorage())
  activeSpaceStore.setState({ lastSpaceId: null })
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('useCurrentSpaceId', () => {
  it('is loading while the space list has not resolved yet', () => {
    const { result } = renderHook(() => useCurrentSpaceId(), { wrapper: wrapperFor(fakeApi(new Promise(() => {}))) })

    expect(result.current).toEqual({ spaceId: undefined, isLoading: true })
  })

  it("prefers the URL's own spaceId, even if a different space is remembered", async () => {
    activeSpaceStore.getState().remember('space-2')
    const { result } = renderHook(() => useCurrentSpaceId(), { wrapper: wrapperFor(fakeApi(), '/s/personal-1/members') })

    await waitFor(() => expect(result.current.isLoading).toBe(false))
    expect(result.current.spaceId).toBe('personal-1')
  })

  it('falls back to the remembered space when the URL carries none', async () => {
    activeSpaceStore.getState().remember('space-2')
    const { result } = renderHook(() => useCurrentSpaceId(), { wrapper: wrapperFor(fakeApi()) })

    await waitFor(() => expect(result.current.spaceId).toBe('space-2'))
  })

  it('falls back to the personal space when nothing is remembered', async () => {
    const { result } = renderHook(() => useCurrentSpaceId(), { wrapper: wrapperFor(fakeApi()) })

    await waitFor(() => expect(result.current.spaceId).toBe('personal-1'))
  })

  it('falls back to the personal space and forgets a stale remembered id', async () => {
    activeSpaceStore.getState().remember('space-9-gone')
    const { result } = renderHook(() => useCurrentSpaceId(), { wrapper: wrapperFor(fakeApi([PERSONAL])) })

    await waitFor(() => expect(result.current.spaceId).toBe('personal-1'))
    expect(activeSpaceStore.getState().lastSpaceId).toBeNull()
  })

  it('does not fetch the space list when disabled', () => {
    const api = fakeApi()
    renderHook(() => useCurrentSpaceId({ enabled: false }), { wrapper: wrapperFor(api) })

    expect(api.listMySpaces).not.toHaveBeenCalled()
  })
})
