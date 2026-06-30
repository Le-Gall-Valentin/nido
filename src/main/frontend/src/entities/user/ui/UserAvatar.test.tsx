import { render } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { UserAvatar } from './UserAvatar'

describe('UserAvatar', () => {
  it('renders the user initials', () => {
    const { getByText } = render(<UserAvatar username="john.doe" role="USER" />)
    expect(getByText('JD')).toBeDefined()
  })

  it('uses the violet gradient for SUPER_ADMIN', () => {
    const { getByText } = render(<UserAvatar username="root" role="SUPER_ADMIN" />)
    // jsdom normalizes hex colors to rgb(): #a78bfa → rgb(167, 139, 250)
    expect(getByText('R').style.background).toContain('rgb(167, 139, 250)')
  })

  it('uses the accent gradient for other roles', () => {
    const { getByText } = render(<UserAvatar username="alice" role="USER" />)
    expect(getByText('A').style.background).toContain('var(--color-accent)')
  })

  it('is hidden from assistive technology', () => {
    const { getByText } = render(<UserAvatar username="alice" role="USER" />)
    expect(getByText('A').getAttribute('aria-hidden')).toBe('true')
  })

  it('applies the size className override', () => {
    const { getByText } = render(<UserAvatar username="alice" role="USER" className="size-10 rounded-xl text-base" />)
    expect(getByText('A').className).toContain('size-10')
  })
})
