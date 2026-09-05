import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { AddContributionModal } from './AddContributionModal'
import type { SpaceMember } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const members: SpaceMember[] = [{ userId: 'alice', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2026-01-01' }]

describe('AddContributionModal', () => {
  it('submits the selected member, amount and date', () => {
    const onSubmit = vi.fn()
    render(<AddContributionModal goalName="Vacances" members={members} onSubmit={onSubmit} onCancel={vi.fn()} isPending={false} />)

    fireEvent.change(screen.getByLabelText('form.amount_label'), { target: { value: '100' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ memberId: 'alice', amount: 100 }))
  })
})
