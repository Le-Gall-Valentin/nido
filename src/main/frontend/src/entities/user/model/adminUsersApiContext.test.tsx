import { render, renderHook } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { AdminUsersApiProvider, useAdminUsersApi } from './adminUsersApiContext'
import type { IAdminUsersApi } from './IAdminUsersApi'

function fakeApi(): IAdminUsersApi {
  return {
    listUsers: vi.fn(),
    createUser: vi.fn(),
    updateUserRole: vi.fn(),
    activateUser: vi.fn(),
    deactivateUser: vi.fn(),
    resetTotp: vi.fn(),
    deleteUser: vi.fn(),
  }
}

describe('useAdminUsersApi', () => {
  it('returns the injected api when used within the provider', () => {
    const api = fakeApi()
    const wrapper = ({ children }: { children: ReactNode }) => (
      <AdminUsersApiProvider api={api}>{children}</AdminUsersApiProvider>
    )
    const { result } = renderHook(() => useAdminUsersApi(), { wrapper })
    expect(result.current).toBe(api)
  })

  it('throws when used without a provider', () => {
    // Silence the expected React error boundary log for this render.
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {})
    function Consumer() {
      useAdminUsersApi()
      return null
    }
    expect(() => render(<Consumer />)).toThrow('AdminUsersApiProvider')
    spy.mockRestore()
  })
})
