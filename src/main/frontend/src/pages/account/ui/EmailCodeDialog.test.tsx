import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { EmailCodeDialog } from './EmailCodeDialog'
import { EmailCodeInvalidError } from '../api/accountApi'
import { SendLimitError } from '@/features/two-factor'

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

    expect(await screen.findByText('profile.email_code.error.send_limit:{"minutes":5}')).toBeTruthy()
  })

  it('cancelling changes nothing', () => {
    const { onCancel } = open()

    fireEvent.click(screen.getByText('profile.email_code.cancel:{"address":"camille@ancienne.fr"}'))

    expect(onCancel).toHaveBeenCalled()
  })
})
