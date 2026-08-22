import { render } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { SpaceRolePill } from './SpaceRolePill'

describe('SpaceRolePill', () => {
  it('renders the raw role by default', () => {
    const { getByText } = render(<SpaceRolePill role="MEMBER" />)
    expect(getByText('MEMBER')).toBeDefined()
  })

  it('renders the provided label instead of the raw role', () => {
    const { getByText, queryByText } = render(<SpaceRolePill role="OWNER" label="Propriétaire" />)
    expect(getByText('Propriétaire')).toBeDefined()
    expect(queryByText('OWNER')).toBeNull()
  })

  it('applies a distinct class to each of the four roles', () => {
    // The four roles must stay tellable apart: two roles sharing a class would
    // make a member indistinguishable from a viewer.
    const classNames = (['OWNER', 'ADMIN', 'MEMBER', 'VIEWER'] as const).map(
      role => render(<SpaceRolePill role={role} />).getByText(role).className
    )

    expect(new Set(classNames).size).toBe(4)
  })
})
