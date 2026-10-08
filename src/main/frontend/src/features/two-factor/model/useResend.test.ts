import { act, renderHook } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { useResend } from './useResend'
import { ResendTooSoonError, SendLimitError } from './errors'
import { NetworkError } from '@/shared/lib'

describe('useResend', () => {
  it('a code that leaves restarts the wait from what the server said', async () => {
    const { result } = renderHook(() => useResend(0, () => Promise.resolve(60)))

    let outcome: unknown
    await act(async () => { outcome = await result.current.resend() })

    expect(outcome).toEqual({ kind: 'sent' })
    expect(result.current.seconds).toBe(60)
  })

  it('asked again too soon, the code already sent still works: the wait is the one left', async () => {
    const { result } = renderHook(() => useResend(0, () => Promise.reject(new ResendTooSoonError(35))))

    let outcome: unknown
    await act(async () => { outcome = await result.current.resend() })

    expect(outcome).toEqual({ kind: 'recent' })
    expect(result.current.seconds).toBe(35)
  })

  it('the account limit waits for its window and is handed back to be said', async () => {
    const limit = new SendLimitError(600)
    const { result } = renderHook(() => useResend(0, () => Promise.reject(limit)))

    let outcome: unknown
    await act(async () => { outcome = await result.current.resend() })

    expect(outcome).toEqual({ kind: 'failed', error: limit })
    expect(result.current.seconds).toBe(600)
  })

  it('any other failure is handed back, the wait left as it was', async () => {
    const offline = new NetworkError()
    const { result } = renderHook(() => useResend(0, () => Promise.reject(offline)))

    let outcome: unknown
    await act(async () => { outcome = await result.current.resend() })

    expect(outcome).toEqual({ kind: 'failed', error: offline })
    expect(result.current.seconds).toBe(0)
  })

  it('a second click while the first is on its way sends nothing', async () => {
    // A double click would mail two codes, the first already dead by the time it lands.
    let deliver: (seconds: number) => void = () => {}
    const send = vi.fn(() => new Promise<number>(resolve => { deliver = resolve }))
    const { result } = renderHook(() => useResend(0, send))

    let first: Promise<unknown> = Promise.resolve()
    let second: unknown
    await act(async () => {
      first = result.current.resend()
      second = await result.current.resend()
    })
    expect(result.current.sending).toBe(true)
    await act(async () => { deliver(60); await first })

    expect(second).toBeNull()
    expect(send).toHaveBeenCalledTimes(1)
    expect(result.current.sending).toBe(false)
  })
})
