import { useCallback, useEffect, useRef, useState } from 'react'

export type Flash = { kind: 'success' | 'error'; key: string; values?: Record<string, unknown> }

/** A message that fades after `duration` ms; a new one replaces it and gets the whole wait. */
export function useFlash(duration: number) {
  const [flash, setFlash] = useState<Flash | null>(null)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => () => { if (timer.current) clearTimeout(timer.current) }, [])

  const clearFlash = useCallback(() => {
    if (timer.current) clearTimeout(timer.current)
    timer.current = null
    setFlash(null)
  }, [])

  const showFlash = useCallback((kind: Flash['kind'], key: string, values?: Record<string, unknown>) => {
    if (timer.current) clearTimeout(timer.current)
    setFlash(values === undefined ? { kind, key } : { kind, key, values })
    timer.current = setTimeout(() => setFlash(null), duration)
  }, [duration])

  return { flash, showFlash, clearFlash }
}
