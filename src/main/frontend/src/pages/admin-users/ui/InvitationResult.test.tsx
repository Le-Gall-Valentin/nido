import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { InvitationResult } from './InvitationResult'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: object) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

const copyText = vi.hoisted(() => vi.fn())
vi.mock('@/shared/lib/copyText', () => ({ copyText }))

describe('InvitationResult', () => {
  it('says the invitation was mailed, and shows no link', () => {
    render(<InvitationResult username="carol" email="carol@test.com" delivery={{ delivery: 'mail' }} />)

    expect(screen.getByText('invitation.mailed:{"username":"carol","email":"carol@test.com"}')).not.toBeNull()
    expect(screen.queryByRole('textbox')).toBeNull()
  })

  it('shows a link to pass on, completed with this address when the server gave only its path', () => {
    render(<InvitationResult username="carol" email="carol@test.com" delivery={{ delivery: 'link', link: '/welcome#token=abc' }} />)

    expect((screen.getByRole('textbox', { name: 'invitation.link_label' }) as HTMLInputElement).value)
      .toBe(`${window.location.origin}/welcome#token=abc`)
    expect(screen.getByText('invitation.once')).not.toBeNull()
  })

  it('keeps a full link as it is, and copies it', async () => {
    copyText.mockResolvedValue(true)
    render(<InvitationResult username="carol" email="carol@test.com"
      delivery={{ delivery: 'link', link: 'https://nido.example/welcome#token=abc' }} />)

    fireEvent.click(screen.getByRole('button', { name: 'invitation.copy' }))

    await waitFor(() => expect(copyText).toHaveBeenCalledWith('https://nido.example/welcome#token=abc'))
    expect(await screen.findByRole('button', { name: 'invitation.copied' })).not.toBeNull()
  })

  it('says so when the browser could not copy the link, which stays there to copy by hand', async () => {
    copyText.mockResolvedValue(false)
    render(<InvitationResult username="carol" email="carol@test.com"
      delivery={{ delivery: 'link', link: 'https://nido.example/welcome#token=abc' }} />)

    fireEvent.click(screen.getByRole('button', { name: 'invitation.copy' }))

    expect((await screen.findByText('invitation.copy_failed')).getAttribute('role')).toBe('status')
  })
})
