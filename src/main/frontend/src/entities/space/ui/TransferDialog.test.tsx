import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { TransferDialog } from './TransferDialog'
import type { TransferDestination } from './TransferDialog'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const DESTINATIONS: TransferDestination[] = [
  { id: 'space-2', name: 'La Famille', accent: '#c17a5c', glyph: '🏡' },
  { id: 'space-3', name: 'Coloc', accent: '#4a7fa0', glyph: '🏠' },
]

describe('TransferDialog', () => {
  it('lists every destination', () => {
    render(<TransferDialog itemName="Bolognaise" operation="copy" destinations={DESTINATIONS} onClose={vi.fn()} onConfirm={vi.fn()} />)

    expect(screen.getByText('La Famille')).toBeDefined()
    expect(screen.getByText('Coloc')).toBeDefined()
  })

  it('shows the empty state and disables confirm when there is no destination', () => {
    render(<TransferDialog itemName="Bolognaise" operation="copy" destinations={[]} onClose={vi.fn()} onConfirm={vi.fn()} />)

    expect(screen.getByText('transfer.no_destination')).toBeDefined()
    expect((screen.getByText('transfer.copy_submit').closest('button') as HTMLButtonElement).disabled).toBe(true)
  })

  it('disables confirm until a destination is selected', () => {
    render(<TransferDialog itemName="Bolognaise" operation="copy" destinations={DESTINATIONS} onClose={vi.fn()} onConfirm={vi.fn()} />)

    expect((screen.getByText('transfer.copy_submit').closest('button') as HTMLButtonElement).disabled).toBe(true)

    fireEvent.click(screen.getByText('La Famille'))

    expect((screen.getByText('transfer.copy_submit').closest('button') as HTMLButtonElement).disabled).toBe(false)
  })

  it('confirms the copy with the selected destination id, then closes', async () => {
    const onConfirm = vi.fn().mockResolvedValue(undefined)
    const onClose = vi.fn()
    render(<TransferDialog itemName="Bolognaise" operation="copy" destinations={DESTINATIONS} onClose={onClose} onConfirm={onConfirm} />)

    fireEvent.click(screen.getByText('La Famille'))
    fireEvent.click(screen.getByText('transfer.copy_submit'))

    await waitFor(() => expect(onConfirm).toHaveBeenCalledWith('space-2'))
    await waitFor(() => expect(onClose).toHaveBeenCalledOnce())
  })

  it('shows the move-specific label when operation is move', () => {
    render(<TransferDialog itemName="Bolognaise" operation="move" destinations={DESTINATIONS} onClose={vi.fn()} onConfirm={vi.fn()} />)

    expect(screen.getByText('transfer.move_submit')).toBeDefined()
    expect(screen.queryByText('transfer.copy_submit')).toBeNull()
  })

  it('shows an error and does not close when the transfer fails', async () => {
    const onConfirm = vi.fn().mockRejectedValue(new Error('boom'))
    const onClose = vi.fn()
    render(<TransferDialog itemName="Bolognaise" operation="copy" destinations={DESTINATIONS} onClose={onClose} onConfirm={onConfirm} />)

    fireEvent.click(screen.getByText('La Famille'))
    fireEvent.click(screen.getByText('transfer.copy_submit'))

    expect(await screen.findByRole('alert')).toBeDefined()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('calls onClose when cancelled', () => {
    const onClose = vi.fn()
    render(<TransferDialog itemName="Bolognaise" operation="copy" destinations={DESTINATIONS} onClose={onClose} onConfirm={vi.fn()} />)

    fireEvent.click(screen.getByText('transfer.cancel'))

    expect(onClose).toHaveBeenCalledOnce()
  })
})
