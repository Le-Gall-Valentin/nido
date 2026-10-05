import { render, screen } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ISetupApi } from '@/pages/setup'
import { createTestQueryClient } from '@/shared/test'
import { SetupGate } from './SetupGate'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

function open(status: ISetupApi['status']) {
  const api: ISetupApi = { status, verifyCode: vi.fn(), encryptionKey: vi.fn(), testMail: vi.fn(), complete: vi.fn() }
  render(
    <QueryClientProvider client={createTestQueryClient()}>
      <SetupGate api={api} renderSetup={() => <p>setup screen</p>}>
        <p>the application</p>
      </SetupGate>
    </QueryClientProvider>,
  )
}

describe('SetupGate', () => {
  beforeEach(() => window.history.pushState({}, '', '/s/123/finance'))

  it('leads every address to the setup screen while the installation is not set up', async () => {
    open(async () => ({ required: true, lockedPublicUrl: null, mailLocked: false }))

    expect(await screen.findByText('setup screen')).not.toBeNull()
    expect(window.location.pathname).toBe('/setup')
    expect(screen.queryByText('the application')).toBeNull()
  })

  it('renders the application once set up', async () => {
    open(async () => ({ required: false, lockedPublicUrl: null, mailLocked: false }))

    expect(await screen.findByText('the application')).not.toBeNull()
  })

  it('renders the application when the question cannot be answered', async () => {
    open(async () => { throw new Error('offline') })

    expect(await screen.findByText('the application', {}, { timeout: 3000 })).not.toBeNull()
  })

  it('waits while it does not know', () => {
    open(() => new Promise(() => {}))

    expect(screen.queryByText('the application')).toBeNull()
    expect(screen.queryByText('setup screen')).toBeNull()
  })
})
