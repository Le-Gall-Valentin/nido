import { fireEvent, render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { UserAvatarStack } from './UserAvatarStack'

const PEOPLE = [
  { userId: 'u-1', name: 'valentin' },
  { userId: 'u-2', name: 'camille' },
  { userId: 'u-3', name: 'paul' },
  { userId: 'u-4', name: 'lea' },
]

describe('UserAvatarStack', () => {
  it('shows three avatars, then how many more', () => {
    render(<UserAvatarStack people={PEOPLE} />)
    expect(screen.getByText('V')).toBeDefined()
    expect(screen.getByText('P')).toBeDefined()
    expect(screen.queryByText('L')).toBeNull()
    expect(screen.getByText('+1')).toBeDefined()
  })

  it('names everyone, the ones behind "+1" included, as a list in the language of the page', () => {
    render(<UserAvatarStack people={PEOPLE} standalone />)
    const trigger = screen.getByRole('button', { name: 'valentin, camille, paul et lea' })
    fireEvent.pointerDown(trigger, { pointerType: 'touch' })
    fireEvent.click(trigger)
    expect(screen.getByRole('tooltip').textContent).toBe('valentin, camille, paul et lea')
  })

  it('inside a row, is no button of its own but still gives the names', () => {
    render(<button type="button">Groceries <UserAvatarStack people={PEOPLE.slice(0, 1)} /></button>)
    expect(screen.getAllByRole('button')).toHaveLength(1)
    expect(screen.getByRole('button', { name: /valentin/ })).toBeDefined()
  })

  it('shows more when asked to', () => {
    render(<UserAvatarStack people={PEOPLE} max={4} />)
    expect(screen.getByText('L')).toBeDefined()
    expect(screen.queryByText(/^\+/)).toBeNull()
  })
})
