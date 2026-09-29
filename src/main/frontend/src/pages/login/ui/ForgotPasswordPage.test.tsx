import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { IPasswordResetApi } from '@/features/password-reset'
import { ForgotPasswordPage } from './ForgotPasswordPage'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: object) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

describe('ForgotPasswordPage', () => {
  it('asks for an identifier, then says a link may be on its way — without saying whether the account exists', async () => {
    const api: IPasswordResetApi = {
      capabilities: vi.fn(), requestReset: vi.fn().mockResolvedValue(undefined), checkToken: vi.fn(), confirmReset: vi.fn(),
    }
    render(<MemoryRouter><ForgotPasswordPage api={api} /></MemoryRouter>)

    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('forgot.title')
    fireEvent.change(screen.getByLabelText('field.identifier'), { target: { value: 'jane' } })
    fireEvent.click(screen.getByRole('button', { name: 'action.send' }))

    await waitFor(() => expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('forgot.sent.title'))
    expect(screen.getByText('forgot.sent.body:{"identifier":"jane"}')).not.toBeNull()
    expect(screen.queryByLabelText('field.identifier')).toBeNull()
  })

  it('leads back to the login page', () => {
    const api = { capabilities: vi.fn(), requestReset: vi.fn(), checkToken: vi.fn(), confirmReset: vi.fn() }
    render(<MemoryRouter><ForgotPasswordPage api={api} /></MemoryRouter>)

    expect(screen.getByRole('link', { name: 'forgot.back' }).getAttribute('href')).toBe('/login')
  })
})
