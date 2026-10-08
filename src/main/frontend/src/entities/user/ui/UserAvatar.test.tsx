import { render } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { avatarHue } from '../lib/avatarHue'
import { UserAvatar } from './UserAvatar'

describe('UserAvatar', () => {
  it('renders the user initials', () => {
    const { getByText } = render(<UserAvatar userId="u-1" username="john.doe" />)
    expect(getByText('JD')).toBeDefined()
  })

  it('takes its colour from the id, on a lightness the theme sets', () => {
    const { getByText } = render(<UserAvatar userId="u-1" username="alice" />)
    const background = getByText('A').style.background
    expect(background).toContain(`var(--avatar-c) ${avatarHue('u-1')}`)
    expect(background).toContain('var(--avatar-l-from)')
  })

  it('gives two people with the same initials two colours', () => {
    expect(avatarHue('u-1')).not.toBe(avatarHue('u-2'))
    const { getAllByText } = render(<>
      <UserAvatar userId="u-1" username="victor.lemoine" />
      <UserAvatar userId="u-2" username="valerie.lambert" />
    </>)
    const [first, second] = getAllByText('VL')
    expect(first.style.background).not.toBe(second.style.background)
  })

  it('keeps the colour of a person whose name changes', () => {
    const { getByText } = render(<>
      <UserAvatar userId="u-1" username="alice" />
      <UserAvatar userId="u-1" username="bob" />
    </>)
    expect(getByText('A').style.background).toBe(getByText('B').style.background)
  })

  it('is hidden from assistive technology', () => {
    const { getByText } = render(<UserAvatar userId="u-1" username="alice" />)
    expect(getByText('A').getAttribute('aria-hidden')).toBe('true')
  })

  it('applies the size className override', () => {
    const { getByText } = render(<UserAvatar userId="u-1" username="alice" className="size-10 rounded-xl text-base" />)
    expect(getByText('A').className).toContain('size-10')
  })
})
