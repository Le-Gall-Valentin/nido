import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { SpaceInvitation } from '@/entities/space'
import { InvitationList } from './InvitationList'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k), i18n: { language: 'en' } }),
}))

const PENDING: SpaceInvitation = {
  id: 'i-1', email: 'carol@test.com', role: 'MEMBER', code: 'NIDO-ABC123',
  status: 'PENDING', expiresAt: '2999-01-01T00:00:00Z', createdAt: '2024-01-01T00:00:00Z',
}
const REVOKED: SpaceInvitation = { ...PENDING, id: 'i-2', status: 'REVOKED' }
const ACCEPTED: SpaceInvitation = { ...PENDING, id: 'i-3', status: 'ACCEPTED', expiresAt: '2020-01-01T00:00:00Z' }

const writeText = vi.fn().mockResolvedValue(undefined)
beforeEach(() => {
  writeText.mockClear()
  Object.assign(navigator, { clipboard: { writeText } })
})

describe('InvitationList — empty state', () => {
  it('shows the empty message when there are no invitations', () => {
    render(<InvitationList invitations={[]} onRevoke={vi.fn()} />)
    expect(screen.getByText('invitations.empty')).toBeDefined()
  })
})

describe('InvitationList — rendering', () => {
  it('shows the email and the code with a copy button', () => {
    render(<InvitationList invitations={[PENDING]} onRevoke={vi.fn()} />)
    expect(screen.getByText('carol@test.com')).toBeDefined()
    expect(screen.getByText('NIDO-ABC123')).toBeDefined()
  })

  it('shows a revoke action for a PENDING invitation', () => {
    render(<InvitationList invitations={[PENDING]} onRevoke={vi.fn()} />)
    expect(screen.getByLabelText(/action_revoke/)).toBeDefined()
  })

  it('does not show a revoke action for a REVOKED invitation', () => {
    render(<InvitationList invitations={[REVOKED]} onRevoke={vi.fn()} />)
    expect(screen.queryByLabelText(/action_revoke/)).toBeNull()
  })

  it('shows the expiry line for a PENDING invitation', () => {
    render(<InvitationList invitations={[PENDING]} onRevoke={vi.fn()} />)
    expect(screen.getByText(/invitations\.expires/)).toBeDefined()
  })

  it('does not show the expiry line for an ACCEPTED invitation', () => {
    // Once accepted or revoked, "expires" is meaningless — and reads as
    // stale/wrong when the date has since passed.
    render(<InvitationList invitations={[ACCEPTED]} onRevoke={vi.fn()} />)
    expect(screen.queryByText(/invitations\.expires/)).toBeNull()
  })

  it('does not show the expiry line for a REVOKED invitation', () => {
    render(<InvitationList invitations={[REVOKED]} onRevoke={vi.fn()} />)
    expect(screen.queryByText(/invitations\.expires/)).toBeNull()
  })
})

describe('InvitationList — interactions', () => {
  it('calls onRevoke with the invitation when the revoke button is clicked', () => {
    const onRevoke = vi.fn()
    render(<InvitationList invitations={[PENDING]} onRevoke={onRevoke} />)
    fireEvent.click(screen.getByLabelText(/action_revoke/))
    expect(onRevoke).toHaveBeenCalledWith(PENDING)
  })

  it('copies the code to the clipboard when the copy button is clicked', async () => {
    render(<InvitationList invitations={[PENDING]} onRevoke={vi.fn()} />)
    fireEvent.click(screen.getByLabelText(/action_copy/))
    await waitFor(() => expect(writeText).toHaveBeenCalledWith('NIDO-ABC123'))
  })
})
