import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { InvalidLinkError, NetworkError, WeakPasswordError } from '@/shared/lib'
import type { IPasswordResetApi } from '../model/IPasswordResetApi'
import { NewPasswordForm } from './NewPasswordForm'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: object) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

function setup(confirmReset: IPasswordResetApi['confirmReset'], username?: string) {
  const onDone = vi.fn()
  const onInvalid = vi.fn()
  render(<NewPasswordForm onSave={(password) => confirmReset('abc', password)} username={username} onDone={onDone} onInvalid={onInvalid} />)
  const type = (label: string, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } })
  const save = () => screen.getByRole('button', { name: 'action.save' }) as HTMLButtonElement
  return { onDone, onInvalid, type, save }
}

describe('NewPasswordForm', () => {
  it('hands the username to password managers when it knows it, out of sight', () => {
    setup(vi.fn(), 'carol')

    const field = document.querySelector('input[autocomplete="username"]') as HTMLInputElement
    expect(field.value).toBe('carol')
    expect(field.hidden).toBe(true)
  })

  it('has no username field when it does not know one', () => {
    setup(vi.fn())

    expect(document.querySelector('input[autocomplete="username"]')).toBeNull()
  })

  it('can only be saved with a password the rules accept, typed twice', () => {
    const { type, save } = setup(vi.fn())

    expect(save().disabled).toBe(true)
    type('field.new_password', 'weak')
    type('field.confirm', 'weak')
    expect(save().disabled).toBe(true)

    type('field.new_password', 'NewPassw0rd!')
    type('field.confirm', 'NewPassw0rd?')
    expect(save().disabled).toBe(true)
    expect(screen.getByText('error.mismatch')).not.toBeNull()

    type('field.confirm', 'NewPassw0rd!')
    expect(save().disabled).toBe(false)
  })

  it('says why a password the rules text seems to allow is refused: past 72 bytes', () => {
    const { type, save } = setup(vi.fn())
    const long = 'Aé1!' + 'é'.repeat(68)
    type('field.new_password', long)
    type('field.confirm', long)

    expect(screen.getByText('error.too_long')).not.toBeNull()
    expect(save().disabled).toBe(true)
  })

  it('ties what is wrong to the field it is about, for a screen reader', () => {
    const { type } = setup(vi.fn())
    const long = 'Aé1!' + 'é'.repeat(68)
    type('field.new_password', long)
    type('field.confirm', 'something else')

    const describedBy = (label: string) => screen.getByLabelText(label).getAttribute('aria-describedby') ?? ''
    const tooLong = screen.getByText('error.too_long')
    const mismatch = screen.getByText('error.mismatch')
    expect(tooLong.id).not.toBe('')
    expect(mismatch.id).not.toBe('')
    expect(describedBy('field.new_password').split(' ')).toContain(tooLong.id)
    expect(describedBy('field.confirm').split(' ')).toContain(mismatch.id)
    expect(screen.getByLabelText('field.new_password').getAttribute('aria-invalid')).toBe('true')
  })

  it('saves the password with the link token, then says it is done', async () => {
    const confirmReset = vi.fn().mockResolvedValue(undefined)
    const { type, save, onDone } = setup(confirmReset)

    type('field.new_password', 'NewPassw0rd!')
    type('field.confirm', 'NewPassw0rd!')
    fireEvent.click(save())

    await waitFor(() => expect(onDone).toHaveBeenCalled())
    expect(confirmReset).toHaveBeenCalledWith('abc', 'NewPassw0rd!')
  })

  it('hands a link that stopped working back to the page', async () => {
    const { type, save, onInvalid, onDone } = setup(vi.fn().mockRejectedValue(new InvalidLinkError()))

    type('field.new_password', 'NewPassw0rd!')
    type('field.confirm', 'NewPassw0rd!')
    fireEvent.click(save())

    await waitFor(() => expect(onInvalid).toHaveBeenCalled())
    expect(onDone).not.toHaveBeenCalled()
  })

  it('shows what went wrong otherwise', async () => {
    const { type, save } = setup(vi.fn().mockRejectedValueOnce(new WeakPasswordError()).mockRejectedValueOnce(new NetworkError()))
    type('field.new_password', 'NewPassw0rd!')
    type('field.confirm', 'NewPassw0rd!')

    fireEvent.click(save())
    expect((await screen.findByRole('alert')).textContent).toContain('error.weak')

    fireEvent.click(save())
    await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('error.network'))
  })
})
