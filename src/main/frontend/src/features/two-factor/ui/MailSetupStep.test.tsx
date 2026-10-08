import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MailSetupStep } from './MailSetupStep'
import {
  CodeError, ConfirmMaxAttemptsError, EnrolmentExpiredError, MethodAlreadyEnabledError, MethodUnavailableError, ResendTooSoonError,
  SendLimitError,
} from '../model/errors'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'
import type { ITwoFactorMethodsApi } from '../model/ITwoFactorMethodsApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: Record<string, unknown>) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

function fill(container: HTMLElement, code: string) {
  fireEvent.change(container.querySelector('input[autocomplete="one-time-code"]')!, { target: { value: code } })
}

function setup(api: Partial<Pick<ITwoFactorMethodsApi, 'setupMail' | 'confirm'>> = {}, variant: 'page' | 'dialog' = 'page', wait = 0) {
  const handlers = { onSuccess: vi.fn(), onBack: vi.fn(), onDismiss: vi.fn() }
  const view = render(<MailSetupStep sentTo="camille@exemple.fr" resendAfterSeconds={wait} variant={variant}
    api={{ setupMail: api.setupMail ?? vi.fn(), confirm: api.confirm ?? vi.fn() }} {...handlers} />)
  return { ...handlers, ...view }
}

describe('MailSetupStep', () => {
  it('on the page, has its heading and the way back to the choice', () => {
    const { onBack } = setup()

    expect(screen.getByText('mailSetup.title')).toBeTruthy()
    expect(screen.getByText('mailSetup.subtitle:{"address":"camille@exemple.fr"}')).toBeTruthy()
    fireEvent.click(screen.getByText('mailSetup.back'))
    expect(onBack).toHaveBeenCalled()
  })

  it('in a dialog, the dialog gives the title', () => {
    setup({}, 'dialog')

    expect(screen.queryByText('mailSetup.title')).toBeNull()
    expect(screen.getByText('mailSetup.subtitle:{"address":"camille@exemple.fr"}')).toBeTruthy()
  })

  it('the right code turns the mail on', async () => {
    const confirm = vi.fn().mockResolvedValue(undefined)
    const { container, onSuccess } = setup({ confirm })

    fill(container, '004213')
    fireEvent.click(screen.getByRole('button', { name: /mailSetup\.submit/ }))

    await waitFor(() => expect(onSuccess).toHaveBeenCalled())
    expect(confirm).toHaveBeenCalledWith('MAIL', '004213')
  })

  it('names each way the code can fail', async () => {
    const confirm = vi.fn()
      .mockRejectedValueOnce(new CodeError())
      .mockRejectedValueOnce(new ConfirmMaxAttemptsError())
      .mockRejectedValueOnce(new EnrolmentExpiredError())
      .mockRejectedValueOnce(new MethodUnavailableError())
    const { container } = setup({ confirm })
    const submit = () => { fill(container, '000000'); fireEvent.click(screen.getByRole('button', { name: /mailSetup\.submit/ })) }

    submit(); expect(await screen.findByText('mailSetup.error.invalid_code')).toBeTruthy()
    submit(); expect(await screen.findByText('mailSetup.error.max_attempts')).toBeTruthy()
    submit(); expect(await screen.findByText('mailSetup.error.expired')).toBeTruthy()
    submit(); expect(await screen.findByText('mailSetup.error.mail_unavailable')).toBeTruthy()
  })

  it('already on — another tab got there first — is a success', async () => {
    const { container, onSuccess } = setup({ confirm: vi.fn().mockRejectedValue(new MethodAlreadyEnabledError()) })

    fill(container, '004213')
    fireEvent.click(screen.getByRole('button', { name: /mailSetup\.submit/ }))

    await waitFor(() => expect(onSuccess).toHaveBeenCalled())
  })

  it('asks for a new code from the resend link', async () => {
    const setupMail = vi.fn().mockResolvedValue({ sentTo: 'camille@exemple.fr', resendAfterSeconds: 60 })
    setup({ setupMail })

    fireEvent.click(screen.getByText('resend.action'))

    await waitFor(() => expect(setupMail).toHaveBeenCalled())
    expect(await screen.findByText('resend.wait:{"time":"1:00"}')).toBeTruthy()
  })

  it('the route limit, a lost network and a server error are said on a submit', async () => {
    const confirm = vi.fn()
      .mockRejectedValueOnce(new RateLimitError(30))
      .mockRejectedValueOnce(new NetworkError())
      .mockRejectedValueOnce(new ServerError())
    const { container } = setup({ confirm })
    const submit = () => { fill(container, '000000'); fireEvent.click(screen.getByRole('button', { name: /mailSetup\.submit/ })) }

    submit(); expect(await screen.findByText('twoFactor:error.rate_limit_timed:{"seconds":30}')).toBeTruthy()
    submit(); expect(await screen.findByText('twoFactor:error.network')).toBeTruthy()
    submit(); expect(await screen.findByText('twoFactor:error.server')).toBeTruthy()
  })

  it('a code ended by wrong guesses or by time offers a new one at once', async () => {
    const confirm = vi.fn().mockRejectedValueOnce(new ConfirmMaxAttemptsError()).mockRejectedValueOnce(new EnrolmentExpiredError())
    const { container } = setup({ confirm }, 'page', 60)
    const submit = () => { fill(container, '000000'); fireEvent.click(screen.getByRole('button', { name: /mailSetup\.submit/ })) }
    expect(screen.queryByText('resend.action')).toBeNull()

    submit()
    expect(await screen.findByText('resend.action')).toBeTruthy()
  })

  it('half a code is not sent', () => {
    const confirm = vi.fn()
    const { container } = setup({ confirm })

    fill(container, '004')
    fireEvent.click(screen.getByRole('button', { name: /mailSetup\.submit/ }))

    expect(screen.getByText('mailSetup.error.incomplete')).toBeTruthy()
    expect(confirm).not.toHaveBeenCalled()
  })

  it('a resend asked again too soon keeps the code already sent, and says when another can leave', async () => {
    const setupMail = vi.fn().mockRejectedValueOnce(new ResendTooSoonError(40))
    setup({ setupMail })

    fireEvent.click(screen.getByText('resend.action'))
    expect(await screen.findByText('resend.wait:{"time":"0:40"}')).toBeTruthy()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('a resend stopped by the account limit, by mail switched off or by the network is said', async () => {
    for (const [error, text] of [
      [new SendLimitError(600), 'twoFactor:error.send_limit:{"minutes":10}'],
      [new MethodUnavailableError(), 'mailSetup.error.mail_unavailable'],
      [new NetworkError(), 'twoFactor:error.network'],
    ] as const) {
      const view = setup({ setupMail: vi.fn().mockRejectedValue(error) })
      fireEvent.click(screen.getByText('resend.action'))
      expect(await screen.findByText(text)).toBeTruthy()
      view.unmount()
    }
  })

  it('a second click on resend while the first is on its way sends nothing', () => {
    const setupMail = vi.fn(() => new Promise<never>(() => {}))
    setup({ setupMail })

    const resend = screen.getByText('resend.action')
    fireEvent.click(resend)
    fireEvent.click(resend)

    expect(setupMail).toHaveBeenCalledTimes(1)
  })

  it('can be left for later', () => {
    const { onDismiss } = setup()

    fireEvent.click(screen.getByText('setup.dismiss_login'))

    expect(onDismiss).toHaveBeenCalled()
  })
})
