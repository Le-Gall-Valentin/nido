import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import type { EventColor } from '../lib/eventPalette'
import { ColorSwatches } from './ColorSwatches'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

function Picker({ initial, deselectable }: { initial: EventColor | null; deselectable?: boolean }) {
  const [color, setColor] = useState<EventColor | null>(initial)
  return <ColorSwatches label="Couleur" value={color} onChange={setColor} deselectable={deselectable} />
}

describe('ColorSwatches', () => {
  it('is one radio group named by its label, the colour chosen checked', () => {
    render(<Picker initial="event-cyan" />)

    const group = screen.getByRole('radiogroup', { name: 'Couleur' })
    expect(group).toBeTruthy()
    expect(screen.getByRole('radio', { name: 'colors.event-cyan' }).getAttribute('aria-checked')).toBe('true')
    expect(screen.getAllByRole('radio').filter((radio) => radio.tabIndex === 0)).toHaveLength(1)
  })

  it('moves to the next colour with the arrows, round to the first', () => {
    render(<Picker initial="event-brique" />)

    fireEvent.keyDown(screen.getByRole('radio', { name: 'colors.event-brique' }), { key: 'ArrowRight' })

    const first = screen.getByRole('radio', { name: 'colors.event-violet' })
    expect(first.getAttribute('aria-checked')).toBe('true')
    expect(document.activeElement).toBe(first)
  })

  it('takes the colour back when chosen again, only where none is a choice', () => {
    const { unmount } = render(<Picker initial="event-cyan" deselectable />)
    fireEvent.click(screen.getByRole('radio', { name: 'colors.event-cyan' }))
    expect(screen.getAllByRole('radio').every((radio) => radio.getAttribute('aria-checked') === 'false')).toBe(true)
    unmount()

    render(<Picker initial="event-cyan" />)
    fireEvent.click(screen.getByRole('radio', { name: 'colors.event-cyan' }))
    expect(screen.getByRole('radio', { name: 'colors.event-cyan' }).getAttribute('aria-checked')).toBe('true')
  })
})
