import { renderHook, act } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LanguageProvider, useLanguage } from './LanguageProvider'

const mockChangeLanguage = vi.fn().mockResolvedValue(undefined)
const listeners: Record<string, ((lng: string) => void)[]> = {}

vi.mock('../i18n', () => ({
  default: {
    language: 'fr',
    changeLanguage: (lng: string) => {
      mockChangeLanguage(lng)
      listeners['languageChanged']?.forEach(fn => fn(lng))
      return Promise.resolve()
    },
    on: (event: string, fn: (lng: string) => void) => {
      listeners[event] = [...(listeners[event] ?? []), fn]
    },
    off: (event: string, fn: (lng: string) => void) => {
      listeners[event] = (listeners[event] ?? []).filter(f => f !== fn)
    },
  },
}))

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <LanguageProvider>{children}</LanguageProvider>
)

beforeEach(() => {
  mockChangeLanguage.mockClear()
  Object.keys(listeners).forEach(k => { listeners[k] = [] })
})

describe('LanguageProvider', () => {
  it('initial language comes from i18n', () => {
    const { result } = renderHook(() => useLanguage(), { wrapper })
    expect(result.current.language).toBe('fr')
  })

  it('setLanguage calls i18n.changeLanguage', () => {
    const { result } = renderHook(() => useLanguage(), { wrapper })
    act(() => result.current.setLanguage('en'))
    expect(mockChangeLanguage).toHaveBeenCalledWith('en')
  })

  it('state updates when i18n fires languageChanged', () => {
    const { result } = renderHook(() => useLanguage(), { wrapper })
    act(() => result.current.setLanguage('en'))
    expect(result.current.language).toBe('en')
  })

  it('throws when used outside LanguageProvider', () => {
    expect(() => renderHook(() => useLanguage())).toThrow(
      'useLanguage must be used inside LanguageProvider'
    )
  })

  it('unsubscribes from languageChanged on unmount', () => {
    const { unmount } = renderHook(() => useLanguage(), { wrapper })
    const listenerCount = listeners['languageChanged']?.length ?? 0
    unmount()
    expect(listeners['languageChanged']?.length ?? 0).toBe(listenerCount - 1)
  })
})