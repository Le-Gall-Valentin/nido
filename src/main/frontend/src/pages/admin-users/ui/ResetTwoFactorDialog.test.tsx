import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AdminUser, TwoFactorMethod } from '@/entities/user'
import { ServerError } from '@/shared/lib'
import { ResetTwoFactorDialog } from './ResetTwoFactorDialog'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: Record<string, unknown>) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

function target(methods: TwoFactorMethod[]): AdminUser {
  return { id: 'u-1', username: 'alice', email: 'alice@test.com', role: 'USER', isActive: true, invitation: null,
    createdAt: '2024-01-01T00:00:00Z', twoFactorMethods: methods }
}

function setup(methods: TwoFactorMethod[], onReset = vi.fn().mockResolvedValue(undefined)) {
  const handlers = { onClose: vi.fn(), onSuccess: vi.fn() }
  render(<ResetTwoFactorDialog user={target(methods)} mail="available" onReset={onReset} {...handlers} />)
  return { ...handlers, onReset }
}

beforeEach(() => { vi.clearAllMocks() })

describe('ResetTwoFactorDialog', () => {
  it('with two methods nothing is ticked and nothing can be sent', () => {
    setup(['APP', 'MAIL'])

    expect((screen.getByLabelText(/reset_two_factor\.app/) as HTMLInputElement).checked).toBe(false)
    expect((screen.getByLabelText(/reset_two_factor\.mail/) as HTMLInputElement).checked).toBe(false)
    expect((screen.getByRole('button', { name: /reset_two_factor\.submit_one/ }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('ticking one says the other still protects the account, and removes only it', async () => {
    const { onReset, onSuccess } = setup(['APP', 'MAIL'])

    fireEvent.click(screen.getByLabelText(/reset_two_factor\.app/))
    expect(screen.getByText('reset_two_factor.keeps_mail')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /reset_two_factor\.submit_one/ }))

    await waitFor(() => expect(onSuccess).toHaveBeenCalled())
    expect(onReset).toHaveBeenCalledWith({ id: 'u-1', methods: ['APP'] })
  })

  it('ticking both warns the password alone is left', () => {
    setup(['APP', 'MAIL'])

    fireEvent.click(screen.getByLabelText(/reset_two_factor\.mail/))
    fireEvent.click(screen.getByLabelText(/reset_two_factor\.app/))

    expect(screen.getByText('reset_two_factor.none_left')).toBeTruthy()
    expect(screen.getByRole('button', { name: /reset_two_factor\.submit_all/ })).toBeTruthy()
  })

  it('a single method is shown ticked, with no choice to make', async () => {
    const { onReset } = setup(['MAIL'])

    const box = screen.getByLabelText(/reset_two_factor\.mail/) as HTMLInputElement
    expect(box.checked).toBe(true)
    expect(box.disabled).toBe(true)
    expect(screen.queryByLabelText(/reset_two_factor\.app/)).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: /reset_two_factor\.submit_one/ }))

    await waitFor(() => expect(onReset).toHaveBeenCalledWith({ id: 'u-1', methods: ['MAIL'] }))
  })

  it('says what went wrong', async () => {
    setup(['APP'], vi.fn().mockRejectedValue(new ServerError()))

    fireEvent.click(screen.getByRole('button', { name: /reset_two_factor\.submit_one/ }))

    expect((await screen.findByRole('alert')).textContent).toContain('reset_two_factor.error.server')
  })

  it('cancel closes', () => {
    const { onClose } = setup(['APP'])

    fireEvent.click(screen.getByText('reset_two_factor.cancel'))

    expect(onClose).toHaveBeenCalled()
  })
})
