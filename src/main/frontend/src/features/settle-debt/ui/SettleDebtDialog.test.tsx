import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { FinanceApiProvider, type IFinanceApi } from '@/entities/finance'
import { SettleDebtDialog } from './SettleDebtDialog'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const NAMES: Record<string, string> = { 'u-me': 'valentin', 'u-cam': 'camille' }

function renderDialog(settleDebt: IFinanceApi['settleDebt'], onClose = vi.fn()) {
  render(
    <QueryClientProvider client={createTestQueryClient()}>
      <FinanceApiProvider api={{ settleDebt } as unknown as IFinanceApi}>
        <SettleDebtDialog spaceId="space-1" debt={{ fromMemberId: 'u-me', toMemberId: 'u-cam', amount: 42.5 }}
          memberLabel={(id) => NAMES[id]} onClose={onClose} />
      </FinanceApiProvider>
    </QueryClientProvider>
  )
  return { onClose }
}

describe('SettleDebtDialog', () => {
  it('settles the debt it was opened on, for the amount chosen, and closes', async () => {
    const settleDebt = vi.fn().mockResolvedValue(undefined)
    const { onClose } = renderDialog(settleDebt)

    fireEvent.change(screen.getByLabelText('amount_label'), { target: { value: '20' } })
    fireEvent.click(screen.getByText('confirm'))

    await waitFor(() => expect(settleDebt).toHaveBeenCalledWith('space-1', 'u-me', 'u-cam', 20, expect.any(String)))
    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1))
  })

  it('names who pays whom with the labels it is given', () => {
    renderDialog(vi.fn())

    expect(screen.getByText(/^message:\{"from":"valentin","to":"camille"/)).toBeDefined()
  })

  it('says so when the settlement is refused, and stays open', async () => {
    const { onClose } = renderDialog(vi.fn().mockRejectedValue(new Error('down')))

    fireEvent.click(screen.getByText('confirm'))

    expect(await screen.findByText('submit_error')).toBeDefined()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('closes on cancel without settling anything', () => {
    const settleDebt = vi.fn()
    const { onClose } = renderDialog(settleDebt)

    fireEvent.click(screen.getByText('cancel'))

    expect(onClose).toHaveBeenCalledTimes(1)
    expect(settleDebt).not.toHaveBeenCalled()
  })
})
