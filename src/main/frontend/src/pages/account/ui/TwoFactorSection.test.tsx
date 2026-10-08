import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it, vi } from 'vitest'
import { createTestQueryClient } from '@/shared/test'
import type { User } from '@/entities/user'
import type { ITwoFactorMethodsApi, MethodState } from '@/features/two-factor'
import {
  MethodAlreadyEnabledError, MethodNotEnabledError, MethodUnavailableError, ResendTooSoonError, SendLimitError,
} from '@/features/two-factor'
import { NetworkError, RateLimitError } from '@/shared/lib'
import { TwoFactorSection } from './TwoFactorSection'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: Record<string, unknown>) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

vi.mock('@/features/two-factor', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/features/two-factor')>()),
  AppSetupFlow: ({ onSuccess }: { onSuccess: () => void }) => <button onClick={onSuccess}>app-setup-success</button>,
  MailSetupStep: ({ sentTo, onSuccess }: { sentTo: string; onSuccess: () => void }) => (
    <div><span>{`mail-setup-${sentTo}`}</span><button onClick={onSuccess}>mail-setup-success</button></div>
  ),
}))

vi.mock('./DisableMethodDialog', () => ({
  DisableMethodDialog: ({ method, paused, resendAfterSeconds, onSuccess, onStale }: {
    method: string; paused: boolean; resendAfterSeconds: number; onSuccess: () => void; onStale: () => void
  }) => (
    <div>
      <span>{`disable-${method}-${paused}-${resendAfterSeconds}`}</span>
      <button onClick={onSuccess}>disable-success</button>
      <button onClick={onStale}>disable-stale</button>
    </div>
  ),
}))

const USER: User = { id: '1', username: 'camille', email: 'camille@exemple.fr', role: 'USER', createdAt: '2026-01-01T00:00:00Z', twoFactorMethods: ['APP'] }

function state(method: 'APP' | 'MAIL', enabled: boolean, usable: boolean): MethodState {
  return { method, enabled, usable }
}

function setup(methods: MethodState[], overrides: Partial<ITwoFactorMethodsApi> = {}, user: User = USER) {
  const api: ITwoFactorMethodsApi = {
    list: vi.fn().mockResolvedValue(methods), setupApp: vi.fn(), setupMail: vi.fn(), confirm: vi.fn(),
    sendDisableCode: vi.fn(), disable: vi.fn(), ...overrides,
  }
  const onPatch = vi.fn()
  render(
    <QueryClientProvider client={createTestQueryClient()}>
      <TwoFactorSection user={user} onPatch={onPatch} api={api} />
    </QueryClientProvider>,
  )
  return { api, onPatch }
}

