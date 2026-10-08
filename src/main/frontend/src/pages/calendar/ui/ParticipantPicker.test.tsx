import { describe, it, expect, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import type { SpaceMember } from '@/entities/space'
import { ParticipantPicker } from './ParticipantPicker'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

const members: SpaceMember[] = [
  { userId: 'u1', username: 'victor.lemoine', email: null, role: 'OWNER', joinedAt: '2026-01-01T00:00:00Z' },
  { userId: 'u2', username: 'valerie.lambert', email: null, role: 'MEMBER', joinedAt: '2026-01-02T00:00:00Z' },
  { userId: 'u3', username: null, email: null, role: 'MEMBER', joinedAt: '2026-01-03T00:00:00Z' },
]

describe('ParticipantPicker', () => {
  it('writes every name, so two people with the same initials can be told apart', () => {
    render(<ParticipantPicker members={members} selected={[]} onChange={vi.fn()} />)
    expect(screen.getByRole('button', { name: 'victor.lemoine' }).textContent).toContain('victor.lemoine')
    expect(screen.getByRole('button', { name: 'valerie.lambert' }).textContent).toContain('valerie.lambert')
    expect(screen.getByRole('button', { name: 'member.deleted' })).toBeDefined()
  })

  it('adds and removes a participant', () => {
    const onChange = vi.fn()
    render(<ParticipantPicker members={members} selected={['u1']} onChange={onChange} />)
    expect(screen.getByRole('button', { name: 'victor.lemoine' }).getAttribute('aria-pressed')).toBe('true')
    fireEvent.click(screen.getByRole('button', { name: 'valerie.lambert' }))
    expect(onChange).toHaveBeenLastCalledWith(['u1', 'u2'])
    fireEvent.click(screen.getByRole('button', { name: 'victor.lemoine' }))
    expect(onChange).toHaveBeenLastCalledWith([])
  })
})
