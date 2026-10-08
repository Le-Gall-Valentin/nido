import { fireEvent, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { AvatarStack } from './AvatarStack'
import { renderWithActions } from '../test/renderWithActions'

describe('AvatarStack', () => {
  it('shows three avatars and a "+n" for the rest, naming everyone for assistive tech', () => {
    renderWithActions(<AvatarStack memberIds={['u-me', 'u-cam', 'u-paul', 'u-lea']} />)

    expect(screen.getByRole('button', { name: 'valentin, camille, paul et lea' })).toBeDefined()
    expect(screen.getByText('+1')).toBeDefined()
  })

  it('names everyone on a tap, since a dashboard row opens nothing that would', () => {
    renderWithActions(<AvatarStack memberIds={['u-me', 'u-cam']} />)
    const stack = screen.getByRole('button', { name: 'valentin et camille' })

    fireEvent.pointerDown(stack, { pointerType: 'touch' })
    fireEvent.click(stack)

    expect(screen.getByRole('tooltip').textContent).toBe('valentin et camille')
  })

  it('names a member it cannot resolve with the fallback, never with an id', () => {
    renderWithActions(<AvatarStack memberIds={['u-gone']} />)

    expect(screen.getByRole('button', { name: 'member.former' })).toBeDefined()
    expect(screen.queryByText(/u-gone/)).toBeNull()
  })
})
