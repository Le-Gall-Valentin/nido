import { render, screen, fireEvent, within, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { LoginPage } from './LoginPage'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useAuth } from '@/features/auth'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/features/auth', () => ({
  LoginForm: ({ labelId, onLoginOutcome, initialIdentifier }: { labelId?: string; onLoginOutcome?: (o: unknown) => void; initialIdentifier?: string }) => (
    <div>
      <form aria-label="login" aria-labelledby={labelId} />
      <output data-testid="identifier">{initialIdentifier ?? ''}</output>
      <button onClick={() => onLoginOutcome?.({ kind: 'totp_required', username: 'alice' })}>trigger-totp</button>
      <button onClick={() => onLoginOutcome?.({ kind: 'enrollment_proposed', user: { id: '1', username: 'alice', role: 'USER' } })}>trigger-enroll</button>
    </div>
  ),
  useAuth: vi.fn(),
}))

vi.mock('@/features/two-factor', () => ({
  CodeStep: ({ onVerified, onBack }: { onVerified: (u: unknown) => void; onBack: () => void }) => (
    <div>
      <button onClick={() => onVerified({ id: '1', username: 'alice', role: 'USER' })}>verify</button>
      <button onClick={onBack}>back</button>
    </div>
  ),
  EnrollProposal: ({ onActivate, onSkip }: { onActivate: () => void; onSkip: () => void }) => (
    <div>
      <button onClick={onActivate}>activate</button>
      <button onClick={onSkip}>skip</button>
    </div>
  ),
  AppSetupFlow: ({ onSuccess, onDismiss }: { onSuccess: () => void; onDismiss?: () => void }) => (
    <div>
      <button onClick={onSuccess}>setup-success</button>
      {onDismiss && <button onClick={onDismiss}>setup-dismiss</button>}
    </div>
  ),
  twoFactorApi: {},
}))

const availability = vi.hoisted(() => ({ current: 'unavailable' as 'loading' | 'available' | 'unavailable' }))

vi.mock('@/entities/capabilities', () => ({
  usePasswordResetAvailability: () => availability.current,
  capabilitiesApi: {},
}))

const mockFinalizeLogin = vi.fn()
const mockTwoFactorApi = {
  verify: vi.fn(), sendMailCode: vi.fn(), list: vi.fn(), setupApp: vi.fn(), setupMail: vi.fn(),
  confirm: vi.fn(), sendDisableCode: vi.fn(), disable: vi.fn(),
}

