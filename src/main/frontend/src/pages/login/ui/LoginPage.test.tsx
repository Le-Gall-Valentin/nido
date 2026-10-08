import { act, render, screen, fireEvent, within, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { LoginPage } from './LoginPage'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useAuth } from '@/features/auth'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

/** The last callback the choice was given: what a request still in flight answers to once it lands. */
const late = vi.hoisted(() => ({
  choose: null as null | ((choice: unknown) => void),
}))

const ALICE = vi.hoisted(() => ({ id: '1', username: 'alice', email: 'alice@x.fr', role: 'USER', createdAt: '2026-01-01T00:00:00Z', twoFactorMethods: [] as string[] }))

vi.mock('@/features/auth', () => ({
  LoginForm: ({ labelId, onLoginOutcome, initialIdentifier }: { labelId?: string; onLoginOutcome?: (o: unknown) => void; initialIdentifier?: string }) => (
    <div>
      <form aria-label="login" aria-labelledby={labelId} />
      <output data-testid="identifier">{initialIdentifier ?? ''}</output>
      <button onClick={() => onLoginOutcome?.({ kind: 'two_factor_required', challenge: { username: 'alice', methods: ['APP'], maskedEmail: null, mailCode: null } })}>trigger-app</button>
      <button onClick={() => onLoginOutcome?.({ kind: 'two_factor_required', challenge: { username: 'alice', methods: ['MAIL'], maskedEmail: 'a••••••e@x.fr', mailCode: { sent: true, resendAfterSeconds: 60 } } })}>trigger-mail</button>
      <button onClick={() => onLoginOutcome?.({ kind: 'two_factor_required', challenge: { username: 'alice', methods: ['MAIL'], maskedEmail: 'a••••••e@x.fr', mailCode: { sent: false, retryAfterSeconds: 420 } } })}>trigger-mail-limit</button>
      <button onClick={() => onLoginOutcome?.({ kind: 'two_factor_required', challenge: { username: 'alice', methods: ['APP', 'MAIL'], maskedEmail: 'a••••••e@x.fr', mailCode: null } })}>trigger-both</button>
      <button onClick={() => onLoginOutcome?.({ kind: 'enrollment_proposed', user: ALICE })}>trigger-enroll</button>
    </div>
  ),
  useAuth: vi.fn(),
}))

vi.mock('@/features/two-factor', () => ({
  MethodChoiceStep: ({ onChoose, onBack }: { onChoose: (c: unknown) => void; onBack: () => void }) => (
    late.choose = onChoose,
    <div>
      <span>choice</span>
      <button onClick={() => onChoose({ method: 'APP' })}>choose-app</button>
      <button onClick={() => onChoose({ method: 'MAIL', resendAfterSeconds: 60 })}>choose-mail</button>
      <button onClick={onBack}>back</button>
    </div>
  ),
  CodeStep: ({ method, resendAfterSeconds, mailLimitSeconds, onVerified, onBack, onChooseAnother }: {
    method: string; resendAfterSeconds?: number; mailLimitSeconds?: number | null
    onVerified: (u: unknown) => void; onBack: () => void; onChooseAnother?: () => void
  }) => (
    <div>
      <span>{`code-${method}-${resendAfterSeconds ?? 0}-${mailLimitSeconds ?? 'none'}`}</span>
      <button onClick={() => onVerified(ALICE)}>verify</button>
      <button onClick={onBack}>back</button>
      {onChooseAnother && <button onClick={onChooseAnother}>choose-another</button>}
    </div>
  ),
  EnrollProposal: ({ mailAvailable, onAppChosen, onMailStarted, onSkip }: {
    mailAvailable: boolean; onAppChosen: () => void; onMailStarted: (s: unknown) => void; onSkip: () => void
  }) => (
    <div>
      <span>{`proposal-mail-${mailAvailable}`}</span>
      <button onClick={onAppChosen}>activate-app</button>
      <button onClick={() => onMailStarted({ sentTo: 'alice@x.fr', resendAfterSeconds: 60 })}>activate-mail</button>
      <button onClick={onSkip}>skip</button>
    </div>
  ),
  AppSetupFlow: ({ onSuccess, onDismiss }: { onSuccess: () => void; onDismiss?: () => void }) => (
    <div>
      <button onClick={onSuccess}>setup-success</button>
      {onDismiss && <button onClick={onDismiss}>setup-dismiss</button>}
    </div>
  ),
  MailSetupStep: ({ sentTo, onSuccess, onBack, onDismiss }: { sentTo: string; onSuccess: () => void; onBack?: () => void; onDismiss?: () => void }) => (
    <div>
      <span>{`mail-setup-${sentTo}`}</span>
      <button onClick={onSuccess}>mail-setup-success</button>
      {onBack && <button onClick={onBack}>mail-setup-back</button>}
      {onDismiss && <button onClick={onDismiss}>mail-setup-dismiss</button>}
    </div>
  ),
  twoFactorApi: {},
}))

