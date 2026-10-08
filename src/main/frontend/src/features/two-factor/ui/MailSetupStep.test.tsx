import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MailSetupStep } from './MailSetupStep'
import { CodeError, ConfirmMaxAttemptsError, EnrolmentExpiredError, MethodAlreadyEnabledError, MethodUnavailableError } from '../model/errors'
import type { ITwoFactorMethodsApi } from '../model/ITwoFactorMethodsApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: Record<string, unknown>) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

function fill(container: HTMLElement, code: string) {
  fireEvent.change(container.querySelector('input[autocomplete="one-time-code"]')!, { target: { value: code } })
}

function setup(api: Partial<Pick<ITwoFactorMethodsApi, 'setupMail' | 'confirm'>> = {}, variant: 'page' | 'dialog' = 'page') {
  const handlers = { onSuccess: vi.fn(), onBack: vi.fn(), onDismiss: vi.fn() }
  const view = render(<MailSetupStep sentTo="camille@exemple.fr" resendAfterSeconds={0} variant={variant}
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
})
