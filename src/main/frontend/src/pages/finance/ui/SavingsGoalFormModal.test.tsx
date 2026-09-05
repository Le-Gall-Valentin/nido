import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { SavingsGoalFormModal } from './SavingsGoalFormModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

describe('SavingsGoalFormModal', () => {
  it('rejects submitting without a name', () => {
    const onSubmit = vi.fn()
    render(<SavingsGoalFormModal mode="create" onSubmit={onSubmit} onCancel={vi.fn()} />)

    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits the entered name and target amount', () => {
    const onSubmit = vi.fn()
    render(<SavingsGoalFormModal mode="create" onSubmit={onSubmit} onCancel={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('savings.name_label'), { target: { value: 'Vacances' } })
    fireEvent.change(screen.getByLabelText('savings.target_amount_label'), { target: { value: '2000' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Vacances', targetAmount: 2000, targetDate: null })
  })
})
