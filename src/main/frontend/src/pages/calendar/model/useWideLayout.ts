import { useEffect, useState } from 'react'

/** Tailwind's `md`. The layout switch every calendar view makes is at this width. */
const QUERY = '(min-width: 768px)'

function isWide(): boolean {
  return typeof window !== 'undefined' && typeof window.matchMedia === 'function' && window.matchMedia(QUERY).matches
}

/**
 * Whether the desktop layout is on screen. Deliberately the width and not the pointer: a touch
 * tablet at 1024px shows the desktop grid, and its edges must behave the desktop way.
 */
export function useWideLayout(): boolean {
  const [wide, setWide] = useState(isWide)
  useEffect(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return
    const mql = window.matchMedia(QUERY)
    const onChange = () => setWide(mql.matches)
    mql.addEventListener('change', onChange)
    return () => mql.removeEventListener('change', onChange)
  }, [])
  return wide
}
