import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { DeleteTaskModal } from './DeleteTaskModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

describe('DeleteTaskModal', () => {
  it('confirms deletion then closes', async () => {
    const onDelete = vi.fn().mockResolvedValue(undefined)
    const onClose = vi.fn()
    render(<DeleteTaskModal taskTitle="Sortir les poubelles" onClose={onClose} onDelete={onDelete} />)

    fireEvent.click(screen.getByText('delete_confirm.confirm'))

    await waitFor(() => expect(onDelete).toHaveBeenCalledOnce())
    await waitFor(() => expect(onClose).toHaveBeenCalledOnce())
  })

  it('shows an error and does not close when deletion fails', async () => {
    const onDelete = vi.fn().mockRejectedValue(new Error('boom'))
    const onClose = vi.fn()
    render(<DeleteTaskModal taskTitle="Sortir les poubelles" onClose={onClose} onDelete={onDelete} />)

    fireEvent.click(screen.getByText('delete_confirm.confirm'))

    expect(await screen.findByRole('alert')).toBeDefined()
    expect(onClose).not.toHaveBeenCalled()
  })
})
