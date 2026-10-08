import { useCallback, useEffect, useState } from 'react'

/** Seconds before another code can be asked for, counting down from what the server said. */
export function useResendCountdown(initialSeconds: number) {
  const [seconds, setSeconds] = useState(Math.max(0, initialSeconds))
  const running = seconds > 0

  // One interval for the whole wait, not a timeout re-armed at each second: a tab that was asleep, or a
  // render that came late, still counts every second that went by.
  useEffect(() => {
    if (!running) return
    const timer = setInterval(() => setSeconds(left => Math.max(0, left - 1)), 1000)
    return () => clearInterval(timer)
  }, [running])

  const restart = useCallback((next: number) => setSeconds(Math.max(0, next)), [])

  return { seconds, restart }
}
