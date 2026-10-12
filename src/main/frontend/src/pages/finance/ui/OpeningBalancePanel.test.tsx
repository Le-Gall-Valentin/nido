import { describe, expect, it, vi } from 'vitest'
import { screen, fireEvent, waitFor } from '@testing-library/react'
import { renderWithQuery } from '@/shared/test'
import { FinanceApiProvider, type IFinanceApi } from '@/entities/finance'
import { OpeningBalancePanel } from './OpeningBalancePanel'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

/** Only what the panel calls: the rest of the finance API is never reached from it. */
function fakeApi(overrides: Partial<IFinanceApi>): IFinanceApi {
  return {
    getOpeningBalance: vi.fn().mockResolvedValue(null), setOpeningBalance: vi.fn(), clearOpeningBalance: vi.fn(),
    ...overrides,
  } as unknown as IFinanceApi
}

function renderPanel(api: IFinanceApi, onClose = vi.fn()) {
  renderWithQuery(
    <FinanceApiProvider api={api}>
      <OpeningBalancePanel spaceId="space-1" spaceTimezone="Europe/Paris" onClose={onClose} />
    </FinanceApiProvider>
  )
  return onClose
}

const CURRENT = { amount: 1200, asOf: '2026-09-30' }

describe('OpeningBalancePanel', () => {
  it('shows that it is loading, rather than nothing, while the current opening balance loads', () => {
    renderPanel(fakeApi({ getOpeningBalance: vi.fn().mockReturnValue(new Promise(() => {})) }))

    expect(screen.getByLabelText('loading')).toBeDefined()
    expect(screen.queryByLabelText('opening_balance.amount_label')).toBeNull()
  })

  it('says the opening balance could not be loaded, rather than offering an empty form as if there were none', async () => {
    renderPanel(fakeApi({ getOpeningBalance: vi.fn().mockRejectedValue(new Error('boom')) }))

    await waitFor(() => expect(screen.getByText('opening_balance.load_failed')).toBeDefined())
    expect(screen.queryByLabelText('opening_balance.amount_label')).toBeNull()
  })

  it('keeps Save busy while the save is on its way, so a second click sends nothing', async () => {
    const setOpeningBalance = vi.fn().mockReturnValue(new Promise(() => {}))
    renderPanel(fakeApi({ setOpeningBalance }))

    fireEvent.change(await screen.findByLabelText('opening_balance.amount_label'), { target: { value: '100' } })
    fireEvent.click(screen.getByText('form.save'))

    await waitFor(() => expect(screen.getByText('form.save').closest('button')?.disabled).toBe(true))
    fireEvent.click(screen.getByText('form.save'))
    expect(setOpeningBalance).toHaveBeenCalledTimes(1)
  })

  it('closes once the opening balance is saved', async () => {
    const setOpeningBalance = vi.fn().mockResolvedValue(CURRENT)
    const onClose = renderPanel(fakeApi({ setOpeningBalance }))

    fireEvent.change(await screen.findByLabelText('opening_balance.amount_label'), { target: { value: '1200' } })
    fireEvent.click(screen.getByText('form.save'))

    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(setOpeningBalance).toHaveBeenCalledWith('space-1', 1200, expect.any(String))
  })

  it('removes the opening balance of this space and closes', async () => {
    const clearOpeningBalance = vi.fn().mockResolvedValue(undefined)
    const onClose = renderPanel(fakeApi({ getOpeningBalance: vi.fn().mockResolvedValue(CURRENT), clearOpeningBalance }))

    fireEvent.click(await screen.findByText('opening_balance.clear'))
    fireEvent.click(screen.getByText('opening_balance.clear_confirm'))

    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(clearOpeningBalance).toHaveBeenCalledWith('space-1')
  })

  it('stays open and says so when the save is refused', async () => {
    const onClose = renderPanel(fakeApi({ setOpeningBalance: vi.fn().mockRejectedValue(new Error('boom')) }))

    fireEvent.change(await screen.findByLabelText('opening_balance.amount_label'), { target: { value: '1200' } })
    fireEvent.click(screen.getByText('form.save'))

    await waitFor(() => expect(screen.getByText('form.submit_error')).toBeDefined())
    expect(onClose).not.toHaveBeenCalled()
  })

  it('stays open and says so when the removal is refused', async () => {
    const onClose = renderPanel(fakeApi({
      getOpeningBalance: vi.fn().mockResolvedValue(CURRENT), clearOpeningBalance: vi.fn().mockRejectedValue(new Error('boom')),
    }))

    fireEvent.click(await screen.findByText('opening_balance.clear'))
    fireEvent.click(screen.getByText('opening_balance.clear_confirm'))

    await waitFor(() => expect(screen.getByText('form.submit_error')).toBeDefined())
    expect(onClose).not.toHaveBeenCalled()
  })

  it('asks before removing the opening balance, and removes nothing when told not to', async () => {
    const clearOpeningBalance = vi.fn().mockResolvedValue(undefined)
    const onClose = renderPanel(fakeApi({ getOpeningBalance: vi.fn().mockResolvedValue(CURRENT), clearOpeningBalance }))

    fireEvent.click(await screen.findByText('opening_balance.clear'))
    expect(screen.getAllByText('opening_balance.clear_title').length).toBeGreaterThan(0)
    fireEvent.click(screen.getByText('delete_confirm.cancel'))

    expect(screen.queryAllByText('opening_balance.clear_title')).toHaveLength(0)
    expect(clearOpeningBalance).not.toHaveBeenCalled()
    expect(onClose).not.toHaveBeenCalled()
  })
})
