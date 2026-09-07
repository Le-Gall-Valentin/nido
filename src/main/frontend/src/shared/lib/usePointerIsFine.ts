import { useEffect, useState } from 'react'

const QUERY = '(pointer: fine)'

function isFine(): boolean {
  return typeof window !== 'undefined' && typeof window.matchMedia === 'function' && window.matchMedia(QUERY).matches
}

/**
 * True for a mouse/trackpad (a "fine" pointer), false for touch-only devices.
 * Listens for changes so a hybrid device (e.g. a touchscreen laptop) updates
 * if the user switches input mid-session.
 */
export function usePointerIsFine(): boolean {
  const [fine, setFine] = useState(isFine)

  useEffect(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return
    const mql = window.matchMedia(QUERY)
    const onChange = () => setFine(mql.matches)
    mql.addEventListener('change', onChange)
    return () => mql.removeEventListener('change', onChange)
  }, [])

  return fine
}
