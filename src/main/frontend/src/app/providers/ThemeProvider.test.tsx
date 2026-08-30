import { renderHook, act } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ThemeProvider, useTheme } from './ThemeProvider'

class MemoryStorage implements Storage {
  private readonly map = new Map<string, string>()
  get length(): number { return this.map.size }
  clear(): void { this.map.clear() }
  getItem(key: string): string | null { return this.map.has(key) ? this.map.get(key)! : null }
  key(index: number): string | null { return Array.from(this.map.keys())[index] ?? null }
  removeItem(key: string): void { this.map.delete(key) }
  setItem(key: string, value: string): void { this.map.set(key, value) }
}

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <ThemeProvider>{children}</ThemeProvider>
)

beforeEach(() => {
  vi.stubGlobal('localStorage', new MemoryStorage())
  document.documentElement.removeAttribute('data-theme')
})

afterEach(() => {
  vi.unstubAllGlobals()
  document.documentElement.removeAttribute('data-theme')
})

describe('ThemeProvider', () => {
  it('defaults to system when localStorage is empty', () => {
    const { result } = renderHook(() => useTheme(), { wrapper })
    expect(result.current.theme).toBe('system')
    expect(document.documentElement.getAttribute('data-theme')).toBeNull()
  })

  it('reads initial theme from localStorage', () => {
    localStorage.setItem('nido:theme', 'dark')
    const { result } = renderHook(() => useTheme(), { wrapper })
    expect(result.current.theme).toBe('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('setTheme dark: writes localStorage and sets data-theme attribute', () => {
    const { result } = renderHook(() => useTheme(), { wrapper })
    act(() => result.current.setTheme('dark'))
    expect(result.current.theme).toBe('dark')
    expect(localStorage.getItem('nido:theme')).toBe('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('setTheme light: writes localStorage and sets data-theme attribute', () => {
    const { result } = renderHook(() => useTheme(), { wrapper })
    act(() => result.current.setTheme('light'))
    expect(result.current.theme).toBe('light')
    expect(localStorage.getItem('nido:theme')).toBe('light')
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
  })

  it('setTheme system: removes data-theme attribute', () => {
    localStorage.setItem('nido:theme', 'dark')
    const { result } = renderHook(() => useTheme(), { wrapper })
    act(() => result.current.setTheme('system'))
    expect(result.current.theme).toBe('system')
    expect(localStorage.getItem('nido:theme')).toBe('system')
    expect(document.documentElement.getAttribute('data-theme')).toBeNull()
  })

  it('ignores unknown localStorage value and defaults to system', () => {
    localStorage.setItem('nido:theme', 'purple')
    const { result } = renderHook(() => useTheme(), { wrapper })
    expect(result.current.theme).toBe('system')
  })

  it('throws when used outside ThemeProvider', () => {
    expect(() => renderHook(() => useTheme())).toThrow(
      'useTheme must be used inside ThemeProvider'
    )
  })

  it('falls back to system when localStorage.getItem throws (e.g. private browsing)', () => {
    vi.stubGlobal('localStorage', {
      getItem: () => { throw new DOMException('blocked', 'SecurityError') },
      setItem: vi.fn(),
    })

    const { result } = renderHook(() => useTheme(), { wrapper })

    expect(result.current.theme).toBe('system')
  })

  it('keeps the in-memory theme when localStorage.setItem throws', () => {
    vi.stubGlobal('localStorage', {
      getItem: () => null,
      setItem: () => { throw new DOMException('blocked', 'SecurityError') },
    })
    const { result } = renderHook(() => useTheme(), { wrapper })

    act(() => result.current.setTheme('dark'))

    expect(result.current.theme).toBe('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })
})