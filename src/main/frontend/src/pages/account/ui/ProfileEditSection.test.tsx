import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { ProfileEditSection } from './ProfileEditSection'
import { ConflictError, InvalidCurrentPasswordError } from '../api/accountApi'
import { NetworkError, ServerError, RateLimitError } from '@/shared/lib'
import type { User } from '@/entities/user'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const BASE_USER: User = {
  id: '1', username: 'alice', email: 'alice@test.com',
  role: 'USER', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}

function setup(overrides: { onUpdateProfile?: (username: string, email: string, currentPassword?: string) => Promise<void>; onPatch?: (partial: Partial<User>) => void } = {}) {
  const onUpdateProfile = (overrides.onUpdateProfile ?? vi.fn().mockResolvedValue(undefined)) as (username: string, email: string, currentPassword?: string) => Promise<void>
  const onPatch = (overrides.onPatch ?? vi.fn()) as (partial: Partial<User>) => void
  const result = render(
    <ProfileEditSection user={BASE_USER} onPatch={onPatch} onUpdateProfile={onUpdateProfile} />
  )
  return { ...result, onUpdateProfile, onPatch }
}

describe('ProfileEditSection', () => {
  it('renders username and email fields pre-filled', () => {
    const { getByLabelText } = setup()
    expect((getByLabelText('profile.username') as HTMLInputElement).value).toBe('alice')
    expect((getByLabelText('profile.email') as HTMLInputElement).value).toBe('alice@test.com')
  })

  it('save button is disabled when fields are unchanged', () => {
    const { getByRole } = setup()
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('save button is enabled after editing username', () => {
    const { getByLabelText, getByRole } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'bob' } })
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(false)
  })

  it('calls onUpdateProfile with trimmed values and onPatch on success', async () => {
    const { getByLabelText, getByRole, onUpdateProfile, onPatch } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: '  bob  ' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))
    await waitFor(() => expect(onUpdateProfile).toHaveBeenCalledWith('bob', 'alice@test.com', undefined))
    expect(onPatch).toHaveBeenCalledWith({ username: 'bob', email: 'alice@test.com' })
  })

  it('shows success flash on update', async () => {
    const { getByLabelText, getByRole, findByRole } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'bob' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))
    const alert = await findByRole('status')
    expect(alert.textContent).toContain('profile.success')
  })

  it('shows conflict error on ConflictError', async () => {
    const { getByLabelText, getByRole, findByRole } = setup({
      onUpdateProfile: vi.fn().mockRejectedValue(new ConflictError()),
    })
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'taken' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('profile.error.conflict')
  })

  it('cancel resets fields to original values', () => {
    const { getByLabelText, getByRole } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'bob' } })
    fireEvent.click(getByRole('button', { name: 'profile.cancel' }))
    expect((getByLabelText('profile.username') as HTMLInputElement).value).toBe('alice')
  })

  it('save button is disabled and shows error when username is too short', () => {
    const { getByLabelText, getByRole, getByText } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'ab' } })
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(true)
    expect(getByText('profile.error.username_too_short')).toBeDefined()
  })

  it('save button is disabled and shows error when the username holds an @', () => {
    const { getByLabelText, getByRole, getByText } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'alice@home' } })
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(true)
    expect(getByText('profile.error.username_at')).toBeDefined()
  })

  it('save button is disabled and shows error when username is too long', () => {
    const { getByLabelText, getByRole, getByText } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'a'.repeat(51) } })
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(true)
    expect(getByText('profile.error.username_too_long')).toBeDefined()
  })

  it('shows network error on NetworkError', async () => {
    const { getByLabelText, getByRole, findByRole } = setup({
      onUpdateProfile: vi.fn().mockRejectedValue(new NetworkError()),
    })
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'bob' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('profile.error.network')
  })

  it('shows server error on ServerError', async () => {
    const { getByLabelText, getByRole, findByRole } = setup({
      onUpdateProfile: vi.fn().mockRejectedValue(new ServerError()),
    })
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'bob' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('profile.error.server')
  })

  it('shows rate_limit error on RateLimitError', async () => {
    const { getByLabelText, getByRole, findByRole } = setup({
      onUpdateProfile: vi.fn().mockRejectedValue(new RateLimitError()),
    })
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'bob' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('profile.error.rate_limit')
  })

  it('disables save when email is cleared', () => {
    const { getByLabelText, getByRole } = setup()
    fireEvent.change(getByLabelText('profile.email'), { target: { value: '' } })
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('asks for the current password as soon as the address changes', () => {
    const { getByLabelText, queryByLabelText, getByRole } = setup()
    expect(queryByLabelText('profile.current_password')).toBeNull()

    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })

    expect(getByLabelText('profile.current_password')).not.toBeNull()
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(true)
    fireEvent.change(getByLabelText('profile.current_password'), { target: { value: 'secret' } })
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(false)
  })

  it('sends the password with the new address', async () => {
    const { getByLabelText, getByRole, onUpdateProfile } = setup()
    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })
    fireEvent.change(getByLabelText('profile.current_password'), { target: { value: 'secret' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))

    await waitFor(() => expect(onUpdateProfile).toHaveBeenCalledWith('alice', 'new@test.com', 'secret'))
  })

  it('a change of letter case only asks for no password', () => {
    const { getByLabelText, queryByLabelText, getByRole } = setup()
    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'ALICE@test.com' } })

    expect(queryByLabelText('profile.current_password')).toBeNull()
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(false)
  })

  it('says so when the password is wrong, and empties it', async () => {
    const { getByLabelText, getByRole, findByRole } = setup({
      onUpdateProfile: vi.fn().mockRejectedValue(new InvalidCurrentPasswordError()),
    })
    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })
    fireEvent.change(getByLabelText('profile.current_password'), { target: { value: 'wrong' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))

    expect((await findByRole('alert')).textContent).toContain('profile.error.wrong_password')
    expect((getByLabelText('profile.current_password') as HTMLInputElement).value).toBe('')
  })

  it('forgets the typed password once the address goes back to the one it had', () => {
    const { getByLabelText } = setup()
    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })
    fireEvent.change(getByLabelText('profile.current_password'), { target: { value: 'secret' } })

    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'alice@test.com' } })
    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'other@test.com' } })

    expect((getByLabelText('profile.current_password') as HTMLInputElement).value).toBe('')
  })

  it('ties the reason for the password to its field', () => {
    const { getByLabelText, getByText } = setup()
    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })

    const hint = getByText('profile.current_password_hint')
    expect(hint.id).not.toBe('')
    expect(getByLabelText('profile.current_password').getAttribute('aria-describedby')).toBe(hint.id)
  })
})
