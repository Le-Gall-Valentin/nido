import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MailTestFailedError, SettingsInvalidError } from '@/shared/lib'
import { renderWithQuery } from '@/shared/test'
import type { ISettingsApi } from '../model/ISettingsApi'
import type { InstanceSettings } from '../model/types'
import { AdminSettingsPage } from './AdminSettingsPage'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: object) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

const SETTINGS: InstanceSettings = {
  groups: [
    { group: 'mail', fields: [
      { key: 'mail.host', value: 'smtp.example.com', source: 'DATABASE', variable: 'NIDO_SMTP_HOST', secret: false, required: false, set: true },
      { key: 'mail.port', value: '587', source: 'DEFAULT', variable: 'NIDO_SMTP_PORT', secret: false, required: false, set: true },
      { key: 'mail.security', value: 'starttls', source: 'DEFAULT', variable: 'NIDO_SMTP_SECURITY', secret: false, required: false, set: true },
      { key: 'mail.username', value: 'jane', source: 'DATABASE', variable: 'NIDO_SMTP_USERNAME', secret: false, required: false, set: true },
      { key: 'mail.password', value: null, source: 'DATABASE', variable: 'NIDO_SMTP_PASSWORD', secret: true, required: false, set: true },
      { key: 'mail.from', value: 'nido@example.com', source: 'DATABASE', variable: 'NIDO_MAIL_FROM', secret: false, required: false, set: true },
    ] },
    { group: 'public-url', fields: [
      { key: 'public-url', value: 'https://nido.example.com', source: 'ENVIRONMENT', variable: 'NIDO_APP_URL', secret: false, required: true, set: true },
    ] },
    { group: 'sessions', fields: [
      { key: 'sessions.access-token-minutes', value: '15', source: 'DEFAULT', variable: 'NIDO_JWT_EXPIRY_MINUTES', secret: false, required: false, set: true },
      { key: 'sessions.refresh-token-days', value: '30', source: 'DEFAULT', variable: 'NIDO_REFRESH_TOKEN_EXPIRY_DAYS', secret: false, required: false, set: true },
    ] },
    { group: 'api', fields: [
      { key: 'api.swagger', value: 'false', source: 'DEFAULT', variable: 'SWAGGER_ENABLED', secret: false, required: false, set: true },
    ] },
  ],
}

function open(overrides: Partial<ISettingsApi> = {}) {
  const api: ISettingsApi = {
    get: vi.fn().mockResolvedValue(SETTINGS),
    update: vi.fn().mockResolvedValue(SETTINGS),
    reset: vi.fn().mockResolvedValue(SETTINGS),
    testMail: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }
  renderWithQuery(<AdminSettingsPage api={api} />)
  return api
}

const card = (group: string) => within(screen.getByRole('region', { name: `group.${group}.title` }))

