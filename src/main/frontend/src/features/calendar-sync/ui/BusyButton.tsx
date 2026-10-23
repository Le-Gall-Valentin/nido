import type { ComponentProps } from 'react'
import { Button } from '@/shared/ui'

/**
 * A button that waits while an action runs, its own or another's: inert and dimmed, but focusable still. A disabled
 * button drops the focus it holds to <body>, from where Tab leaves the dialog; this one keeps it, so that a failure
 * said next to it is met where the person already is.
 */
export function BusyButton({ busy, onClick, className = '', ...props }: ComponentProps<typeof Button> & { busy: boolean }) {
  return (
    <Button {...props} aria-disabled={busy || undefined}
      className={`aria-disabled:cursor-not-allowed aria-disabled:opacity-50 ${className}`}
      onClick={(event) => {
        if (!busy) onClick?.(event)
      }} />
  )
}
