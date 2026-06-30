import { render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setSessionExpiredCallback, hasSessionHint } from '@/shared/lib'
import { AuthStoreProvider } from './AuthStoreProvider'

vi.mock('@/shared/lib', () => ({
  setSessionExpiredCallback: vi.fn(),
  setSessionHint: vi.fn(),
  clearSessionHint: vi.fn(),
  hasSessionHint: vi.fn(),
  triggerSessionExpired: vi.fn(),
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

const mockedSetSessionExpiredCallback = vi.mocked(setSessionExpiredCallback)
const mockedHasSessionHint = vi.mocked(hasSessionHint)

function createApiMock() {
  return {
    login: vi.fn(),
    logout: vi.fn(),
    getMe: vi.fn(),
  }
}

beforeEach(() => {
  vi.clearAllMocks()
})

// cleanup is already registered globally in test-setup.ts

describe('AuthStoreProvider', () => {
  it('renders Spinner while isInitializing', () => {
    const api = createApiMock()
    // getMe never resolves → isInitializing stays true
    api.getMe.mockReturnValue(new Promise(() => {}))
    mockedHasSessionHint.mockReturnValue(true)

    render(
      <AuthStoreProvider api={api}>
        <div>content</div>
      </AuthStoreProvider>,
    )

    expect(screen.queryByRole('status')).not.toBeNull()
    expect(screen.queryByText('content')).toBeNull()
  })

  it('renders children when not initializing', async () => {
    const api = createApiMock()
    // No session hint → initialize skips getMe and sets isInitializing: false immediately
    mockedHasSessionHint.mockReturnValue(false)

    render(
      <AuthStoreProvider api={api}>
        <div>content</div>
      </AuthStoreProvider>,
    )

    await waitFor(() => {
      expect(screen.queryByRole('status')).toBeNull()
    })
    expect(screen.queryByText('content')).not.toBeNull()
  })

  it('calls setSessionExpiredCallback with a function on mount and null on unmount', () => {
    const api = createApiMock()
    mockedHasSessionHint.mockReturnValue(false)

    const { unmount } = render(
      <AuthStoreProvider api={api}>
        <div>content</div>
      </AuthStoreProvider>,
    )

    expect(mockedSetSessionExpiredCallback).toHaveBeenCalledWith(expect.any(Function))

    unmount()

    expect(mockedSetSessionExpiredCallback).toHaveBeenLastCalledWith(null)
  })

  it('does not call api.getMe when there is no session hint', async () => {
    const api = createApiMock()
    mockedHasSessionHint.mockReturnValue(false)

    render(
      <AuthStoreProvider api={api}>
        <div>content</div>
      </AuthStoreProvider>,
    )

    await waitFor(() => {
      expect(screen.queryByRole('status')).toBeNull()
    })

    expect(api.getMe).not.toHaveBeenCalled()
  })

  it('does not call logout when session expired fires after unmount', async () => {
    const api = createApiMock()
    mockedHasSessionHint.mockReturnValue(false)

    let capturedCallback: (() => void) | null = null
    mockedSetSessionExpiredCallback.mockImplementation((cb) => { capturedCallback = cb })

    const { unmount } = render(
      <AuthStoreProvider api={api}>
        <div>content</div>
      </AuthStoreProvider>,
    )

    await waitFor(() => { expect(capturedCallback).not.toBeNull() })
    const staleCallback = capturedCallback!

    unmount()

    staleCallback()

    expect(api.logout).not.toHaveBeenCalled()
  })

  it('calls api.getMe when there is a session hint', async () => {
    const api = createApiMock()
    api.getMe.mockResolvedValue({ id: '1', username: 'alice', role: 'USER' as const })
    mockedHasSessionHint.mockReturnValue(true)

    render(
      <AuthStoreProvider api={api}>
        <div>content</div>
      </AuthStoreProvider>,
    )

    await waitFor(() => {
      expect(screen.queryByRole('status')).toBeNull()
    })

    expect(api.getMe).toHaveBeenCalledOnce()
    expect(screen.queryByText('content')).not.toBeNull()
  })

  it('when getMe rejects, isInitializing becomes false and content is rendered', async () => {
    const api = createApiMock()
    api.getMe.mockRejectedValue(new Error('network error'))
    mockedHasSessionHint.mockReturnValue(true)

    render(
      <AuthStoreProvider api={api}>
        <div>content</div>
      </AuthStoreProvider>,
    )

    await waitFor(() => {
      expect(screen.queryByRole('status')).toBeNull()
    })
    expect(screen.getByText('content')).not.toBeNull()
  })
})