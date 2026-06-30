import { afterEach, describe, expect, it, vi } from 'vitest'
import { clearSessionHint, hasSessionHint, setSessionHint } from './sessionHint'

class MemoryStorage implements Storage {
  private readonly map = new Map<string, string>()

  get length(): number {
    return this.map.size
  }

  clear(): void {
    this.map.clear()
  }

  getItem(key: string): string | null {
    return this.map.has(key) ? this.map.get(key)! : null
  }

  key(index: number): string | null {
    return Array.from(this.map.keys())[index] ?? null
  }

  removeItem(key: string): void {
    this.map.delete(key)
  }

  setItem(key: string, value: string): void {
    this.map.set(key, value)
  }
}

describe('sessionHint', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('persists and clears session hint when localStorage is available', () => {
    const storage = new MemoryStorage()
    vi.stubGlobal('localStorage', storage)

    expect(hasSessionHint()).toBe(false)
    setSessionHint()
    expect(hasSessionHint()).toBe(true)
    clearSessionHint()
    expect(hasSessionHint()).toBe(false)
  })

  it('fails safe when localStorage is unavailable', () => {
    vi.stubGlobal('localStorage', {
      getItem: () => {
        throw new Error('storage unavailable')
      },
      setItem: () => {
        throw new Error('storage unavailable')
      },
      removeItem: () => {
        throw new Error('storage unavailable')
      },
    } as unknown as Storage)

    expect(() => setSessionHint()).not.toThrow()
    expect(() => clearSessionHint()).not.toThrow()
    expect(hasSessionHint()).toBe(false)
  })
})
