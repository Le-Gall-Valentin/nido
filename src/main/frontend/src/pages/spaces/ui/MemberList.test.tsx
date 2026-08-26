import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { SpaceMember } from '@/entities/space'
import { MemberList } from './MemberList'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => opts?.name ? `${k}:${opts.name}` : k, i18n: { language: 'en' } }),
}))

const OWNER: SpaceMember = { userId: 'u-owner', username: 'alice', email: 'alice@test.com', role: 'OWNER', joinedAt: '2024-01-01T00:00:00Z' }
const ADMIN: SpaceMember = { userId: 'u-admin', username: 'bob', email: 'bob@test.com', role: 'ADMIN', joinedAt: '2024-01-02T00:00:00Z' }
const VIEWER: SpaceMember = { userId: 'u-viewer', username: 'carol', email: 'carol@test.com', role: 'VIEWER', joinedAt: '2024-01-03T00:00:00Z' }
const DELETED: SpaceMember = { userId: 'u-deleted', username: null, email: null, role: 'MEMBER', joinedAt: '2024-01-04T00:00:00Z' }

function noop() {}

describe('MemberList — deleted accounts', () => {
  it('renders a member with a null username as a deleted account, not a blank row', () => {
    render(
      <MemberList members={[DELETED]} currentUserId="someone-else" myRole="OWNER" onChangeRole={noop} onRemove={noop} onTransfer={noop} />
    )
    expect(screen.getByText('members.deleted_account')).toBeDefined()
  })

  it('does not crash when both username and email are null', () => {
    expect(() =>
      render(
        <MemberList members={[DELETED]} currentUserId="someone-else" myRole="OWNER" onChangeRole={noop} onRemove={noop} onTransfer={noop} />
      )
    ).not.toThrow()
  })

  it('also treats an absent (undefined) username as a deleted account, not a blank row', () => {
    // The `=== null` check this used to use would miss an absent field —
    // the API contract promises null, but a strict equality check is not
    // the defense that actually guarantees it.
    const absentUsername = { ...DELETED, username: undefined } as unknown as SpaceMember
    render(
      <MemberList members={[absentUsername]} currentUserId="someone-else" myRole="OWNER" onChangeRole={noop} onRemove={noop} onTransfer={noop} />
    )
    expect(screen.getByText('members.deleted_account')).toBeDefined()
  })
})

describe('MemberList — empty state', () => {
  it('shows the empty message when there are no members', () => {
    render(<MemberList members={[]} currentUserId="me" myRole="OWNER" onChangeRole={noop} onRemove={noop} onTransfer={noop} />)
    expect(screen.getByText('members.empty')).toBeDefined()
  })
})

describe('MemberList — action gating', () => {
  it('never shows manage actions on my own row, even as owner', () => {
    render(
      <MemberList members={[OWNER]} currentUserId={OWNER.userId} myRole="OWNER" onChangeRole={noop} onRemove={noop} onTransfer={noop} />
    )
    expect(screen.queryByRole('combobox')).toBeNull()
    expect(screen.queryByLabelText(/action_remove/)).toBeNull()
  })

  it('never shows manage or transfer actions on the owner row', () => {
    render(
      <MemberList members={[OWNER, ADMIN]} currentUserId={ADMIN.userId} myRole="ADMIN" onChangeRole={noop} onRemove={noop} onTransfer={noop} />
    )
    // Only the OWNER row is rendered here besides my own (ADMIN) row, which is also excluded.
    expect(screen.queryByRole('combobox')).toBeNull()
    expect(screen.queryByLabelText(/action_transfer/)).toBeNull()
  })

  it('shows change-role select and remove button for a manageable member when I am ADMIN', () => {
    render(
      <MemberList members={[OWNER, VIEWER]} currentUserId="u-admin" myRole="ADMIN" onChangeRole={noop} onRemove={noop} onTransfer={noop} />
    )
    expect(screen.getByRole('combobox')).toBeDefined()
    expect(screen.getByLabelText('members.action_remove:carol')).toBeDefined()
  })

  it('shows change-role select and remove button for a manageable member when I am OWNER', () => {
    render(
      <MemberList members={[OWNER, VIEWER]} currentUserId={OWNER.userId} myRole="OWNER" onChangeRole={noop} onRemove={noop} onTransfer={noop} />
    )
    expect(screen.getByRole('combobox')).toBeDefined()
    expect(screen.getByLabelText('members.action_remove:carol')).toBeDefined()
  })

  it('does not show manage actions when my role cannot manage the space (MEMBER)', () => {
    render(
      <MemberList members={[OWNER, VIEWER]} currentUserId="someone-else" myRole="MEMBER" onChangeRole={noop} onRemove={noop} onTransfer={noop} />
    )
    expect(screen.queryByRole('combobox')).toBeNull()
  })

  it('shows the transfer action only for OWNER, excluding VIEWER targets', () => {
    render(
      <MemberList members={[OWNER, ADMIN, VIEWER]} currentUserId={OWNER.userId} myRole="OWNER" onChangeRole={noop} onRemove={noop} onTransfer={noop} />
    )
    expect(screen.getByLabelText('members.action_transfer:bob')).toBeDefined()
    expect(screen.queryByLabelText('members.action_transfer:carol')).toBeNull()
  })

  it('does not show the transfer action when I am only ADMIN, not OWNER', () => {
    render(
      <MemberList members={[OWNER, VIEWER]} currentUserId="someone-else" myRole="ADMIN" onChangeRole={noop} onRemove={noop} onTransfer={noop} />
    )
    expect(screen.queryByLabelText(/action_transfer/)).toBeNull()
  })
})

describe('MemberList — interactions', () => {
  it('calls onChangeRole with the member and the selected role', () => {
    const onChangeRole = vi.fn()
    render(
      <MemberList members={[OWNER, VIEWER]} currentUserId={OWNER.userId} myRole="OWNER" onChangeRole={onChangeRole} onRemove={noop} onTransfer={noop} />
    )
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'ADMIN' } })
    expect(onChangeRole).toHaveBeenCalledWith(VIEWER, 'ADMIN')
  })

  it('calls onRemove with the member', () => {
    const onRemove = vi.fn()
    render(
      <MemberList members={[OWNER, VIEWER]} currentUserId={OWNER.userId} myRole="OWNER" onChangeRole={noop} onRemove={onRemove} onTransfer={noop} />
    )
    fireEvent.click(screen.getByLabelText('members.action_remove:carol'))
    expect(onRemove).toHaveBeenCalledWith(VIEWER)
  })

  it('calls onTransfer with the member', () => {
    const onTransfer = vi.fn()
    render(
      <MemberList members={[OWNER, ADMIN]} currentUserId={OWNER.userId} myRole="OWNER" onChangeRole={noop} onRemove={noop} onTransfer={onTransfer} />
    )
    fireEvent.click(screen.getByLabelText('members.action_transfer:bob'))
    expect(onTransfer).toHaveBeenCalledWith(ADMIN)
  })
})
