import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MailTestFailedError, SettingsInvalidError } from '@/shared/lib'
import type { ISetupApi } from '../model/ISetupApi'
import type { SetupStatus } from '../model/types'
import { SetupAlreadyDoneError, SetupCodeInvalidError } from '../model/errors'
import { SetupPage } from './SetupPage'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: object) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))
vi.mock('@/shared/lib', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/shared/lib')>()),
  useLanguage: () => ({ language: 'fr', setLanguage: vi.fn() }),
}))

const OPEN: SetupStatus = { required: true, lockedPublicUrl: null, mailLocked: false }

function fakeApi(overrides: Partial<ISetupApi> = {}): ISetupApi {
  return {
    status: vi.fn(),
    verifyCode: vi.fn().mockResolvedValue(undefined),
    encryptionKey: vi.fn().mockResolvedValue({ source: 'GENERATED', key: 'the-generated-key' }),
    testMail: vi.fn().mockResolvedValue(undefined),
    complete: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }
}

function open(api = fakeApi(), status = OPEN) {
  const signIn = vi.fn().mockResolvedValue(true)
  const goTo = vi.fn()
  render(<SetupPage status={status} api={api} signIn={signIn} goTo={goTo} />)
  return { api, signIn, goTo }
}

const type = (label: string, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } })
const click = (name: string) => fireEvent.click(screen.getByRole('button', { name }))

async function passTheCode() {
  type('code.field', ' k7qm-3xrp-w9td ')
  click('action.next')
  await screen.findByLabelText('admin.username')
}

async function passTheAdmin() {
  type('admin.username', 'jane')
  type('admin.email', 'jane@example.fr')
  type('admin.password', 'Str0ng!Password')
  type('admin.confirm', 'Str0ng!Password')
  click('action.next')
  await screen.findByLabelText('address.field')
}

