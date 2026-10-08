import { renderHook } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { SpaceMember } from '../model/types'
import { useMemberName } from './useMemberName'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

const member = (userId: string, username: string | null): SpaceMember => ({
  userId, username, email: username && `${username}@nido.test`, role: 'MEMBER', joinedAt: '2026-01-01T00:00:00Z',
})

describe('useMemberName', () => {
  it('names a member by their account name', () => {
    const { result } = renderHook(() => useMemberName([member('u-1', 'marie')]))
    expect(result.current('u-1')).toBe('marie')
  })

  it('names an anonymised account as deleted, never by its id', () => {
    const { result } = renderHook(() => useMemberName([member('u-1', null)]))
    expect(result.current('u-1')).toBe('member.deleted')
  })

  it('names someone no longer in the space as a former member', () => {
    const { result } = renderHook(() => useMemberName([member('u-1', 'marie')]))
    expect(result.current('u-gone')).toBe('member.former')
  })

  it('says only "a member" while the list is unknown, since nobody can be said to have left', () => {
    const { result } = renderHook(() => useMemberName(undefined))
    expect(result.current('u-1')).toBe('member.generic')
  })
})
