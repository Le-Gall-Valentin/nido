import { useCallback, useEffect, useState } from 'react'

function secondsUntil(deadline: number) {
  return Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
}

/**
 * Seconds before another code can be asked for, counting down from what the server said. The wait is read
 * from a deadline, not counted tick by tick: a locked phone or a throttled tab runs timers late or in
 * bursts, and a count of ticks would fall behind the server's own clock.
 */
export function useResendCountdown(initialSeconds: number) {
  const [deadline, setDeadline] = useState(() => Date.now() + Math.max(0, initialSeconds) * 1000)
  const [seconds, setSeconds] = useState(() => secondsUntil(deadline))
  const running = seconds > 0

  useEffect(() => {
    if (!running) return
    const refresh = () => setSeconds(secondsUntil(deadline))
    const timer = setInterval(refresh, 1000)
    document.addEventListener('visibilitychange', refresh)
    return () => {
      clearInterval(timer)
      document.removeEventListener('visibilitychange', refresh)
    }
  }, [running, deadline])

  const restart = useCallback((next: number) => {
    const at = Date.now() + Math.max(0, next) * 1000
    setDeadline(at)
    setSeconds(secondsUntil(at))
  }, [])

  return { seconds, restart }
}