describe('AdminSettingsPage', () => {
  it('shows where each value comes from and locks what the environment sets', async () => {
    open()
    await screen.findByRole('region', { name: 'group.public-url.title' })

    expect((card('public-url').getByLabelText('field.public-url') as HTMLInputElement).disabled).toBe(true)
    expect(card('public-url').getByText('source.ENVIRONMENT')).not.toBeNull()
    expect(card('public-url').queryByRole('button', { name: 'action.save' })).toBeNull()
    expect(card('sessions').getAllByText('source.DEFAULT')).toHaveLength(2)
  })

  it('never shows the saved password and keeps it when the field is left empty', async () => {
    const api = open()
    await screen.findByRole('region', { name: 'group.mail.title' })

    expect((card('mail').getByLabelText('field.mail.password') as HTMLInputElement).value).toBe('')
    fireEvent.click(card('mail').getByRole('button', { name: 'action.save' }))

    await waitFor(() => expect(api.update).toHaveBeenCalled())
    expect(vi.mocked(api.update).mock.calls[0][0]).toBe('mail')
    expect(vi.mocked(api.update).mock.calls[0][1]).not.toHaveProperty('mail.password')
    expect(vi.mocked(api.update).mock.calls[0][1]).toMatchObject({ 'mail.host': 'smtp.example.com' })
  })

  it('saves a block and says so', async () => {
    const api = open()
    await screen.findByRole('region', { name: 'group.sessions.title' })

    fireEvent.change(card('sessions').getByLabelText('field.sessions.access-token-minutes'), { target: { value: '30' } })
    fireEvent.click(card('sessions').getByRole('button', { name: 'action.save' }))

    await waitFor(() => expect(api.update).toHaveBeenCalledWith('sessions', {
      'sessions.access-token-minutes': '30',
      'sessions.refresh-token-days': '30',
    }))
    expect(await card('sessions').findByText('saved')).not.toBeNull()
  })

  it('keeps what is being typed in a block when another block is saved', async () => {
    const saved = structuredClone(SETTINGS)
    saved.groups[3].fields[0] = { ...saved.groups[3].fields[0], value: 'true', source: 'DATABASE' }
    open({ update: vi.fn().mockResolvedValue(saved) })
    await screen.findByRole('region', { name: 'group.sessions.title' })

    fireEvent.change(card('sessions').getByLabelText('field.sessions.access-token-minutes'), { target: { value: '45' } })
    fireEvent.click(card('api').getByRole('switch'))
    fireEvent.click(card('api').getByRole('button', { name: 'action.save' }))

    expect(await card('api').findByText('saved')).not.toBeNull()
    expect((card('sessions').getByLabelText('field.sessions.access-token-minutes') as HTMLInputElement).value).toBe('45')
  })

  it('offers no way to empty the public address, saved from this page or not', async () => {
    const saved = structuredClone(SETTINGS)
    saved.groups[1].fields[0] = { ...saved.groups[1].fields[0], source: 'DATABASE' }
    open({ get: vi.fn().mockResolvedValue(saved) })
    await screen.findByRole('region', { name: 'group.public-url.title' })

    expect(card('public-url').queryByRole('button', { name: 'action.reset' })).toBeNull()
    expect(card('mail').getAllByRole('button', { name: 'action.reset' }).length).toBeGreaterThan(0)
  })

  it('saves the protection chosen in the list, and the switch turned on', async () => {
    const api = open()
    await screen.findByRole('region', { name: 'group.mail.title' })

    fireEvent.change(card('mail').getByLabelText('field.mail.security'), { target: { value: 'tls' } })
    fireEvent.click(card('mail').getByRole('button', { name: 'action.save' }))
    fireEvent.click(card('api').getByRole('switch'))
    fireEvent.click(card('api').getByRole('button', { name: 'action.save' }))

    await waitFor(() => expect(api.update).toHaveBeenCalledWith('mail', expect.objectContaining({ 'mail.security': 'tls' })))
    await waitFor(() => expect(api.update).toHaveBeenCalledWith('api', { 'api.swagger': 'true' }))
  })

  it('shows the problem under its field', async () => {
    open({ update: vi.fn().mockRejectedValue(new SettingsInvalidError({ 'sessions.access-token-minutes': 'out_of_range' })) })
    await screen.findByRole('region', { name: 'group.sessions.title' })

    fireEvent.click(card('sessions').getByRole('button', { name: 'action.save' }))

    expect(await card('sessions').findByText('common:setting_problem.out_of_range')).not.toBeNull()
  })

  it('sends a test with the form and reports what the server answered', async () => {
    const api = open({ testMail: vi.fn().mockRejectedValue(new MailTestFailedError('authentication_failed', '535 Authentication failed')) })
    await screen.findByRole('region', { name: 'group.mail.title' })

    fireEvent.click(card('mail').getByRole('button', { name: 'action.test' }))

    expect(await card('mail').findByText('common:mail_failure.authentication_failed — 535 Authentication failed')).not.toBeNull()
    expect(api.testMail).toHaveBeenCalledWith(expect.objectContaining({ 'mail.host': 'smtp.example.com' }))
  })

  it('tests the mail the environment sets, though nothing in it can be changed here', async () => {
    const locked: InstanceSettings = {
      groups: SETTINGS.groups.map((group) => group.group !== 'mail' ? group : {
        ...group,
        fields: group.fields.map((field) => ({ ...field, source: 'ENVIRONMENT' as const })),
      }),
    }
    const api = open({ get: vi.fn().mockResolvedValue(locked) })
    await screen.findByRole('region', { name: 'group.mail.title' })

    expect(card('mail').queryByRole('button', { name: 'action.save' })).toBeNull()
    fireEvent.click(card('mail').getByRole('button', { name: 'action.test' }))

    await waitFor(() => expect(api.testMail).toHaveBeenCalledWith({}))
    expect(await card('mail').findByText('mail.test_sent')).not.toBeNull()
  })

  it('keeps the password in the form of its block, where Enter saves the block', async () => {
    const api = open()
    await screen.findByRole('region', { name: 'group.mail.title' })

    const form = card('mail').getByLabelText('field.mail.password').closest('form')
    expect(form).not.toBeNull()
    fireEvent.submit(form!)

    await waitFor(() => expect(api.update).toHaveBeenCalledWith('mail', expect.objectContaining({ 'mail.host': 'smtp.example.com' })))
  })

  it('keeps password managers from filling the administrator\'s own sign-in into the mail server fields', async () => {
    open()
    await screen.findByRole('region', { name: 'group.mail.title' })

    expect(card('mail').getByLabelText('field.mail.username').getAttribute('autocomplete')).toBe('off')
    expect(card('mail').getByLabelText('field.mail.host').getAttribute('autocomplete')).toBe('off')
    expect(card('mail').getByLabelText('field.mail.password').getAttribute('autocomplete')).toBe('new-password')
  })

  it('clears the saved password on request', async () => {
    const api = open()
    await screen.findByRole('region', { name: 'group.mail.title' })

    fireEvent.click(card('mail').getByRole('button', { name: 'secret.clear' }))

    await waitFor(() => expect(api.reset).toHaveBeenCalledWith('mail', 'mail.password'))
  })

  it('turns mail off by clearing its server', async () => {
    const api = open()
    await screen.findByRole('region', { name: 'group.mail.title' })

    fireEvent.click(card('mail').getByRole('button', { name: 'action.disable_mail' }))

    await waitFor(() => expect(api.reset).toHaveBeenCalledWith('mail', 'mail.host'))
  })
})
