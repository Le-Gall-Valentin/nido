import { render, screen, waitFor } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import type { ICapabilitiesApi } from '@/entities/capabilities'
import { createTestQueryClient } from '@/shared/test'
import { RequirePasswordReset } from './RequirePasswordReset'

function openForgotPage(capabilities: ICapabilitiesApi['capabilities']) {
  const api: ICapabilitiesApi = { capabilities }
  render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter initialEntries={['/forgot-password']}>
        <Routes>
          <Route path="/forgot-password" element={<RequirePasswordReset api={api}><p>forgot page</p></RequirePasswordReset>} />
          <Route path="/login" element={<p>login page</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('RequirePasswordReset', () => {
  it('shows the page when password reset is available', async () => {
    openForgotPage(async () => ({ passwordReset: true, mail: true }))

    expect(await screen.findByText('forgot page')).not.toBeNull()
  })

  it('redirects to the login page when password reset is unavailable', async () => {
    openForgotPage(async () => ({ passwordReset: false, mail: false }))

    expect(await screen.findByText('login page')).not.toBeNull()
  })

  it('shows nothing while it does not know yet', () => {
    openForgotPage(() => new Promise(() => {}))

    expect(screen.queryByText('forgot page')).toBeNull()
    expect(screen.queryByText('login page')).toBeNull()
  })

  it('redirects when the question cannot be answered', async () => {
    openForgotPage(async () => { throw new Error('network') })

    await waitFor(() => expect(screen.getByText('login page')).not.toBeNull())
  })
})
