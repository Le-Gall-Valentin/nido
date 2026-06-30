import { render } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { RolePill } from './RolePill'

describe('RolePill', () => {
  it('renders the raw role by default', () => {
    const { getByText } = render(<RolePill role="ADMIN" />)
    expect(getByText('ADMIN')).toBeDefined()
  })

  it('renders the provided label instead of the raw role', () => {
    const { getByText, queryByText } = render(<RolePill role="SUPER_ADMIN" label="Super administrateur" />)
    expect(getByText('Super administrateur')).toBeDefined()
    expect(queryByText('SUPER_ADMIN')).toBeNull()
  })

  it('applies a distinct class per role', () => {
    const { getByText: superText } = render(<RolePill role="SUPER_ADMIN" />)
    const { getByText: userText } = render(<RolePill role="USER" />)
    expect(superText('SUPER_ADMIN').className).not.toBe(userText('USER').className)
  })
})
