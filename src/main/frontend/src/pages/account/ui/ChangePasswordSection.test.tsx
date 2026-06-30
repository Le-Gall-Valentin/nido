import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { ChangePasswordSection } from './ChangePasswordSection'
import { InvalidCurrentPasswordError } from '../api/accountApi'
import { NetworkError, ServerError, RateLimitError } from '@/shared/lib'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

function setup(onChangePassword = vi.fn().mockResolvedValue(undefined)) {
  return { ...render(<ChangePasswordSection onChangePassword={onChangePassword} />), onChangePassword }
}

function fillForm(container: HTMLElement, current: string, next: string, confirm: string) {
  fireEvent.change(container.querySelector('input[name="currentPassword"]')!, { target: { value: current } })
  fireEvent.change(container.querySelector('input[name="newPassword"]')!, { target: { value: next } })
  fireEvent.change(container.querySelector('input[name="confirmPassword"]')!, { target: { value: confirm } })
}

describe('ChangePasswordSection', () => {
  it('submit button disabled when fields are empty', () => {
    const { getByRole } = setup()
    expect((getByRole('button', { name: 'password.submit' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('submit button disabled when passwords do not match', () => {
    const { container, getByRole } = setup()
    fillForm(container, 'old', 'New1!pass', 'different')
    expect((getByRole('button', { name: 'password.submit' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('shows mismatch error when passwords differ', () => {
    const { container, getByText } = setup()
    fillForm(container, 'old', 'New1!pass', 'different')
    expect(getByText('password.error.mismatch')).toBeDefined()
  })

  it('shows too_short warning when new password is under 8 chars', () => {
    const { container, getByText } = setup()
    fillForm(container, 'old', 'Ab1!', 'Ab1!')
    expect(getByText('password.error.too_short')).toBeDefined()
  })

  it('calls onChangePassword with currentPassword and newPassword on submit', async () => {
    const { container, getByRole, onChangePassword } = setup()
    fillForm(container, 'oldpass', 'Newpass1!', 'Newpass1!')
    fireEvent.click(getByRole('button', { name: 'password.submit' }))
    await waitFor(() => expect(onChangePassword).toHaveBeenCalledWith('oldpass', 'Newpass1!'))
  })

  it('shows success flash and clears fields on success', async () => {
    const { container, getByRole, findByRole } = setup()
    fillForm(container, 'oldpass', 'Newpass1!', 'Newpass1!')
    fireEvent.click(getByRole('button', { name: 'password.submit' }))
    await findByRole('status')
    expect((container.querySelector('input[name="currentPassword"]') as HTMLInputElement).value).toBe('')
  })

  it('shows wrong_current error on InvalidCurrentPasswordError', async () => {
    const { container, getByRole, findByRole } = setup(vi.fn().mockRejectedValue(new InvalidCurrentPasswordError()))
    fillForm(container, 'wrong', 'Newpass1!', 'Newpass1!')
    fireEvent.click(getByRole('button', { name: 'password.submit' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('password.error.wrong_current')
  })

  it('shows network error on NetworkError', async () => {
    const { container, getByRole, findByRole } = setup(vi.fn().mockRejectedValue(new NetworkError()))
    fillForm(container, 'old', 'Newpass1!', 'Newpass1!')
    fireEvent.click(getByRole('button', { name: 'password.submit' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('password.error.network')
  })

  it('shows server error on ServerError', async () => {
    const { container, getByRole, findByRole } = setup(vi.fn().mockRejectedValue(new ServerError()))
    fillForm(container, 'old', 'Newpass1!', 'Newpass1!')
    fireEvent.click(getByRole('button', { name: 'password.submit' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('password.error.server')
  })

  it('shows rate_limit error on RateLimitError', async () => {
    const { container, getByRole, findByRole } = setup(vi.fn().mockRejectedValue(new RateLimitError()))
    fillForm(container, 'old', 'Newpass1!', 'Newpass1!')
    fireEvent.click(getByRole('button', { name: 'password.submit' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('password.error.rate_limit')
  })
})