describe('TwoFactorSection', () => {
  it('lists both methods and sums them up in the title', async () => {
    setup([state('APP', true, true), state('MAIL', false, true)])

    expect(await screen.findByText('twofa.app_title')).toBeTruthy()
    expect(screen.getByText('twofa.mail_title')).toBeTruthy()
    expect(screen.getAllByText('twofa.status_enabled').length).toBe(2)
  })

  it('the title says paused when every method on is paused', async () => {
    setup([state('APP', false, true), state('MAIL', true, false)])

    await screen.findByText('twofa.mail_paused')
    expect(screen.getAllByText('twofa.status_paused').length).toBe(2)
  })

  it('turning the app on opens its setup, and success records it', async () => {
    const { onPatch } = setup([state('APP', false, true), state('MAIL', true, true)], {}, { ...USER, twoFactorMethods: ['MAIL'] })

    fireEvent.click(await screen.findByRole('button', { name: 'twofa.btn_enable' }))
    fireEvent.click(await screen.findByText('app-setup-success'))

    expect(onPatch).toHaveBeenCalledWith({ twoFactorMethods: ['APP', 'MAIL'] })
    expect(await screen.findByText('twofa.success_enabled_app')).toBeTruthy()
  })

  it('turning the mail on sends the code on the click, then asks for it', async () => {
    const setupMail = vi.fn().mockResolvedValue({ sentTo: 'camille@exemple.fr', resendAfterSeconds: 60 })
    const { onPatch } = setup([state('APP', true, true), state('MAIL', false, true)], { setupMail })

    fireEvent.click(await screen.findByRole('button', { name: 'twofa.btn_enable' }))
    expect(await screen.findByText('mail-setup-camille@exemple.fr')).toBeTruthy()
    fireEvent.click(screen.getByText('mail-setup-success'))

    expect(setupMail).toHaveBeenCalledOnce()
    expect(onPatch).toHaveBeenCalledWith({ twoFactorMethods: ['APP', 'MAIL'] })
  })

  it('mail switched off since the page loaded is said, not a generic error', async () => {
    setup([state('APP', true, true), state('MAIL', false, true)], { setupMail: vi.fn().mockRejectedValue(new MethodUnavailableError()) })

    fireEvent.click(await screen.findByRole('button', { name: 'twofa.btn_enable' }))

    expect(await screen.findByText('twofa.error.mail_unavailable')).toBeTruthy()
  })

  it('turning the mail off sends its code on the click, then opens the dialog', async () => {
    const sendDisableCode = vi.fn().mockResolvedValue({ resendAfterSeconds: 60 })
    const { onPatch } = setup([state('APP', false, true), state('MAIL', true, true)], { sendDisableCode }, { ...USER, twoFactorMethods: ['MAIL'] })

    fireEvent.click(await screen.findByRole('button', { name: 'twofa.btn_disable' }))
    expect(await screen.findByText('disable-MAIL-false-60')).toBeTruthy()
    fireEvent.click(screen.getByText('disable-success'))

    expect(onPatch).toHaveBeenCalledWith({ twoFactorMethods: [] })
  })

  it('a paused mail opens the dialog without sending anything', async () => {
    const sendDisableCode = vi.fn()
    setup([state('APP', false, true), state('MAIL', true, false)], { sendDisableCode }, { ...USER, twoFactorMethods: ['MAIL'] })

    fireEvent.click(await screen.findByRole('button', { name: 'twofa.btn_disable' }))

    expect(await screen.findByText('disable-MAIL-true-0')).toBeTruthy()
    expect(sendDisableCode).not.toHaveBeenCalled()
  })

  it('the app is turned off from its own dialog, no code sent', async () => {
    setup([state('APP', true, true), state('MAIL', false, true)])

    fireEvent.click(await screen.findByRole('button', { name: 'twofa.btn_disable' }))

    await waitFor(() => expect(screen.getByText('disable-APP-false-0')).toBeTruthy())
  })

  it('a method turned on meanwhile, in another tab, is said and the list reloaded', async () => {
    const setupMail = vi.fn().mockRejectedValue(new MethodAlreadyEnabledError())
    const { api } = setup([state('APP', true, true), state('MAIL', false, true)], { setupMail })

    fireEvent.click(await screen.findByRole('button', { name: 'twofa.btn_enable' }))

    expect(await screen.findByText('twofa.error.changed')).toBeTruthy()
    await waitFor(() => expect(api.list).toHaveBeenCalledTimes(2))
  })

  it('a method turned off meanwhile is said and the list reloaded', async () => {
    const sendDisableCode = vi.fn().mockRejectedValue(new MethodNotEnabledError())
    const { api } = setup([state('APP', false, true), state('MAIL', true, true)], { sendDisableCode }, { ...USER, twoFactorMethods: ['MAIL'] })

    fireEvent.click(await screen.findByRole('button', { name: 'twofa.btn_disable' }))

    expect(await screen.findByText('twofa.error.changed')).toBeTruthy()
    await waitFor(() => expect(api.list).toHaveBeenCalledTimes(2))
  })

  it('a paused mail that came back while its dialog was open closes it, says so and reloads the list', async () => {
    const { api } = setup([state('APP', false, true), state('MAIL', true, false)], {}, { ...USER, twoFactorMethods: ['MAIL'] })

    fireEvent.click(await screen.findByRole('button', { name: 'twofa.btn_disable' }))
    fireEvent.click(await screen.findByText('disable-stale'))

    expect(await screen.findByText('twofa.error.changed')).toBeTruthy()
    expect(screen.queryByText('disable-stale')).toBeNull()
    await waitFor(() => expect(api.list).toHaveBeenCalledTimes(2))
  })

  it('a paused mail next to the app on says the app still protects the account', async () => {
    setup([state('APP', true, true), state('MAIL', true, false)], {}, { ...USER, twoFactorMethods: ['APP', 'MAIL'] })

    expect(await screen.findByText('twofa.mail_paused_app_on')).toBeTruthy()
  })

  it('turning the mail on again within the minute opens the code already sent, with the time left', async () => {
    setup([state('APP', true, true), state('MAIL', false, true)], { setupMail: vi.fn().mockRejectedValue(new ResendTooSoonError(25)) })

    fireEvent.click(await screen.findByRole('button', { name: 'twofa.btn_enable' }))

    expect(await screen.findByText('mail-setup-camille@exemple.fr')).toBeTruthy()
  })

  it('turning the mail off again within the minute opens the dialog on the code already sent', async () => {
    setup([state('APP', false, true), state('MAIL', true, true)],
      { sendDisableCode: vi.fn().mockRejectedValue(new ResendTooSoonError(25)) }, { ...USER, twoFactorMethods: ['MAIL'] })

    fireEvent.click(await screen.findByRole('button', { name: 'twofa.btn_disable' }))

    expect(await screen.findByText('disable-MAIL-false-25')).toBeTruthy()
  })

  it('the account limit, the route limit and a lost network are said', async () => {
    const setupMail = vi.fn()
      .mockRejectedValueOnce(new SendLimitError(540))
      .mockRejectedValueOnce(new RateLimitError(20))
      .mockRejectedValueOnce(new NetworkError())
    setup([state('APP', true, true), state('MAIL', false, true)], { setupMail })
    const enable = async () => fireEvent.click(await screen.findByRole('button', { name: 'twofa.btn_enable' }))

    await enable(); expect(await screen.findByText('twoFactor:error.send_limit:{"minutes":9}')).toBeTruthy()
    await enable(); expect(await screen.findByText('twoFactor:error.rate_limit_timed:{"seconds":20}')).toBeTruthy()
    await enable(); expect(await screen.findByText('twoFactor:error.network')).toBeTruthy()
  })

  it('the buttons wait while a code is on its way', async () => {
    setup([state('APP', true, true), state('MAIL', false, true)], { setupMail: vi.fn(() => new Promise<never>(() => {})) })

    fireEvent.click(await screen.findByRole('button', { name: 'twofa.btn_enable' }))

    await waitFor(() => expect((screen.getByRole('button', { name: 'twofa.btn_enable' }) as HTMLButtonElement).disabled).toBe(true))
    expect((screen.getByRole('button', { name: 'twofa.btn_disable' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('methods that cannot be loaded say so', async () => {
    setup([], { list: vi.fn().mockRejectedValue(new NetworkError()) })

    expect(await screen.findByText('twofa.error.load')).toBeTruthy()
  })

  it('a method turned off from its dialog is recorded and said', async () => {
    const { onPatch } = setup([state('APP', true, true), state('MAIL', true, true)], {}, { ...USER, twoFactorMethods: ['APP', 'MAIL'] })

    fireEvent.click((await screen.findAllByRole('button', { name: 'twofa.btn_disable' }))[0])
    fireEvent.click(await screen.findByText('disable-success'))

    expect(onPatch).toHaveBeenCalledWith({ twoFactorMethods: ['MAIL'] })
    expect(await screen.findByText('twofa.success_disabled')).toBeTruthy()
  })
})
