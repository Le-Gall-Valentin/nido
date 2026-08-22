import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { activeSpaceStore, LAST_SPACE_STORAGE_KEY } from './activeSpaceStore'

// Node's own global `localStorage` shadows jsdom's real Storage in this test
// environment (see src/shared/lib/sessionHint.test.ts and
// src/app/providers/ThemeProvider.test.tsx for the same workaround), so it is
// stubbed with a working in-memory implementation for the duration of the test.
class MemoryStorage implements Storage {
  private readonly map = new Map<string, string>()
  get length(): number { return this.map.size }
  clear(): void { this.map.clear() }
  getItem(key: string): string | null { return this.map.has(key) ? this.map.get(key)! : null }
  key(index: number): string | null { return Array.from(this.map.keys())[index] ?? null }
  removeItem(key: string): void { this.map.delete(key) }
  setItem(key: string, value: string): void { this.map.set(key, value) }
}

describe('activeSpaceStore', () => {
  beforeEach(() => {
    vi.stubGlobal('localStorage', new MemoryStorage())
    activeSpaceStore.setState({ lastSpaceId: null })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('starts with no remembered space', () => {
    expect(activeSpaceStore.getState().lastSpaceId).toBeNull()
  })

  it('remembers a space and persists it', () => {
    activeSpaceStore.getState().remember('space-1')

    expect(activeSpaceStore.getState().lastSpaceId).toBe('space-1')
    expect(localStorage.getItem(LAST_SPACE_STORAGE_KEY)).toBe('space-1')
  })

  it('forgets the remembered space', () => {
    activeSpaceStore.getState().remember('space-1')
    activeSpaceStore.getState().forget()

    expect(activeSpaceStore.getState().lastSpaceId).toBeNull()
    expect(localStorage.getItem(LAST_SPACE_STORAGE_KEY)).toBeNull()
  })

  it('survives a storage that throws', () => {
    // Private browsing, storage blocked: switching context must keep working.
    localStorage.setItem = () => { throw new Error('denied') }
    expect(() => activeSpaceStore.getState().remember('space-2')).not.toThrow()
    expect(activeSpaceStore.getState().lastSpaceId).toBe('space-2')
  })
})
