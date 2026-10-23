import { useEffect, useRef } from 'react'

/**
 * Where focus goes after the next render, when the element holding it is about to go away — a button replaced by a
 * form, a row by its confirmation. Given as a selector inside the root; when nothing matches, the root's status region
 * takes it. Focus is never left to fall to <body>, from where Tab leaves a modal.
 */
export function usePendingFocus<T extends HTMLElement = HTMLDivElement>() {
  const rootRef = useRef<T>(null)
  const pending = useRef<string | null>(null)

  useEffect(() => {
    const selector = pending.current
    if (!selector) return
    pending.current = null
    const root = rootRef.current
    if (!root) return
    ;(root.querySelector<HTMLElement>(selector) ?? root.querySelector<HTMLElement>('[role="status"]'))?.focus()
  })

  return { rootRef, focusNext: (selector: string) => { pending.current = selector } }
}
