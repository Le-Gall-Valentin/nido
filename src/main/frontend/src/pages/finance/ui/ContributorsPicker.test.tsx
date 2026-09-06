import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ContributorsPicker } from './ContributorsPicker'
import type { SpaceMember } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const alice: SpaceMember = { userId: 'alice', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2026-01-01' }
const bob: SpaceMember = { userId: 'bob', username: 'bob', email: 'b@test.com', role: 'MEMBER', joinedAt: '2026-01-01' }

function renderPicker(overrides: Partial<React.ComponentProps<typeof ContributorsPicker>> = {}) {
  return render(
    <ContributorsPicker
      members={[alice, bob]} payerId="alice" onPayerChange={vi.fn()}
      contributorIds={['alice', 'bob']} onToggleContributor={vi.fn()}
      customizeShares={false} onCustomizeSharesChange={vi.fn()}
      customShares={{}} onCustomShareChange={vi.fn()}
      {...overrides}
    />
  )
}

describe('ContributorsPicker', () => {
  it('lists every member as a payer option and a contributor toggle', () => {
    renderPicker()

    expect(screen.getByLabelText('form.payer_label')).toBeDefined()
    expect(screen.getByRole('button', { name: 'alice' })).toBeDefined()
    expect(screen.getByRole('button', { name: 'bob' })).toBeDefined()
  })

  it('reports a payer change', () => {
    const onPayerChange = vi.fn()
    renderPicker({ onPayerChange })

    fireEvent.change(screen.getByLabelText('form.payer_label'), { target: { value: 'bob' } })

    expect(onPayerChange).toHaveBeenCalledWith('bob')
  })

  it('toggles a contributor on click', () => {
    const onToggleContributor = vi.fn()
    renderPicker({ onToggleContributor })

    fireEvent.click(screen.getByRole('button', { name: 'bob' }))

    expect(onToggleContributor).toHaveBeenCalledWith('bob')
  })

  it('hides the customize-shares toggle when there are no contributors', () => {
    renderPicker({ contributorIds: [] })

    expect(screen.queryByText('form.customize_shares_label')).toBeNull()
  })

  it('shows a share amount input per contributor once customization is on', () => {
    const onCustomShareChange = vi.fn()
    renderPicker({ customizeShares: true, customShares: { alice: 30 }, onCustomShareChange })

    const inputs = screen.getAllByRole('spinbutton')
    fireEvent.change(inputs[0], { target: { value: '40' } })

    expect(onCustomShareChange).toHaveBeenCalledWith('alice', 40)
  })
})
