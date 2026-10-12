import { render, fireEvent } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { useState } from 'react'
import { Dialog } from './Dialog'

/**
 * With the real focus trap — Dialog.test.tsx replaces it — since the trap moving the focus into the dialog is
 * precisely what the focus to give back must not be confused with.
 */
describe('Dialog with its focus trap', () => {
  it('gives the focus back to the element that opened it, even when its content took the focus', () => {
    function Harness() {
      const [open, setOpen] = useState(false)
      return (
        <div>
          <button onClick={() => setOpen(true)}>Open</button>
          <Dialog open={open} onClose={() => setOpen(false)} title="Test">
            <button>Inside</button>
          </Dialog>
        </div>
      )
    }
    const { getByText } = render(<Harness />)
    const trigger = getByText('Open')
    trigger.focus()

    fireEvent.click(trigger)
    expect(document.activeElement).toBe(getByText('Inside'))
    fireEvent.keyDown(document, { key: 'Escape' })

    expect(document.activeElement).toBe(trigger)
  })
})
