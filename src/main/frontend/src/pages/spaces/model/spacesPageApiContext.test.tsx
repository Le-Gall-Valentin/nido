import { render, renderHook } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { SpacesPageApiProvider, useSpacesPageApi } from './spacesPageApiContext'
import type { ISpacesPageApi } from './ISpacesPageApi'

function fakeApi(): ISpacesPageApi {
  return {
    getSpaceDetail: vi.fn(),
    listMembers: vi.fn(),
    listInvitations: vi.fn(),
    listReceivedInvitations: vi.fn(),
    createSpace: vi.fn(),
    updateSpace: vi.fn(),
    deleteSpace: vi.fn(),
    changeMemberRole: vi.fn(),
    removeMember: vi.fn(),
    transferOwnership: vi.fn(),
    leaveSpace: vi.fn(),
    inviteMember: vi.fn(),
    revokeInvitation: vi.fn(),
    acceptInvitation: vi.fn(),
  }
}

describe('useSpacesPageApi', () => {
  it('returns the injected api when used within the provider', () => {
    const api = fakeApi()
    const wrapper = ({ children }: { children: ReactNode }) => (
      <SpacesPageApiProvider api={api}>{children}</SpacesPageApiProvider>
    )
    const { result } = renderHook(() => useSpacesPageApi(), { wrapper })
    expect(result.current).toBe(api)
  })

  it('throws when used without a provider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {})
    function Consumer() {
      useSpacesPageApi()
      return null
    }
    expect(() => render(<Consumer />)).toThrow('SpacesPageApiProvider')
    spy.mockRestore()
  })
})
