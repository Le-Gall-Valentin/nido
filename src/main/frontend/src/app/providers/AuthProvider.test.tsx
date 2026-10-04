import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useAuth, type IAuthApi } from '@/features/auth'
import { clearSessionHint, setSessionHint } from '@/shared/lib'
import { AuthProvider } from './AuthProvider'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

const alice = {
  id: 'alice', username: 'alice', email: 'alice@test.com', role: 'USER' as const,
  createdAt: '2026-01-01T00:00:00Z', totpEnabled: false, language: null,
}

function fakeApi(): IAuthApi {
  return { login: vi.fn(), logout: vi.fn().mockResolvedValue(undefined), getMe: vi.fn().mockResolvedValue(alice) }
}

function SignOut() {
  const logout = useAuth((s) => s.logout)
  return <button onClick={() => void logout()}>sign out</button>
}

function renderSignedIn(queryClient: QueryClient) {
  setSessionHint()
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider api={fakeApi()}><SignOut /></AuthProvider>
    </QueryClientProvider>,
  )
}

describe('AuthProvider', () => {
  afterEach(() => clearSessionHint())

  it('forgets every cached answer when the session ends', async () => {
    const queryClient = new QueryClient()
    queryClient.setQueryData(['spaces'], [{ id: 'alice-space' }])
    renderSignedIn(queryClient)

    fireEvent.click(await screen.findByRole('button', { name: 'sign out' }))

    await waitFor(() => expect(queryClient.getQueryData(['spaces'])).toBeUndefined())
  })

  it('keeps them while the session lasts', async () => {
    const queryClient = new QueryClient()
    queryClient.setQueryData(['spaces'], [{ id: 'alice-space' }])
    renderSignedIn(queryClient)

    await screen.findByRole('button', { name: 'sign out' })

    expect(queryClient.getQueryData(['spaces'])).toEqual([{ id: 'alice-space' }])
  })
})