const availability = vi.hoisted(() => ({ reset: 'unavailable' as 'loading' | 'available' | 'unavailable', mail: 'unavailable' as 'loading' | 'available' | 'unavailable' }))

vi.mock('@/entities/capabilities', () => ({
  usePasswordResetAvailability: () => availability.reset,
  useMailAvailability: () => availability.mail,
  capabilitiesApi: {},
}))

const mockFinalizeLogin = vi.fn()

describe('LoginPage', () => {
  beforeEach(() => {
    availability.reset = 'unavailable'
    availability.mail = 'unavailable'
    vi.mocked(useAuth).mockImplementation((selector) =>
      selector({ finalizeLogin: mockFinalizeLogin, user: null, isInitializing: false, signedOut: false, signingIn: null, login: vi.fn(), logout: vi.fn(), initialize: vi.fn(), patchUser: vi.fn() })
    )
    mockFinalizeLogin.mockClear()
  })

  describe('credentials step (initial)', () => {
    beforeEach(() => {
      render(
        <MemoryRouter>
          <LoginPage />
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

  describe('second factor', () => {
    const renderPage = () => render(<MemoryRouter><LoginPage /></MemoryRouter>)

    it('the app alone goes straight to its code, with no way to another method', () => {
      renderPage()
      fireEvent.click(screen.getByText('trigger-app'))

      expect(screen.getByText('code-APP-0-none')).toBeTruthy()
      expect(screen.queryByText('choose-another')).toBeNull()
    })

    it('the mail alone goes to its code, already sent', () => {
      renderPage()
      fireEvent.click(screen.getByText('trigger-mail'))

      expect(screen.getByText('code-MAIL-60-none')).toBeTruthy()
    })

    it('a code the login could not send carries the wait', () => {
      renderPage()
      fireEvent.click(screen.getByText('trigger-mail-limit'))

      expect(screen.getByText('code-MAIL-0-420')).toBeTruthy()
    })

    it('both lead to the choice, and back to it under the same sign-in', () => {
      renderPage()
      fireEvent.click(screen.getByText('trigger-both'))
      expect(screen.getByText('choice')).toBeTruthy()

      fireEvent.click(screen.getByText('choose-mail'))
      expect(screen.getByText('code-MAIL-60-none')).toBeTruthy()

      fireEvent.click(screen.getByText('choose-another'))
      fireEvent.click(screen.getByText('choose-app'))
      expect(screen.getByText('code-APP-0-none')).toBeTruthy()
    })

    it('a code that lands after going back leaves the identifiers in place', () => {
      // The mail's code was on its way when "back" was pressed: its answer must not open a code screen
      // for a sign-in that is no longer there — the column would stay empty until a reload.
      renderPage()
      fireEvent.click(screen.getByText('trigger-both'))
      const choose = late.choose
      fireEvent.click(screen.getByText('back'))

      act(() => choose?.({ method: 'MAIL', resendAfterSeconds: 60 }))

      expect(screen.getByRole('form')).toBeTruthy()
      expect(screen.queryByText('code-MAIL-60-none')).toBeNull()
    })

    it('back from a code returns to the identifiers', () => {
      renderPage()
      fireEvent.click(screen.getByText('trigger-app'))
      fireEvent.click(screen.getByText('back'))

      expect(screen.getByRole('form')).toBeTruthy()
    })

    it('a right code finishes the sign-in', () => {
      renderPage()
      fireEvent.click(screen.getByText('trigger-app'))
      fireEvent.click(screen.getByText('verify'))

      expect(mockFinalizeLogin).toHaveBeenCalledWith(ALICE)
    })
  })

  describe('proposal', () => {
    const renderPage = () => render(<MemoryRouter><LoginPage /></MemoryRouter>)

    it('offers the mail only when the server can send it', () => {
      renderPage()
      fireEvent.click(screen.getByText('trigger-enroll'))
      expect(screen.getByText('proposal-mail-false')).toBeTruthy()
    })

    it('offers both when mail is on', () => {
      availability.mail = 'available'
      renderPage()
      fireEvent.click(screen.getByText('trigger-enroll'))
      expect(screen.getByText('proposal-mail-true')).toBeTruthy()
    })

    it('the app, once set up, signs in with the method on', () => {
      renderPage()
      fireEvent.click(screen.getByText('trigger-enroll'))
      fireEvent.click(screen.getByText('activate-app'))
      fireEvent.click(screen.getByText('setup-success'))

      expect(mockFinalizeLogin).toHaveBeenCalledWith({ ...ALICE, twoFactorMethods: ['APP'] })
    })

    it('the mail goes to the address check, and back to the proposal if wanted', () => {
      renderPage()
      fireEvent.click(screen.getByText('trigger-enroll'))
      fireEvent.click(screen.getByText('activate-mail'))
      expect(screen.getByText('mail-setup-alice@x.fr')).toBeTruthy()

      fireEvent.click(screen.getByText('mail-setup-back'))
      expect(screen.getByText(/proposal-mail/)).toBeTruthy()
    })

    it('the mail, once confirmed, signs in with the method on', () => {
      renderPage()
      fireEvent.click(screen.getByText('trigger-enroll'))
      fireEvent.click(screen.getByText('activate-mail'))
      fireEvent.click(screen.getByText('mail-setup-success'))

      expect(mockFinalizeLogin).toHaveBeenCalledWith({ ...ALICE, twoFactorMethods: ['MAIL'] })
    })

    it('skipping or leaving a setup signs in as is', () => {
      renderPage()
      fireEvent.click(screen.getByText('trigger-enroll'))
      fireEvent.click(screen.getByText('skip'))
      expect(mockFinalizeLogin).toHaveBeenLastCalledWith(ALICE)
    })
  })

  describe('forgotten password', () => {
    function Where() {
      const location = useLocation()
      return <output data-testid="where">{JSON.stringify(location.state)}</output>
    }

    it('offers the link under the form when the server can send it', () => {
      availability.reset = 'available'
      render(<MemoryRouter><LoginPage /></MemoryRouter>)

      expect(screen.getByRole('link', { name: 'forgot.link' }).getAttribute('href')).toBe('/forgot-password')
    })

    it.each(['unavailable', 'loading'] as const)('shows the page as it always was when %s', (state) => {
      availability.reset = state
      render(<MemoryRouter><LoginPage /></MemoryRouter>)

      expect(screen.queryByRole('link', { name: 'forgot.link' })).toBeNull()
      expect(screen.getByText('help.contact_admin')).not.toBeNull()
    })

    it('after an accepted invitation, welcomes once and fills in the identifier', async () => {
      render(
        <MemoryRouter initialEntries={[{ pathname: '/login', state: { invitation: 'accepted', identifier: 'carol' } }]}>
          <Routes>
            <Route path="/login" element={<><LoginPage /><Where /></>} />
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
            <Route path="/login" element={<><LoginPage /><Where /></>} />
          </Routes>
        </MemoryRouter>,
      )

      expect(screen.getByText('reset.done')).not.toBeNull()
      await waitFor(() => expect(screen.getByTestId('where').textContent).toBe('null'))
      expect(screen.getByText('reset.done')).not.toBeNull()
    })
  })
})