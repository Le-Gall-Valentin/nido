import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { InvitationBadge } from './InvitationBadge'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

describe('InvitationBadge', () => {
  it('tells a pending invitation from an expired one', () => {
    const { rerender } = render(<InvitationBadge invitation={{ status: 'pending', expiresAt: '2026-10-12T10:00:00Z' }} />)
    expect(screen.getByText('invitation.pending')).not.toBeNull()

    rerender(<InvitationBadge invitation={{ status: 'expired', expiresAt: '2026-10-01T10:00:00Z' }} />)
    expect(screen.getByText('invitation.expired')).not.toBeNull()
  })
})
