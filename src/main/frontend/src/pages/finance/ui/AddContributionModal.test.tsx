import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { AddContributionModal } from './AddContributionModal'
import type { SpaceMember } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const members: SpaceMember[] = [{ userId: 'alice', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2026-01-01' }]

describe('AddContributionModal', () => {
  it('shows the submit error handed down by the caller when the backend rejected the request', () => {
    render(<AddContributionModal goalName="Vacances" remaining={2000} members={members} onSubmit={vi.fn()} onCancel={vi.fn()} isPending={false} submitError="savings.contribution_too_high" />)

    expect(screen.getByText('savings.contribution_too_high')).toBeDefined()
  })

  it('submits the selected member, amount and date', () => {
    const onSubmit = vi.fn()
    render(<AddContributionModal goalName="Vacances" remaining={2000} members={members} onSubmit={onSubmit} onCancel={vi.fn()} isPending={false} />)

    fireEvent.change(screen.getByLabelText('form.amount_label'), { target: { value: '100' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ memberId: 'alice', amount: 100 }))
  })

  it('allows a contribution up to exactly what remains on the goal', () => {
    const onSubmit = vi.fn()
    render(<AddContributionModal goalName="Vacances" remaining={50} members={members} onSubmit={onSubmit} onCancel={vi.fn()} isPending={false} />)

    fireEvent.change(screen.getByLabelText('form.amount_label'), { target: { value: '50' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ amount: 50 }))
  })

  it('rejects a contribution that would exceed the goal target', () => {
    const onSubmit = vi.fn()
    render(<AddContributionModal goalName="Vacances" remaining={50} members={members} onSubmit={onSubmit} onCancel={vi.fn()} isPending={false} />)

    fireEvent.change(screen.getByLabelText('form.amount_label'), { target: { value: '51' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText('savings.contribution_too_high')).toBeDefined()
  })
})
