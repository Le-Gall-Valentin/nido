import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { NetworkError } from '@/shared/lib'
import type { IPasswordResetApi } from '../model/IPasswordResetApi'
import { InvalidResetLinkError, WeakPasswordError } from '../model/errors'
import { NewPasswordForm } from './NewPasswordForm'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: object) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

function setup(confirmReset: IPasswordResetApi['confirmReset']) {
  const onDone = vi.fn()
  const onInvalid = vi.fn()
  const api: IPasswordResetApi = { capabilities: vi.fn(), requestReset: vi.fn(), checkToken: vi.fn(), confirmReset }
  render(<NewPasswordForm api={api} token="abc" onDone={onDone} onInvalid={onInvalid} />)
  const type = (label: string, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } })
  const save = () => screen.getByRole('button', { name: 'action.save' }) as HTMLButtonElement
  return { onDone, onInvalid, type, save }
}

describe('NewPasswordForm', () => {
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
    const { type, save, onInvalid, onDone } = setup(vi.fn().mockRejectedValue(new InvalidResetLinkError()))

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
