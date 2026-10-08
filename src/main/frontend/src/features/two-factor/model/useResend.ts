import { useRef, useState } from 'react'
import { useResendCountdown } from './useResendCountdown'
import { ResendTooSoonError, SendLimitError } from './errors'

/** What came of "Renvoyer": sent; a code sent moments ago still works; or a failure for the screen to word. */
export type ResendOutcome =
  | { kind: 'sent' }
  | { kind: 'recent' }
  | { kind: 'failed'; error: unknown }

/**
 * "Renvoyer le code" on any code screen: one request at a time, and the wait before the next from what the server
 * said — the time left of a code sent moments ago, or of the account's window once its limit is reached.
 *
 * @param send asks for a code; resolves with the seconds before another
 * @returns `resend` resolves with null when a request was already on its way: nothing was sent
 */
export function useResend(initialSeconds: number, send: () => Promise<number>) {
  const countdown = useResendCountdown(initialSeconds)
  const [sending, setSending] = useState(false)
  const sendingRef = useRef(false)

  async function resend(): Promise<ResendOutcome | null> {
    if (sendingRef.current) return null
    sendingRef.current = true
    setSending(true)
    try {
      countdown.restart(await send())
      return { kind: 'sent' }
    } catch (error) {
      if (error instanceof ResendTooSoonError) {
        countdown.restart(error.seconds)
        return { kind: 'recent' }
      }
      if (error instanceof SendLimitError) countdown.restart(error.seconds)
      return { kind: 'failed', error }
    } finally {
      sendingRef.current = false
      setSending(false)
    }
  }

  return { seconds: countdown.seconds, sending, resend, restart: countdown.restart }
}
