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

function renderPanel(api: IFinanceApi) {
  return renderWithQuery(
    <FinanceApiProvider api={api}>
      <OpeningBalancePanel spaceId="space-1" spaceTimezone="Europe/Paris" onClose={vi.fn()} />
    </FinanceApiProvider>
  )
}

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
})
