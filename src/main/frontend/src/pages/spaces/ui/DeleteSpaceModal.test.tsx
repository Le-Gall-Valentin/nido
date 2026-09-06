import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { DeleteSpaceModal } from './DeleteSpaceModal'
import { ServerError } from '@/shared/lib'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string | string[], opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : Array.isArray(k) ? k[0] : k) }),
}))

beforeEach(() => { vi.clearAllMocks() })

function setup(overrides: { onDelete?: () => Promise<void> } = {}) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  const onDelete = overrides.onDelete ?? vi.fn().mockResolvedValue(undefined)
  const result = render(<DeleteSpaceModal spaceName="Chez nous" onClose={onClose} onDelete={onDelete} onSuccess={onSuccess} />)
  return { ...result, onClose, onSuccess, onDelete }
}

describe('DeleteSpaceModal', () => {
  it('calls onDelete and onSuccess when confirmed', async () => {
    const { getByText, onDelete, onSuccess } = setup()
    fireEvent.click(getByText('delete.submit'))
    await waitFor(() => expect(onSuccess).toHaveBeenCalledOnce())
    expect(onDelete).toHaveBeenCalledOnce()
  })

  it('calls onClose when cancelled', () => {
    const { getByText, onClose } = setup()
    fireEvent.click(getByText('delete.cancel'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('shows a server error and does not call onSuccess', async () => {
    const { getByText, onSuccess, findByRole } = setup({ onDelete: vi.fn().mockRejectedValue(new ServerError()) })
    fireEvent.click(getByText('delete.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('delete.error.server')
    expect(onSuccess).not.toHaveBeenCalled()
  })
})
