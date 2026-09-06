import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { SavingsGoalFormModal } from './SavingsGoalFormModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

describe('SavingsGoalFormModal', () => {
  it('shows the submit error handed down by the caller when the backend rejected the request', () => {
    render(<SavingsGoalFormModal mode="create" onSubmit={vi.fn()} onCancel={vi.fn()} submitError="form.submit_error" />)

    expect(screen.getByText('form.submit_error')).toBeDefined()
  })

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

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Vacances', targetAmount: 2000, targetDate: null, color: '#5c7a58', glyph: '🎯' })
  })

  it('defaults to the first color and glyph of the palette when creating a goal', () => {
    const onSubmit = vi.fn()
    render(<SavingsGoalFormModal mode="create" onSubmit={onSubmit} onCancel={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('savings.name_label'), { target: { value: 'Vacances' } })
    fireEvent.change(screen.getByLabelText('savings.target_amount_label'), { target: { value: '2000' } })
    fireEvent.click(screen.getByLabelText('savings.color_option:{"color":"#c17a5c"}'))
    fireEvent.click(screen.getByLabelText('savings.glyph_option:{"glyph":"🏖️"}'))
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Vacances', targetAmount: 2000, targetDate: null, color: '#c17a5c', glyph: '🏖️' })
  })

  it('pre-fills the color and glyph from the goal being edited', () => {
    const onSubmit = vi.fn()
    const goal = {
      id: 'g1', name: 'Vacances', targetAmount: 2000, targetDate: null, color: '#4a7fa0', glyph: '🏠',
      totalContributed: 0, contributions: [],
    }
    render(<SavingsGoalFormModal mode="edit" goal={goal} onSubmit={onSubmit} onCancel={vi.fn()} />)

    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Vacances', targetAmount: 2000, targetDate: null, color: '#4a7fa0', glyph: '🏠' })
  })
})
