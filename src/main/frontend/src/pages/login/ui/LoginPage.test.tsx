import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { LoginPage } from './LoginPage'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useAuth } from '@/features/auth'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/features/auth', () => ({
  LoginForm: ({ labelId, onLoginOutcome }: { labelId?: string; onLoginOutcome?: (o: unknown) => void }) => (
    <div>
      <form aria-label="login" aria-labelledby={labelId} />
      <button onClick={() => onLoginOutcome?.({ kind: 'totp_required', username: 'alice' })}>trigger-totp</button>
      <button onClick={() => onLoginOutcome?.({ kind: 'enrollment_proposed', user: { id: '1', username: 'alice', role: 'USER' } })}>trigger-enroll</button>
    </div>
  ),
  useAuth: vi.fn(),
}))

vi.mock('@/features/totp', () => ({
  TotpVerifyStep: ({ onVerified, onBack }: { onVerified: (u: unknown) => void; onBack: () => void }) => (
    <div>
      <button onClick={() => onVerified({ id: '1', username: 'alice', role: 'USER' })}>verify</button>
      <button onClick={onBack}>back</button>
    </div>
  ),
  TotpEnrollProposal: ({ onActivate, onSkip }: { onActivate: () => void; onSkip: () => void }) => (
    <div>
      <button onClick={onActivate}>activate</button>
      <button onClick={onSkip}>skip</button>
    </div>
  ),
  TotpSetupFlow: ({ onSuccess, onDismiss }: { onSuccess: () => void; onDismiss?: () => void }) => (
    <div>
      <button onClick={onSuccess}>setup-success</button>
      {onDismiss && <button onClick={onDismiss}>setup-dismiss</button>}
    </div>
  ),
  totpApi: {},
}))

const mockFinalizeLogin = vi.fn()
const mockTotpApi = { verify: vi.fn(), setup: vi.fn(), confirm: vi.fn(), getStatus: vi.fn(), disable: vi.fn() }

describe('LoginPage', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockImplementation((selector) =>
      selector({ finalizeLogin: mockFinalizeLogin, user: null, isInitializing: false, login: vi.fn(), logout: vi.fn(), initialize: vi.fn(), patchUser: vi.fn() } as Parameters<typeof selector>[0])
    )
    mockFinalizeLogin.mockClear()
  })

  describe('credentials step (initial)', () => {
    beforeEach(() => {
      render(
        <MemoryRouter>
          <LoginPage totpApi={mockTotpApi} />
        </MemoryRouter>
      )
    })

    it('renders the login form', () => {
      expect(screen.getByRole('form')).not.toBeNull()
    })

    it('renders the page title', () => {
      expect(screen.getByRole('heading', { level: 1 })).not.toBeNull()
      expect(screen.getByText('form.title')).not.toBeNull()
    })

    it('renders the subtitle', () => {
      expect(screen.getByText('form.subtitle')).not.toBeNull()
    })

    it('renders help text', () => {
      expect(screen.getByText('help.no_account')).not.toBeNull()
      expect(screen.getByText('help.contact_admin')).not.toBeNull()
    })

    it('renders the footer', () => {
      expect(screen.getByRole('contentinfo')).not.toBeNull()
      expect(screen.getByText('footer')).not.toBeNull()
    })

    it('links the form to the title via aria-labelledby', () => {
      const heading = screen.getByRole('heading', { level: 1 })
      const form = screen.getByRole('form')
      expect(heading.id).toBeTruthy()
      expect(form.getAttribute('aria-labelledby')).toBe(heading.id)
    })
  })

  describe('step transitions', () => {
    it('shows TotpVerifyStep when login outcome is totp_required', () => {
      render(<MemoryRouter><LoginPage totpApi={mockTotpApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-totp'))
      expect(screen.getByText('verify')).not.toBeNull()
      expect(screen.getByText('back')).not.toBeNull()
      expect(screen.queryByRole('form')).toBeNull()
    })

    it('shows TotpEnrollProposal when login outcome is enrollment_proposed', () => {
      render(<MemoryRouter><LoginPage totpApi={mockTotpApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-enroll'))
      expect(screen.getByText('activate')).not.toBeNull()
      expect(screen.getByText('skip')).not.toBeNull()
      expect(screen.queryByRole('form')).toBeNull()
    })

    it('returns to credentials step when back is clicked from totp step', () => {
      render(<MemoryRouter><LoginPage totpApi={mockTotpApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-totp'))
      fireEvent.click(screen.getByText('back'))
      expect(screen.getByRole('form')).not.toBeNull()
      expect(screen.queryByText('verify')).toBeNull()
    })

    it('shows TotpSetupFlow when user activates from enrollment proposal', () => {
      render(<MemoryRouter><LoginPage totpApi={mockTotpApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-enroll'))
      fireEvent.click(screen.getByText('activate'))
      expect(screen.getByText('setup-success')).not.toBeNull()
      expect(screen.getByText('setup-dismiss')).not.toBeNull()
      expect(screen.queryByText('activate')).toBeNull()
    })

    it('calls finalizeLogin after totp verify', () => {
      render(<MemoryRouter><LoginPage totpApi={mockTotpApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-totp'))
      fireEvent.click(screen.getByText('verify'))
      expect(mockFinalizeLogin).toHaveBeenCalledWith({ id: '1', username: 'alice', role: 'USER' })
    })

    it('calls finalizeLogin when skip is chosen from enrollment proposal', () => {
      render(<MemoryRouter><LoginPage totpApi={mockTotpApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-enroll'))
      fireEvent.click(screen.getByText('skip'))
      expect(mockFinalizeLogin).toHaveBeenCalledWith({ id: '1', username: 'alice', role: 'USER' })
    })

    it('calls finalizeLogin after setup success', () => {
      render(<MemoryRouter><LoginPage totpApi={mockTotpApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-enroll'))
      fireEvent.click(screen.getByText('activate'))
      fireEvent.click(screen.getByText('setup-success'))
      expect(mockFinalizeLogin).toHaveBeenCalledWith({ id: '1', username: 'alice', role: 'USER' })
    })

    it('calls finalizeLogin when setup is dismissed', () => {
      render(<MemoryRouter><LoginPage totpApi={mockTotpApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-enroll'))
      fireEvent.click(screen.getByText('activate'))
      fireEvent.click(screen.getByText('setup-dismiss'))
      expect(mockFinalizeLogin).toHaveBeenCalledWith({ id: '1', username: 'alice', role: 'USER' })
    })
  })
})