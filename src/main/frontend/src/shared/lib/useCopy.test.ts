import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { copyText } from './copyText'
import { useCopy } from './useCopy'

vi.mock('./copyText', () => ({ copyText: vi.fn() }))

describe('useCopy', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  it('says it copied, then goes back to rest', async () => {
    vi.mocked(copyText).mockResolvedValue(true)
    const { result } = renderHook(() => useCopy())

    await act(async () => { await result.current.copy('NIDO-1') })
    expect(copyText).toHaveBeenCalledWith('NIDO-1')
    expect(result.current.copied).toBe(true)
    expect(result.current.failed).toBe(false)

    act(() => { vi.advanceTimersByTime(1500) })
    expect(result.current.copied).toBe(false)
  })

  it('says so when nothing could be copied, until a copy works', async () => {
    vi.mocked(copyText).mockResolvedValueOnce(false).mockResolvedValueOnce(true)
    const { result } = renderHook(() => useCopy())

    await act(async () => { await result.current.copy('NIDO-1') })
    expect(result.current.failed).toBe(true)
    expect(result.current.copied).toBe(false)

    await act(async () => { await result.current.copy('NIDO-1') })
    expect(result.current.failed).toBe(false)
    expect(result.current.copied).toBe(true)
  })
})
