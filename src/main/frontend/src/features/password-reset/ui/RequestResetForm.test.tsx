import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'
import type { IPasswordResetApi } from '../model/IPasswordResetApi'
import { RequestResetForm } from './RequestResetForm'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: object) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

function setup(requestReset: IPasswordResetApi['requestReset']) {
  const onSent = vi.fn()
  const api: IPasswordResetApi = { capabilities: vi.fn(), requestReset, checkToken: vi.fn(), confirmReset: vi.fn() }
  render(<RequestResetForm api={api} onSent={onSent} />)
  const field = () => screen.getByLabelText('field.identifier')
  const submit = () => fireEvent.click(screen.getByRole('button', { name: 'action.send' }))
  return { onSent, field, submit }
}

describe('RequestResetForm', () => {
  it('sends the identifier without its surrounding spaces, then hands it on', async () => {
    const requestReset = vi.fn().mockResolvedValue(undefined)
    const { onSent, field, submit } = setup(requestReset)

    fireEvent.change(field(), { target: { value: '  jane  ' } })
    submit()

    await waitFor(() => expect(onSent).toHaveBeenCalledWith('jane'))
    expect(requestReset).toHaveBeenCalledWith('jane')
  })

  it('sends nothing for an empty identifier', () => {
    const requestReset = vi.fn()
    const { field, submit } = setup(requestReset)

    fireEvent.change(field(), { target: { value: '   ' } })
    submit()

    expect(requestReset).not.toHaveBeenCalled()
  })

  it('says how long to wait when asked too often', async () => {
    const { field, submit, onSent } = setup(vi.fn().mockRejectedValue(new RateLimitError(90)))

    fireEvent.change(field(), { target: { value: 'jane' } })
    submit()

    expect((await screen.findByRole('alert')).textContent).toContain('error.rateLimitWithDelay:{"seconds":90}')
    expect(onSent).not.toHaveBeenCalled()
  })

  it('tells a network failure from a server failure', async () => {
    const { field, submit } = setup(vi.fn().mockRejectedValueOnce(new NetworkError()).mockRejectedValueOnce(new ServerError()))

    fireEvent.change(field(), { target: { value: 'jane' } })
    submit()
    expect((await screen.findByRole('alert')).textContent).toContain('error.network')

    submit()
    await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('error.server'))
  })
})
