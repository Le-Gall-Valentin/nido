import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { AdminUser } from '@/entities/user'
import { MailNotice } from './MailNotice'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: { username?: string }) => (o?.username ? `${k}:${o.username}` : k) }),
}))

const CAROL: AdminUser = {
  id: 'u-3', username: 'carol', email: 'carol@test.com', role: 'USER', isActive: true,
  createdAt: '2026-10-01T00:00:00Z', twoFactorMethods: [], invitation: null,
}
const INVITED: AdminUser = { ...CAROL, invitation: { status: 'pending', expiresAt: '2026-10-12T00:00:00Z' } }

describe('MailNotice', () => {
  it('says the holder will be told by mail', () => {
    render(<MailNotice user={CAROL} mail="available" />)
    expect(screen.getByText('notice.mail:carol')).not.toBeNull()
  })

  it('says nobody will be told when mail is not set up', () => {
    render(<MailNotice user={CAROL} mail="unavailable" />)
    expect(screen.getByText('notice.no_mail:carol')).not.toBeNull()
  })

  it('says an invited account hears nothing, except that its invitation is cancelled', () => {
    const { rerender } = render(<MailNotice user={INVITED} mail="available" />)
    expect(screen.getByText('notice.invited:carol')).not.toBeNull()

    rerender(<MailNotice user={INVITED} mail="available" isDeletion />)
    expect(screen.getByText('notice.invitation_cancelled:carol')).not.toBeNull()
  })

  it('says nothing while it does not know', () => {
    const { container } = render(<MailNotice user={CAROL} mail="loading" />)
    expect(container.textContent).toBe('')
  })
})
