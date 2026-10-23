import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { usePendingFocus } from './usePendingFocus'

function Swap({ withStatus = false }: { withStatus?: boolean }) {
  const { rootRef, focusNext } = usePendingFocus()
  const [open, setOpen] = useState(false)
  return (
    <div ref={rootRef}>
      {open
        ? <input aria-label="Nom" data-name />
        : <button type="button" onClick={() => { focusNext(withStatus ? '[data-missing]' : '[data-name]'); setOpen(true) }}>Ouvrir</button>}
      {withStatus && <p role="status" tabIndex={-1}>état</p>}
    </div>
  )
}

describe('usePendingFocus', () => {
  it('hands focus to what replaced the element that held it', () => {
    render(<Swap />)
    const button = screen.getByRole('button', { name: 'Ouvrir' })
    button.focus()
    fireEvent.click(button)

    expect(document.activeElement).toBe(screen.getByRole('textbox', { name: 'Nom' }))
  })

  it('hands it to the status region when nothing matches', () => {
    render(<Swap withStatus />)
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir' }))

    expect(document.activeElement).toBe(screen.getByRole('status'))
  })
})
