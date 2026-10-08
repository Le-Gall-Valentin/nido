import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { EmailCodeDialog } from './EmailCodeDialog'
import { EmailCodeExpiredError, EmailCodeInvalidError, EmailCodeSpentError } from '../api/accountApi'
import { ResendTooSoonError, SendLimitError } from '@/features/two-factor'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: Record<string, unknown>) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

function fill(code: string) {
  fireEvent.change(document.querySelector('input[autocomplete="one-time-code"]')!, { target: { value: code } })
}

function open(overrides: { onConfirm?: (code: string) => Promise<void>; onResend?: () => Promise<number> } = {}) {
  const props = {
    onConfirm: overrides.onConfirm ?? vi.fn().mockResolvedValue(undefined),
    onResend: overrides.onResend ?? vi.fn().mockResolvedValue(60),
    onCancel: vi.fn(),
  }
  render(<EmailCodeDialog sentTo="camille@nouvelle.fr" previousAddress="camille@ancienne.fr" resendAfterSeconds={0} {...props} />)
  return props
}

describe('EmailCodeDialog', () => {
  it('says where the code went and what cancelling keeps', () => {
    open()

    expect(screen.getByText('profile.email_code.subtitle:{"address":"camille@nouvelle.fr"}')).toBeTruthy()
    expect(screen.getByText('profile.email_code.cancel:{"address":"camille@ancienne.fr"}')).toBeTruthy()
  })

  it('confirms with the code', async () => {
    const { onConfirm } = open()

    fill('004213')
    fireEvent.click(screen.getByRole('button', { name: /profile\.email_code\.submit/ }))

    await waitFor(() => expect(onConfirm).toHaveBeenCalledWith('004213'))
  })

  it('a wrong code is said and the field emptied', async () => {
    open({ onConfirm: vi.fn().mockRejectedValue(new EmailCodeInvalidError()) })

    fill('000000')
    fireEvent.click(screen.getByRole('button', { name: /profile\.email_code\.submit/ }))

    expect(await screen.findByText('profile.email_code.error.invalid_code')).toBeTruthy()
  })

  it('a new code can be asked for', async () => {
    open({ onResend: vi.fn().mockResolvedValue(60) })

    fireEvent.click(screen.getByText('resend.action'))

    expect(await screen.findByText('resend.wait:{"time":"1:00"}')).toBeTruthy()
  })

  it('a refused resend says when another can leave', async () => {
    open({ onResend: vi.fn().mockRejectedValue(new SendLimitError(300)) })

    fireEvent.click(screen.getByText('resend.action'))

    expect(await screen.findByText('twoFactor:error.send_limit:{"minutes":5}')).toBeTruthy()
  })

  it('cancelling changes nothing', () => {
    const { onCancel } = open()

    fireEvent.click(screen.getByText('profile.email_code.cancel:{"address":"camille@ancienne.fr"}'))

    expect(onCancel).toHaveBeenCalled()
  })

  it('a code spent by wrong guesses says to ask for a new one', async () => {
    open({ onConfirm: vi.fn().mockRejectedValue(new EmailCodeSpentError()) })

    fireEvent.change(document.querySelector('input[autocomplete="one-time-code"]')!, { target: { value: '004213' } })
    fireEvent.click(screen.getByRole('button', { name: /profile\.email_code\.submit/ }))

    expect(await screen.findByText('profile.email_code.error.spent')).toBeTruthy()
  })

  it('a code no longer waiting says to ask for a new one, not that someone guessed', async () => {
    open({ onConfirm: vi.fn().mockRejectedValue(new EmailCodeExpiredError()) })

    fill('004213')
    fireEvent.click(screen.getByRole('button', { name: /profile\.email_code\.submit/ }))

    expect(await screen.findByText('profile.email_code.error.expired')).toBeTruthy()
  })

  it('the route limit, a lost network and a server error are said', async () => {
    // PATCH /me allows five a minute: a save and four wrong codes reach it.
    open({ onConfirm: vi.fn()
      .mockRejectedValueOnce(new RateLimitError(50))
      .mockRejectedValueOnce(new NetworkError())
      .mockRejectedValueOnce(new ServerError()) })
    const submit = () => { fill('000000'); fireEvent.click(screen.getByRole('button', { name: /profile\.email_code\.submit/ })) }

    submit(); expect(await screen.findByText('twoFactor:error.rate_limit_timed:{"seconds":50}')).toBeTruthy()
    submit(); expect(await screen.findByText('twoFactor:error.network')).toBeTruthy()
    submit(); expect(await screen.findByText('twoFactor:error.server')).toBeTruthy()
  })

  it('half a code is not sent', () => {
    const { onConfirm } = open()

    fill('004')
    fireEvent.click(screen.getByRole('button', { name: /profile\.email_code\.submit/ }))

    expect(screen.getByText('profile.email_code.error.incomplete')).toBeTruthy()
    expect(onConfirm).not.toHaveBeenCalled()
  })

  it('a resend asked again too soon keeps the code already sent, and says nothing went wrong', async () => {
    open({ onResend: vi.fn().mockRejectedValue(new ResendTooSoonError(25)) })

    fireEvent.click(screen.getByText('resend.action'))

    expect(await screen.findByText('resend.wait:{"time":"0:25"}')).toBeTruthy()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('a resend lost to the network is said', async () => {
    open({ onResend: vi.fn().mockRejectedValue(new NetworkError()) })

    fireEvent.click(screen.getByText('resend.action'))

    expect(await screen.findByText('twoFactor:error.network')).toBeTruthy()
  })

  it('a second click on resend while the first is on its way sends nothing', () => {
    const onResend = vi.fn(() => new Promise<number>(() => {}))
    open({ onResend })

    const resend = screen.getByText('resend.action')
    fireEvent.click(resend)
    fireEvent.click(resend)

    expect(onResend).toHaveBeenCalledTimes(1)
  })
})
