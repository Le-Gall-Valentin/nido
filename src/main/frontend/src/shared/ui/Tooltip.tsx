import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

const GAP = 6
const MARGIN = 8

/**
 * Above the anchor, or under it when there is no room; never past the window's edges. Drawn hidden
 * first, then moved once its own size is known.
 */
function Bubble({ anchor, label }: { anchor: HTMLElement; label: string }) {
  const ref = useRef<HTMLDivElement>(null)

  useLayoutEffect(() => {
    const bubble = ref.current
    if (!bubble) return
    const a = anchor.getBoundingClientRect()
    const b = bubble.getBoundingClientRect()
    const above = a.top - GAP - b.height
    const centred = a.left + a.width / 2 - b.width / 2
    bubble.style.top = `${above >= MARGIN ? above : a.bottom + GAP}px`
    bubble.style.left = `${Math.min(Math.max(centred, MARGIN), window.innerWidth - MARGIN - b.width)}px`
    bubble.style.visibility = 'visible'
  }, [anchor, label])

  return (
    <div ref={ref} role="tooltip" style={{ top: 0, left: 0, visibility: 'hidden' }}
      className="pointer-events-none fixed z-[200] max-w-[min(16rem,calc(100vw-16px))] break-words rounded-[8px] border border-border bg-bg-1 px-2.5 py-1.5 text-xs font-medium text-fg-0 shadow-[0_4px_16px_rgba(44,42,38,0.16)]">
      {label}
    </div>
  )
}

interface TooltipProps {
  label: string
  children: ReactNode
  /**
   * Alone on screen, the trigger becomes a button that a hover, a keyboard focus or a tap opens.
   * Inside something already clickable (a row that opens a detail), only a mouse opens it: a tap keeps
   * doing what the row does, and the label is read out as part of the row.
   */
  standalone?: boolean
  className?: string
}

/**
 * A short label for what a glyph alone cannot say, such as whose initials these are. Drawn at the root
 * of the page, so a dialog that scrolls cannot cut it.
 */
export function Tooltip({ label, children, standalone = false, className = '' }: TooltipProps) {
  const [open, setOpen] = useState(false)
  const triggerRef = useRef<HTMLElement | null>(null)
  // What pressed the trigger, until its click: the focus a press brings is not a keyboard's.
  const pressedWith = useRef<string | null>(null)

  useEffect(() => {
    if (!open) return
    const close = () => setOpen(false)
    const closeOutside = (event: Event) => {
      if (!triggerRef.current?.contains(event.target as Node)) close()
    }
    document.addEventListener('pointerdown', closeOutside, true)
    window.addEventListener('scroll', close, true)
    window.addEventListener('resize', close)
    return () => {
      document.removeEventListener('pointerdown', closeOutside, true)
      window.removeEventListener('scroll', close, true)
      window.removeEventListener('resize', close)
    }
  }, [open])

  // Phones make up a hover around every tap: only a real mouse hovers.
  const hover = {
    onPointerEnter: (event: React.PointerEvent) => { if (event.pointerType === 'mouse') setOpen(true) },
    onPointerLeave: (event: React.PointerEvent) => { if (event.pointerType === 'mouse') setOpen(false) },
  }
  const bubble = open && triggerRef.current ? createPortal(<Bubble anchor={triggerRef.current} label={label} />, document.body) : null

  if (!standalone) {
    return (
      <span ref={triggerRef} className={className} {...hover}>
        {children}
        <span className="sr-only">{label}</span>
        {bubble}
      </span>
    )
  }

  return (
    <button ref={(element) => { triggerRef.current = element }} type="button" aria-label={label} className={className} {...hover}
      onPointerDown={(event) => { pressedWith.current = event.pointerType }}
      onFocus={() => { if (pressedWith.current === null) setOpen(true) }}
      onBlur={() => { pressedWith.current = null; setOpen(false) }}
      onClick={() => {
        // A mouse opened it by hovering and a keyboard by focusing: only a finger or a pen toggles it.
        if (pressedWith.current === 'touch' || pressedWith.current === 'pen') setOpen((wasOpen) => !wasOpen)
        pressedWith.current = null
      }}
      onKeyDown={(event) => {
        if (event.key !== 'Escape' || !open) return
        // Stops here, so a dialog listening on the document stays open.
        event.stopPropagation()
        setOpen(false)
      }}>
      {children}
      {bubble}
    </button>
  )
}
