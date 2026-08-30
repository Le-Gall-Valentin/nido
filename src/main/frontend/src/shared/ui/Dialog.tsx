import { useEffect, useId, useRef, type ReactNode } from 'react'
import { useFocusTrap } from '@/shared/lib'

interface DialogProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  maxWidth?: string
}

export function Dialog({ open, onClose, title, children, maxWidth = 'max-w-md' }: DialogProps) {
  const panelRef = useRef<HTMLDivElement>(null)
  const titleId = useId()
  useFocusTrap(panelRef, open)

  useEffect(() => {
    if (!open) return
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [open, onClose])

  useEffect(() => {
    if (!open) return
    const previouslyFocused = document.activeElement as HTMLElement | null
    return () => previouslyFocused?.focus()
  }, [open])

  if (!open) return null

  return (
    <div role="dialog" aria-modal="true" aria-labelledby={titleId} className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        aria-hidden="true"
        data-testid="backdrop"
        className="fixed inset-0 bg-[rgba(44,42,38,0.32)]"
        onClick={onClose}
      />
      <div ref={panelRef} className={`relative z-10 flex max-h-[90vh] w-full ${maxWidth} flex-col overflow-y-auto rounded-[18px] bg-bg-1 p-[26px] pt-6 shadow-[0_20px_60px_rgba(44,42,38,0.24)]`}>
        <h2 id={titleId} className="sr-only">{title}</h2>
        {children}
      </div>
    </div>
  )
}