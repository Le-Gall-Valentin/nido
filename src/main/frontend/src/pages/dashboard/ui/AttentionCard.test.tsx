import { fireEvent, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { formatAmount } from '@/shared/lib'
import type { AttentionItem } from '@/entities/dashboard'
import { AttentionCard } from './AttentionCard'
import { renderWithActions } from '../test/renderWithActions'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const INVITATION: Extract<AttentionItem, { kind: 'INVITATION' }> = {
  kind: 'INVITATION', severity: 'INFO', invitationId: 'i-1', spaceName: 'Coloc Lyon', spaceGlyph: '🏠',
  spaceAccent: '#c17a5c', role: 'MEMBER', invitedByUsername: 'camille', expiresAt: '2999-01-01T00:00:00Z',
}

const ITEMS: AttentionItem[] = [
  { kind: 'OVERDUE_TASKS', severity: 'HIGH', count: 2, titles: ['Changer le filtre', 'Rendre les livres'] },
  { kind: 'BUDGET_OVERRUN', severity: 'HIGH', categoryId: 'c-resto', label: 'Restaurants', spent: 212, limit: 180 },
  { kind: 'DEBT', severity: 'MEDIUM', toMemberId: 'u-cam', amount: 42.5 },
  INVITATION,
]

describe('AttentionCard', () => {
  it('counts what needs an action and sends overdue tasks and an overrun budget to their module', () => {
    renderWithActions(<AttentionCard items={ITEMS} />)

    expect(screen.getByRole('region', { name: 'attention.title' })).toBeDefined()
    expect(screen.getByText('4')).toBeDefined()
    expect(screen.getByText('attention.overdue_tasks:{"count":2}')).toBeDefined()
    expect(screen.getByText('Changer le filtre, Rendre les livres')).toBeDefined()
    expect(screen.getAllByRole('link', { name: 'attention.see' }).map((link) => link.getAttribute('href')))
      .toEqual(['/s/space-1/organisation/tasks', '/s/space-1/finance'])
  })

  it('names the member owed and opens the settlement from the row', () => {
    const settle = vi.fn()
    renderWithActions(<AttentionCard items={ITEMS} />, { actions: { settle } })

    // Testing Library normalizes the DOM's no-break spaces, not an exact matcher: normalize it the same way.
    expect(screen.getByText(`attention.debt:${JSON.stringify({ amount: formatAmount(42.5), name: 'camille' })}`.replace(/\s+/g, ' '))).toBeDefined()
    fireEvent.click(screen.getByRole('button', { name: 'attention.settle' }))

    expect(settle).toHaveBeenCalledWith({ toMemberId: 'u-cam', amount: 42.5 })
  })

  it('accepts an invitation, saying who sent it', async () => {
    const acceptInvitation = vi.fn().mockResolvedValue({ spaceId: 'space-9' })
    renderWithActions(<AttentionCard items={ITEMS} />, { spaceApi: { acceptInvitation } })

    expect(screen.getByText(/attention\.invitation_from:.*"name":"camille"/)).toBeDefined()
    fireEvent.click(screen.getByRole('button', { name: 'attention.accept' }))

    await waitFor(() => expect(acceptInvitation).toHaveBeenCalledWith('i-1'))
  })

  it('leaves the sender out when their account is gone', () => {
    renderWithActions(<AttentionCard items={[{ ...INVITATION, invitedByUsername: null }]} />)

    expect(screen.getByText(/attention\.invitation_detail:/)).toBeDefined()
    expect(screen.queryByText(/attention\.invitation_from/)).toBeNull()
  })

  it('reports a failed acceptance', async () => {
    const reportError = vi.fn()
    renderWithActions(<AttentionCard items={[INVITATION]} />, {
      actions: { reportError }, spaceApi: { acceptInvitation: vi.fn().mockRejectedValue(new Error('expired')) },
    })

    fireEvent.click(screen.getByRole('button', { name: 'attention.accept' }))

    await waitFor(() => expect(reportError).toHaveBeenCalled())
  })

  it('hides settling from a viewer, who can still accept an invitation of their own', () => {
    renderWithActions(<AttentionCard items={ITEMS} />, { actions: { canWrite: false } })

    expect(screen.queryByRole('button', { name: 'attention.settle' })).toBeNull()
    expect(screen.getByRole('button', { name: 'attention.accept' })).toBeDefined()
  })
})
