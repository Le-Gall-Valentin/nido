import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AlreadyJoinedError, type AdminUser } from '@/entities/user'
import { ResendInvitationModal } from './ResendInvitationModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: object) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

const CAROL: AdminUser = {
  id: 'u-3', username: 'carol', email: 'carol@test.com', role: 'USER', isActive: true,
  createdAt: '2026-10-01T00:00:00Z', twoFactorMethods: [], invitation: { status: 'expired', expiresAt: '2026-10-04T00:00:00Z' },
}

describe('ResendInvitationModal', () => {
  it('speaks of a mail when mail is on, then says the new invitation left', async () => {
    const onResend = vi.fn().mockResolvedValue({ delivery: 'mail' })
    render(<ResendInvitationModal user={CAROL} mail="available" onClose={vi.fn()} onResend={onResend} />)

    expect(screen.getByText('resend.body_mail')).not.toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'resend.submit_mail' }))

    await waitFor(() => expect(onResend).toHaveBeenCalledWith('u-3'))
    expect(await screen.findByText('invitation.mailed:{"username":"carol","email":"carol@test.com"}')).not.toBeNull()
  })

  it('speaks of a new link when mail is off, then shows it', async () => {
    const onResend = vi.fn().mockResolvedValue({ delivery: 'link', link: '/welcome#token=new' })
    render(<ResendInvitationModal user={CAROL} mail="unavailable" onClose={vi.fn()} onResend={onResend} />)

    fireEvent.click(screen.getByRole('button', { name: 'resend.submit_link' }))

    expect(((await screen.findByRole('textbox')) as HTMLInputElement).value).toContain('/welcome#token=new')
  })

  it('says when the account chose its password meanwhile', async () => {
    const onResend = vi.fn().mockRejectedValue(new AlreadyJoinedError())
    render(<ResendInvitationModal user={CAROL} mail="available" onClose={vi.fn()} onResend={onResend} />)

    fireEvent.click(screen.getByRole('button', { name: 'resend.submit_mail' }))

    expect(await screen.findByText('resend.error.already_joined')).not.toBeNull()
  })
})
