import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { Button } from './Button'
import { PageActions } from './PageActions'

describe('PageActions', () => {
  it('holds the buttons themselves, where its rule for the primary one can reach it', () => {
    // The phone layout gives the primary button a row of its own by the attribute the Button writes.
    render(<PageActions><Button>Sync</Button><Button variant="primary">New</Button></PageActions>)
    const primary = screen.getByRole('button', { name: 'New' })
    expect(primary.parentElement?.className).toContain('[&>[data-variant=primary]]:basis-full')
    expect(primary.dataset.variant).toBe('primary')
  })
})
