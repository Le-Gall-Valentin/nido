import { screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { AvatarStack } from './AvatarStack'
import { renderWithActions } from './cardTestHarness'

describe('AvatarStack', () => {
  it('shows three avatars and a "+n" for the rest, naming everyone for assistive tech', () => {
    renderWithActions(<AvatarStack memberIds={['u-me', 'u-cam', 'u-paul', 'u-lea']} />)

    expect(screen.getByRole('img', { name: 'valentin, camille, paul, lea' })).toBeDefined()
    expect(screen.getByText('+1')).toBeDefined()
  })

  it('names a member it cannot resolve with the fallback, never with an id', () => {
    renderWithActions(<AvatarStack memberIds={['u-gone']} />)

    expect(screen.getByRole('img', { name: 'member_unknown' })).toBeDefined()
    expect(screen.queryByText(/u-gone/)).toBeNull()
  })
})
