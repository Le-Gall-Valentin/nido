import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { DisableMethodDialog } from './DisableMethodDialog'
import {
  CodeError, CodeExpiredError, CodeSpentError, MaxAttemptsError, MethodNotEnabledError, ResendTooSoonError, SendLimitError,
  type ITwoFactorMethodsApi,
} from '@/features/two-factor'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'

type DisableApi = Pick<ITwoFactorMethodsApi, 'disable' | 'sendDisableCode'>

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: Record<string, unknown>) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

function fill(code: string) {
  fireEvent.change(document.querySelector('input[autocomplete="one-time-code"]')!, { target: { value: code } })
}

function open(method: 'APP' | 'MAIL', paused = false, api: Partial<DisableApi> = {}) {
  const handlers = { onClose: vi.fn(), onSuccess: vi.fn(), onStale: vi.fn() }
  const full: DisableApi = { disable: api.disable ?? vi.fn().mockResolvedValue(undefined), sendDisableCode: api.sendDisableCode ?? vi.fn() }
  render(<DisableMethodDialog method={method} paused={paused} address="camille@exemple.fr" resendAfterSeconds={0} api={full} {...handlers} />)
  return { ...handlers, api: full }
}

describe('DisableMethodDialog', () => {
  it('the app is turned off with its code', async () => {
    const { api, onSuccess } = open('APP')

    expect(screen.getAllByText('disable.title_app').length).toBeGreaterThan(0)
    fill('123456')
    fireEvent.click(screen.getByRole('button', { name: /disable\.submit/ }))

    await waitFor(() => expect(onSuccess).toHaveBeenCalled())
    expect(api.disable).toHaveBeenCalledWith('APP', '123456')
  })

  it('the mail is turned off with the code sent to its address, and another can be asked for', async () => {
    const { api } = open('MAIL', false, { sendDisableCode: vi.fn().mockResolvedValue({ resendAfterSeconds: 60 }) })

    expect(screen.getByText('disable.subtitle_mail:{"address":"camille@exemple.fr"}')).toBeTruthy()
    fireEvent.click(screen.getByText('resend.action'))
    await waitFor(() => expect(api.sendDisableCode).toHaveBeenCalled())
  })

  it('a paused mail is turned off without a code', async () => {
    const { api, onSuccess } = open('MAIL', true)

    expect(screen.getByText('disable.paused')).toBeTruthy()
    expect(document.querySelector('input[autocomplete="one-time-code"]')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: /disable\.submit/ }))

    await waitFor(() => expect(onSuccess).toHaveBeenCalled())
    expect(api.disable).toHaveBeenCalledWith('MAIL', undefined)
  })

  it('says what went wrong', async () => {
    open('APP', false, { disable: vi.fn().mockRejectedValueOnce(new CodeError()).mockRejectedValueOnce(new MaxAttemptsError()) })

    fill('000000'); fireEvent.click(screen.getByRole('button', { name: /disable\.submit/ }))
    expect(await screen.findByText('disable.error.invalid_code')).toBeTruthy()
    fill('000000'); fireEvent.click(screen.getByRole('button', { name: /disable\.submit/ }))
    expect(await screen.findByText('disable.error.max_attempts')).toBeTruthy()
  })

  it('a refused resend says when another can leave', async () => {
    open('MAIL', false, { sendDisableCode: vi.fn().mockRejectedValue(new SendLimitError(300)) })

    fireEvent.click(screen.getByText('resend.action'))

    expect(await screen.findByText('twoFactor:error.send_limit:{"minutes":5}')).toBeTruthy()
  })

  it('a paused mail that came back while the dialog was open hands back to the page', async () => {
    // Opened without a code field; the server now wants a code. Saying "invalid code" would leave nowhere to type one.
    const { onStale, onSuccess } = open('MAIL', true, { disable: vi.fn().mockRejectedValue(new CodeError()) })

    fireEvent.click(screen.getByRole('button', { name: /disable\.submit/ }))

    await waitFor(() => expect(onStale).toHaveBeenCalled())
    expect(onSuccess).not.toHaveBeenCalled()
  })

  it('a code spent by wrong guesses says to ask for a new one', async () => {
    open('MAIL', false, { disable: vi.fn().mockRejectedValue(new CodeSpentError()) })

    fill('004213')
    fireEvent.click(screen.getByRole('button', { name: /disable\.submit/ }))

    expect(await screen.findByText('disable.error.code_spent')).toBeTruthy()
  })

  it('a code that is no longer waiting says to ask for a new one, not that someone guessed', async () => {
    open('MAIL', false, { disable: vi.fn().mockRejectedValue(new CodeExpiredError()) })

    fill('004213')
    fireEvent.click(screen.getByRole('button', { name: /disable\.submit/ }))

    expect(await screen.findByText('disable.error.code_expired')).toBeTruthy()
  })

  it('the app spent by wrong codes says to wait, there being no code to ask for', async () => {
    open('APP', false, { disable: vi.fn().mockRejectedValue(new CodeSpentError()) })

    fill('000000')
    fireEvent.click(screen.getByRole('button', { name: /disable\.submit/ }))

    expect(await screen.findByText('disable.error.code_spent_app')).toBeTruthy()
  })

  it('the route limit, a lost network and a server error are said', async () => {
    open('APP', false, { disable: vi.fn()
      .mockRejectedValueOnce(new RateLimitError(40))
      .mockRejectedValueOnce(new NetworkError())
      .mockRejectedValueOnce(new ServerError()) })

    fill('000000'); fireEvent.click(screen.getByRole('button', { name: /disable\.submit/ }))
    expect(await screen.findByText('twoFactor:error.rate_limit_timed:{"seconds":40}')).toBeTruthy()
    fill('000000'); fireEvent.click(screen.getByRole('button', { name: /disable\.submit/ }))
    expect(await screen.findByText('twoFactor:error.network')).toBeTruthy()
    fill('000000'); fireEvent.click(screen.getByRole('button', { name: /disable\.submit/ }))
    expect(await screen.findByText('twoFactor:error.server')).toBeTruthy()
  })

  it('already off — another tab, an administrator — is what was asked', async () => {
    const { onSuccess } = open('APP', false, { disable: vi.fn().mockRejectedValue(new MethodNotEnabledError()) })

    fill('123456')
    fireEvent.click(screen.getByRole('button', { name: /disable\.submit/ }))

    await waitFor(() => expect(onSuccess).toHaveBeenCalled())
  })

  it('a resend asked again too soon keeps the code already sent, and says nothing went wrong', async () => {
    open('MAIL', false, { sendDisableCode: vi.fn().mockRejectedValue(new ResendTooSoonError(30)) })

    fireEvent.click(screen.getByText('resend.action'))

    expect(await screen.findByText('resend.wait:{"time":"0:30"}')).toBeTruthy()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('a resend lost to the network is said', async () => {
    open('MAIL', false, { sendDisableCode: vi.fn().mockRejectedValue(new NetworkError()) })

    fireEvent.click(screen.getByText('resend.action'))

    expect(await screen.findByText('twoFactor:error.network')).toBeTruthy()
  })

  it('a second click on resend while the first is on its way sends nothing', () => {
    const sendDisableCode = vi.fn(() => new Promise<never>(() => {}))
    open('MAIL', false, { sendDisableCode })

    const resend = screen.getByText('resend.action')
    fireEvent.click(resend)
    fireEvent.click(resend)

    expect(sendDisableCode).toHaveBeenCalledTimes(1)
  })
})
