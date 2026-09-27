import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom'
import { SpaceIndexRedirect } from './SpaceIndexRedirect'

function Location() {
  return <p data-testid="location">{useLocation().pathname}</p>
}

describe('SpaceIndexRedirect', () => {
  it('opens a space on its dashboard', () => {
    render(
      <MemoryRouter initialEntries={['/s/space-1']}>
        <Routes>
          <Route path="/s/:spaceId">
            <Route index element={<SpaceIndexRedirect />} />
            <Route path="dashboard" element={<Location />} />
          </Route>
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByTestId('location').textContent).toBe('/s/space-1/dashboard')
  })
})