describe('LoginPage', () => {
  beforeEach(() => {
    availability.current = 'unavailable'
    vi.mocked(useAuth).mockImplementation((selector) =>
      selector({ finalizeLogin: mockFinalizeLogin, user: null, isInitializing: false, signedOut: false, signingIn: null, login: vi.fn(), logout: vi.fn(), initialize: vi.fn(), patchUser: vi.fn() })
    )
    mockFinalizeLogin.mockClear()
  })

  describe('credentials step (initial)', () => {
    beforeEach(() => {
      render(
        <MemoryRouter>
          <LoginPage twoFactorApi={mockTwoFactorApi} />
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

    it('renders the Nido brand in the mobile header', () => {
      const mobileHeader = screen.getByTestId('mobile-header')
      expect(within(mobileHeader).getByText('Nido')).not.toBeNull()
      expect(mobileHeader.querySelector('svg')).not.toBeNull()
    })

    it('links the form to the title via aria-labelledby', () => {
      const heading = screen.getByRole('heading', { level: 1 })
      const form = screen.getByRole('form')
      expect(heading.id).toBeTruthy()
      expect(form.getAttribute('aria-labelledby')).toBe(heading.id)
    })
  })

  describe('step transitions', () => {
    it('shows the code step when login outcome is totp_required', () => {
      render(<MemoryRouter><LoginPage twoFactorApi={mockTwoFactorApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-totp'))
      expect(screen.getByText('verify')).not.toBeNull()
      expect(screen.getByText('back')).not.toBeNull()
      expect(screen.queryByRole('form')).toBeNull()
    })

    it('shows the enrol proposal when login outcome is enrollment_proposed', () => {
      render(<MemoryRouter><LoginPage twoFactorApi={mockTwoFactorApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-enroll'))
      expect(screen.getByText('activate')).not.toBeNull()
      expect(screen.getByText('skip')).not.toBeNull()
      expect(screen.queryByRole('form')).toBeNull()
    })

    it('returns to credentials step when back is clicked from totp step', () => {
      render(<MemoryRouter><LoginPage twoFactorApi={mockTwoFactorApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-totp'))
      fireEvent.click(screen.getByText('back'))
      expect(screen.getByRole('form')).not.toBeNull()
      expect(screen.queryByText('verify')).toBeNull()
    })

    it('shows the app setup when user activates from enrollment proposal', () => {
      render(<MemoryRouter><LoginPage twoFactorApi={mockTwoFactorApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-enroll'))
      fireEvent.click(screen.getByText('activate'))
      expect(screen.getByText('setup-success')).not.toBeNull()
      expect(screen.getByText('setup-dismiss')).not.toBeNull()
      expect(screen.queryByText('activate')).toBeNull()
    })

    it('calls finalizeLogin after totp verify', () => {
      render(<MemoryRouter><LoginPage twoFactorApi={mockTwoFactorApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-totp'))
      fireEvent.click(screen.getByText('verify'))
      expect(mockFinalizeLogin).toHaveBeenCalledWith({ id: '1', username: 'alice', role: 'USER' })
    })

    it('calls finalizeLogin when skip is chosen from enrollment proposal', () => {
      render(<MemoryRouter><LoginPage twoFactorApi={mockTwoFactorApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-enroll'))
      fireEvent.click(screen.getByText('skip'))
      expect(mockFinalizeLogin).toHaveBeenCalledWith({ id: '1', username: 'alice', role: 'USER' })
    })

    it('calls finalizeLogin after setup success', () => {
      render(<MemoryRouter><LoginPage twoFactorApi={mockTwoFactorApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-enroll'))
      fireEvent.click(screen.getByText('activate'))
      fireEvent.click(screen.getByText('setup-success'))
      expect(mockFinalizeLogin).toHaveBeenCalledWith({ id: '1', username: 'alice', role: 'USER' })
    })

    it('calls finalizeLogin when setup is dismissed', () => {
      render(<MemoryRouter><LoginPage twoFactorApi={mockTwoFactorApi} /></MemoryRouter>)
      fireEvent.click(screen.getByText('trigger-enroll'))
      fireEvent.click(screen.getByText('activate'))
      fireEvent.click(screen.getByText('setup-dismiss'))
      expect(mockFinalizeLogin).toHaveBeenCalledWith({ id: '1', username: 'alice', role: 'USER' })
    })
  })

  describe('forgotten password', () => {
    function Where() {
      const location = useLocation()
      return <output data-testid="where">{JSON.stringify(location.state)}</output>
    }

    it('offers the link under the form when the server can send it', () => {
      availability.current = 'available'
      render(<MemoryRouter><LoginPage twoFactorApi={mockTwoFactorApi} /></MemoryRouter>)

      expect(screen.getByRole('link', { name: 'forgot.link' }).getAttribute('href')).toBe('/forgot-password')
    })

    it.each(['unavailable', 'loading'] as const)('shows the page as it always was when %s', (state) => {
      availability.current = state
      render(<MemoryRouter><LoginPage twoFactorApi={mockTwoFactorApi} /></MemoryRouter>)

      expect(screen.queryByRole('link', { name: 'forgot.link' })).toBeNull()
      expect(screen.getByText('help.contact_admin')).not.toBeNull()
    })

    it('after an accepted invitation, welcomes once and fills in the identifier', async () => {
      render(
        <MemoryRouter initialEntries={[{ pathname: '/login', state: { invitation: 'accepted', identifier: 'carol' } }]}>
          <Routes>
            <Route path="/login" element={<><LoginPage twoFactorApi={mockTwoFactorApi} /><Where /></>} />
          </Routes>
        </MemoryRouter>,
      )

      expect(screen.getByText('welcome.done')).not.toBeNull()
      expect(screen.getByTestId('identifier').textContent).toBe('carol')
      await waitFor(() => expect(screen.getByTestId('where').textContent).toBe('null'))
      expect(screen.getByText('welcome.done')).not.toBeNull()
      expect(screen.getByTestId('identifier').textContent).toBe('carol')
    })

    it('shows the success banner once and takes its reason out of the history entry', async () => {
      render(
        <MemoryRouter initialEntries={[{ pathname: '/login', state: { passwordReset: 'done' } }]}>
          <Routes>
            <Route path="/login" element={<><LoginPage twoFactorApi={mockTwoFactorApi} /><Where /></>} />
          </Routes>
        </MemoryRouter>,
      )

      expect(screen.getByText('reset.done')).not.toBeNull()
      await waitFor(() => expect(screen.getByTestId('where').textContent).toBe('null'))
      expect(screen.getByText('reset.done')).not.toBeNull()
    })
  })
})