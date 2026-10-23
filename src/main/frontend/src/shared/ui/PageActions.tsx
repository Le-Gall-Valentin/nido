import type { ReactNode } from 'react'

/**
 * A page's actions beside its title. On a phone the secondary ones share a row and the primary one takes the whole
 * width under them; wider, they line up. A label never breaks: short of room, a button goes to the next line instead.
 * The buttons are a little narrower than in a dialog, so that three of them fit beside the sidebar on a tablet.
 */
export function PageActions({ children }: { children: ReactNode }) {
  return (
    <div className="flex w-full flex-wrap gap-2 whitespace-nowrap sm:w-auto *:flex-1 *:px-3 sm:*:flex-none
      [&>[data-variant=primary]]:basis-full sm:[&>[data-variant=primary]]:basis-auto">
      {children}
    </div>
  )
}
