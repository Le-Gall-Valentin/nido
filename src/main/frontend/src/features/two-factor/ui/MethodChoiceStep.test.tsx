import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MethodChoiceStep } from './MethodChoiceStep'
import { ChallengeExpiredError, MethodNotEnabledError, MethodUnavailableError, ResendTooSoonError, SendLimitError } from '../model/errors'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: Record<string, unknown>) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

function setup(sendMailCode = vi.fn()) {
  const onChoose = vi.fn()
  const onBack = vi.fn()
  render(<MethodChoiceStep username="camille" maskedEmail="c••••••n@exemple.fr" api={{ sendMailCode }} onChoose={onChoose} onBack={onBack} />)
  return { onChoose, onBack, sendMailCode }
}

const card = (name: RegExp) => screen.getByRole('button', { name })

describe('MethodChoiceStep', () => {
  it('greets the account and offers both methods, the address masked', () => {
    setup()

    expect(screen.getByText('choose.subtitle:{"username":"camille"}')).toBeTruthy()
    expect(card(/method\.app_title/)).toBeTruthy()
    expect(screen.getByText('method.mail_choice:{"address":"c••••••n@exemple.fr"}')).toBeTruthy()
  })

  it('the app needs nothing sent', () => {
    const { onChoose, sendMailCode } = setup()

    fireEvent.click(card(/method\.app_title/))

    expect(onChoose).toHaveBeenCalledWith({ method: 'APP' })
    expect(sendMailCode).not.toHaveBeenCalled()
  })

  it('the mail sends its code on the click, then moves on with the wait', async () => {
    const { onChoose } = setup(vi.fn().mockResolvedValue({ resendAfterSeconds: 60 }))

    fireEvent.click(card(/method\.mail_title/))

    await waitFor(() => expect(onChoose).toHaveBeenCalledWith({ method: 'MAIL', resendAfterSeconds: 60 }))
  })

  it('coming back within a minute goes to the code already sent', async () => {
    const { onChoose } = setup(vi.fn().mockRejectedValue(new ResendTooSoonError(35)))

    fireEvent.click(card(/method\.mail_title/))

    await waitFor(() => expect(onChoose).toHaveBeenCalledWith({ method: 'MAIL', resendAfterSeconds: 35 }))
  })

  it('a refusal is said on the choice and nothing moves on', async () => {
    const sendMailCode = vi.fn()
      .mockRejectedValueOnce(new SendLimitError(600))
      .mockRejectedValueOnce(new MethodUnavailableError())
      .mockRejectedValueOnce(new ChallengeExpiredError())
    const { onChoose } = setup(sendMailCode)

    fireEvent.click(card(/method\.mail_title/))
    expect(await screen.findByText('choose.error.send_limit:{"minutes":10}')).toBeTruthy()
    fireEvent.click(card(/method\.mail_title/))
    expect(await screen.findByText('choose.error.mail_unavailable')).toBeTruthy()
    fireEvent.click(card(/method\.mail_title/))
    expect(await screen.findByText('choose.error.challenge_expired')).toBeTruthy()
    expect(onChoose).not.toHaveBeenCalled()
  })

  it('goes back to the identifiers', () => {
    const { onBack } = setup()

    fireEvent.click(screen.getByText('verify.back'))

    expect(onBack).toHaveBeenCalled()
  })

  it('a mail method removed meanwhile is said by name, and the app is still there to choose', async () => {
    const { onChoose } = setup(vi.fn().mockRejectedValue(new MethodNotEnabledError()))

    fireEvent.click(card(/method\.mail_title/))

    expect(await screen.findByText('choose.error.mail_not_enabled')).toBeTruthy()
    fireEvent.click(card(/method\.app_title/))
    expect(onChoose).toHaveBeenCalledWith({ method: 'APP' })
  })
})
