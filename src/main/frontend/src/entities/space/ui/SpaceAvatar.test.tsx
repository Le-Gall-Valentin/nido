import { render } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { SpaceAvatar } from './SpaceAvatar'

describe('SpaceAvatar', () => {
  it('renders the glyph on the allowed accent', () => {
    const { getByText } = render(<SpaceAvatar space={{ accent: '#c17a5c', glyph: '🏡' }} size="md" />)
    const avatar = getByText('🏡')
    expect(avatar).toBeDefined()
    expect(avatar.style.background).toContain('rgb(193, 122, 92)') // #c17a5c
  })

  it('falls back to the personal accent when the received accent is outside the palette', () => {
    const { getByText } = render(<SpaceAvatar space={{ accent: '#123456', glyph: '🏡' }} size="md" />)
    const avatar = getByText('🏡')
    expect(avatar.style.background).not.toContain('rgb(18, 52, 86)') // #123456
    expect(avatar.style.background).toContain('rgb(138, 125, 107)') // #8a7d6b (PERSONAL_ACCENT)
  })

  it('is hidden from assistive technology', () => {
    const { getByText } = render(<SpaceAvatar space={{ accent: '#c17a5c', glyph: '🏡' }} size="md" />)
    expect(getByText('🏡').getAttribute('aria-hidden')).toBe('true')
  })
})
