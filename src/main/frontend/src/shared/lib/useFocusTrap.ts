import { useEffect } from 'react'

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(', ')

function getFocusable(container: HTMLElement): HTMLElement[] {
  return Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR))
}

/**
 * Traps keyboard focus inside `containerRef` when `enabled` is true.
 * Tab cycles forward, Shift+Tab cycles backward within the container.
 * When enabled becomes true, focuses the first focusable child automatically.
 */
export function useFocusTrap(
  containerRef: { current: HTMLElement | null },
  enabled: boolean
): void {
  useEffect(() => {
    if (!enabled || !containerRef.current) return
    const container = containerRef.current

    const focusable = getFocusable(container)
    if (focusable.length > 0 && !container.contains(document.activeElement)) {
      focusable[0].focus()
    }

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return
      const focusable = getFocusable(container)
      if (focusable.length === 0) return
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (e.shiftKey) {
        if (document.activeElement === first) { e.preventDefault(); last.focus() }
      } else {
        if (document.activeElement === last) { e.preventDefault(); first.focus() }
      }
    }

    container.addEventListener('keydown', handleKeyDown)
    return () => container.removeEventListener('keydown', handleKeyDown)
    // containerRef is a useRef object — stable identity, intentionally excluded
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled])
}