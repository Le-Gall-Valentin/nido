import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { DisableMethodDialog } from './DisableMethodDialog'
import { CodeError, MaxAttemptsError, SendLimitError, type ITwoFactorMethodsApi } from '@/features/two-factor'

type DisableApi = Pick<ITwoFactorMethodsApi, 'disable' | 'sendDisableCode'>

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: Record<string, unknown>) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

function fill(code: string) {
  fireEvent.change(document.querySelector('input[autocomplete="one-time-code"]')!, { target: { value: code } })
}

function open(method: 'APP' | 'MAIL', paused = false, api: Partial<DisableApi> = {}) {
  const handlers = { onClose: vi.fn(), onSuccess: vi.fn() }
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

    expect(await screen.findByText('disable.error.send_limit:{"minutes":5}')).toBeTruthy()
  })
})
