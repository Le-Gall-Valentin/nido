import { describe, it, expect, vi, afterEach } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useWideLayout } from './useWideLayout'

function mockWidth(wide: boolean) {
  vi.stubGlobal('matchMedia', vi.fn().mockReturnValue({ matches: wide, addEventListener: vi.fn(), removeEventListener: vi.fn() }))
}

describe('useWideLayout', () => {
  afterEach(() => { vi.unstubAllGlobals() })
  it('follows the md breakpoint, not the kind of pointer', () => {
    mockWidth(true)
    expect(renderHook(() => useWideLayout()).result.current).toBe(true)
    mockWidth(false)
    expect(renderHook(() => useWideLayout()).result.current).toBe(false)
  })
})
