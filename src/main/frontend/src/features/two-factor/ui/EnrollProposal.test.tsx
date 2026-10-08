import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { EnrollProposal } from './EnrollProposal'
import { MethodUnavailableError, ResendTooSoonError, SendLimitError } from '../model/errors'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: Record<string, unknown>) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

function setup(mailAvailable: boolean, setupMail = vi.fn()) {
  const handlers = { onAppChosen: vi.fn(), onMailStarted: vi.fn(), onSkip: vi.fn() }
  render(<EnrollProposal username="camille" email="camille@exemple.fr" mailAvailable={mailAvailable}
    api={{ setupMail }} {...handlers} />)
  return handlers
}

describe('EnrollProposal', () => {
  it('without mail, is the screen it always was: the app only', () => {
    const { onAppChosen } = setup(false)

    expect(screen.getByText('enroll.why_title')).toBeTruthy()
    expect(screen.queryByText('method.mail_title')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: /enroll\.activate/ }))
    expect(onAppChosen).toHaveBeenCalled()
  })

  it('with mail, offers both, the app tagged as the most secure', () => {
    setup(true)

    expect(screen.getByText('enroll.question')).toBeTruthy()
    expect(screen.getByRole('button', { name: /method\.app_title/ }).textContent).toContain('method.app_tag')
    expect(screen.getByText('method.mail_enroll:{"address":"camille@exemple.fr"}')).toBeTruthy()
  })

  it('the mail card sends the code on the click and moves on', async () => {
    const { onMailStarted } = setup(true, vi.fn().mockResolvedValue({ sentTo: 'camille@exemple.fr', resendAfterSeconds: 60 }))

    fireEvent.click(screen.getByRole('button', { name: /method\.mail_title/ }))

    await waitFor(() => expect(onMailStarted).toHaveBeenCalledWith({ sentTo: 'camille@exemple.fr', resendAfterSeconds: 60 }))
  })

  it('a code sent moments ago is the one to type', async () => {
    const { onMailStarted } = setup(true, vi.fn().mockRejectedValue(new ResendTooSoonError(40)))

    fireEvent.click(screen.getByRole('button', { name: /method\.mail_title/ }))

    await waitFor(() => expect(onMailStarted).toHaveBeenCalledWith({ sentTo: 'camille@exemple.fr', resendAfterSeconds: 40 }))
  })

  it('a refusal is said on the proposal', async () => {
    const setupMail = vi.fn().mockRejectedValueOnce(new MethodUnavailableError()).mockRejectedValueOnce(new SendLimitError(300))
    const { onMailStarted } = setup(true, setupMail)

    fireEvent.click(screen.getByRole('button', { name: /method\.mail_title/ }))
    expect(await screen.findByText('enroll.error.mail_unavailable')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /method\.mail_title/ }))
    expect(await screen.findByText('enroll.error.send_limit:{"minutes":5}')).toBeTruthy()
    expect(onMailStarted).not.toHaveBeenCalled()
  })

  it('can be skipped', () => {
    const { onSkip } = setup(true)

    fireEvent.click(screen.getByText('enroll.skip'))

    expect(onSkip).toHaveBeenCalled()
  })
})