describe('SetupPage', () => {
  it('goes through the steps, sends everything at once, then signs in on this address', async () => {
    const { api, signIn, goTo } = open()

    await passTheCode()
    await passTheAdmin()
    expect((screen.getByLabelText('address.field') as HTMLInputElement).value).toBe(window.location.origin)
    click('action.next')
    await screen.findByRole('button', { name: 'action.later' })
    click('action.later')
    expect(await screen.findByText('the-generated-key')).not.toBeNull()
    expect((screen.getByRole('button', { name: 'action.finish' }) as HTMLButtonElement).disabled).toBe(true)
    fireEvent.click(screen.getByLabelText('key.saved'))
    click('action.finish')

    await waitFor(() => expect(goTo).toHaveBeenCalledWith('/'))
    expect(api.verifyCode).toHaveBeenCalledWith('k7qm-3xrp-w9td')
    expect(api.complete).toHaveBeenCalledWith({
      code: 'k7qm-3xrp-w9td',
      admin: { username: 'jane', email: 'jane@example.fr', password: 'Str0ng!Password', language: 'fr' },
      publicUrl: window.location.origin,
      mail: null,
      encryptionKeySaved: true,
    })
    expect(signIn).toHaveBeenCalledWith('jane', 'Str0ng!Password')
  })

  it('says when the code is not the one in the logs', async () => {
    open(fakeApi({ verifyCode: vi.fn().mockRejectedValue(new SetupCodeInvalidError()) }))

    type('code.field', 'AAAA-AAAA-AAAA')
    click('action.next')

    expect(await screen.findByText('errors.code_invalid')).not.toBeNull()
  })

  it('refuses an administrator the server would refuse, before sending anything', async () => {
    open()
    await passTheCode()

    type('admin.username', 'jane@home')
    type('admin.email', 'jane@example.fr')
    type('admin.password', 'weak')
    type('admin.confirm', 'other')
    click('action.next')

    expect(screen.getByText('admin.username_has_at')).not.toBeNull()
    expect(screen.getByText('admin.password_too_short')).not.toBeNull()
    expect(screen.getByText('admin.confirm_mismatch')).not.toBeNull()
  })

  it('opens the other address instead of signing in when it is not this one', async () => {
    const { signIn, goTo } = open(fakeApi({ encryptionKey: vi.fn().mockResolvedValue({ source: 'PROVIDED', key: null }) }))
    await passTheCode()
    await passTheAdmin()
    type('address.field', 'https://nido.example.com')
    click('action.next')
    click('action.later')
    expect(await screen.findByText('key.provided')).not.toBeNull()
    click('action.finish')

    expect(await screen.findByRole('link', { name: 'https://nido.example.com' })).not.toBeNull()
    expect(signIn).not.toHaveBeenCalled()
    expect(goTo).not.toHaveBeenCalled()
  })

  it('warns that plain http travels in clear', async () => {
    open()
    await passTheCode()
    await passTheAdmin()

    type('address.field', 'http://192.168.1.10:8080')

    expect(screen.getByText('address.http_warning')).not.toBeNull()
  })

  it('shows what the mail server answered to the test', async () => {
    open(fakeApi({ testMail: vi.fn().mockRejectedValue(new MailTestFailedError('authentication_failed', '535 Authentication failed')) }))
    await passTheCode()
    await passTheAdmin()
    click('action.next')
    type('mail.host', 'smtp.example.com')
    type('mail.from', 'Nido <nido@example.com>')

    click('action.test')

    expect(await screen.findByText('common:mail_failure.authentication_failed — 535 Authentication failed')).not.toBeNull()
  })

  it('keeps the mail password in a form, where Enter moves on like Continue', async () => {
    open()
    await passTheCode()
    await passTheAdmin()
    click('action.next')
    type('mail.host', 'smtp.example.com')

    const form = screen.getByLabelText('mail.password').closest('form')
    expect(form).not.toBeNull()
    fireEvent.submit(form!)

    expect(await screen.findByText('the-generated-key')).not.toBeNull()
  })

  it('keeps password managers from filling the new administrator into the mail server fields', async () => {
    open()
    await passTheCode()
    await passTheAdmin()
    click('action.next')

    for (const field of ['mail.host', 'mail.port', 'mail.username', 'mail.from']) {
      expect(screen.getByLabelText(field).getAttribute('autocomplete')).toBe('off')
    }
    expect(screen.getByLabelText('mail.password').getAttribute('autocomplete')).toBe('new-password')
  })

  it('says how to keep the key when it cannot be copied, as over plain http', async () => {
    open()
    await passTheCode()
    await passTheAdmin()
    click('action.next')
    click('action.later')
    expect(await screen.findByText('the-generated-key')).not.toBeNull()

    click('action.copy')

    expect(await screen.findByText('key.copy_failed')).not.toBeNull()
  })

  it('says the key is copied once it is', async () => {
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: vi.fn().mockResolvedValue(undefined) }, configurable: true })
    open()
    await passTheCode()
    await passTheAdmin()
    click('action.next')
    click('action.later')
    await screen.findByText('the-generated-key')

    click('action.copy')

    expect(await screen.findByRole('button', { name: 'action.copied' })).not.toBeNull()
    Object.defineProperty(navigator, 'clipboard', { value: undefined, configurable: true })
  })

  it('says why the key could not be loaded', async () => {
    open(fakeApi({ encryptionKey: vi.fn().mockRejectedValue(new SetupAlreadyDoneError()) }))
    await passTheCode()
    await passTheAdmin()
    click('action.next')
    click('action.later')

    expect(await screen.findByText('errors.already_done')).not.toBeNull()
  })

  it('refuses an address the server would refuse before going on', async () => {
    open()
    await passTheCode()
    await passTheAdmin()

    type('address.field', 'https://nido.example.com/nido')
    click('action.next')

    expect(screen.getByText('address.invalid')).not.toBeNull()
    expect(screen.queryByLabelText('mail.host')).toBeNull()
  })

  it('wants a server before going on with mail, and says where the test went', async () => {
    const { api } = open()
    await passTheCode()
    await passTheAdmin()
    click('action.next')

    click('action.next')
    expect(screen.getByText('common:setting_problem.required')).not.toBeNull()

    type('mail.host', 'smtp.example.com')
    click('action.test')
    expect(await screen.findByText('mail.test_sent:{"recipient":"jane@example.fr"}')).not.toBeNull()
    expect(api.testMail).toHaveBeenCalledWith(expect.objectContaining({ recipient: 'jane@example.fr', language: 'fr' }))
  })

  it('goes on without mail, or back, when the server configuration sets it', async () => {
    const { api } = open(fakeApi(), { ...OPEN, mailLocked: true })
    await passTheCode()
    await passTheAdmin()
    click('action.next')
    await screen.findByText('mail.locked')

    click('action.back')
    expect(await screen.findByLabelText('address.field')).not.toBeNull()
    click('action.next')
    await screen.findByText('mail.locked')
    click('action.next')
    fireEvent.click(await screen.findByLabelText('key.saved'))
    click('action.finish')

    await waitFor(() => expect(api.complete).toHaveBeenCalledWith(expect.objectContaining({ mail: null })))
  })

  it('asks nothing about mail when the server configuration sets it', async () => {
    open(fakeApi(), { ...OPEN, mailLocked: true })
    await passTheCode()
    await passTheAdmin()
    click('action.next')

    expect(await screen.findByText('mail.locked')).not.toBeNull()
    expect(screen.queryByLabelText('mail.host')).toBeNull()
  })

  it('goes back to the address when the server refuses it at the end', async () => {
    open(fakeApi({
      encryptionKey: vi.fn().mockResolvedValue({ source: 'PROVIDED', key: null }),
      complete: vi.fn().mockRejectedValue(new SettingsInvalidError({ 'public-url': 'invalid_url' })),
    }))
    await passTheCode()
    await passTheAdmin()
    click('action.next')
    click('action.later')
    await screen.findByText('key.provided')
    click('action.finish')

    expect(await screen.findByLabelText('address.field')).not.toBeNull()
    expect(screen.getByText('common:setting_problem.invalid_url')).not.toBeNull()
  })
})
