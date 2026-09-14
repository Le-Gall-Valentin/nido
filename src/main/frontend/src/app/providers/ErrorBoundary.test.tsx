import { render, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach, type MockInstance } from 'vitest'
import { ErrorBoundary } from './ErrorBoundary'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

function Bomb({ shouldThrow = true }: { shouldThrow?: boolean }) {
  if (shouldThrow) throw new Error('test error')
  return <div>safe</div>
}

/** React logs caught errors on its own; only the line this boundary writes is this suite's business. */
function boundaryLogs(spy: MockInstance): unknown[][] {
  return spy.mock.calls.filter((call) => call[0] === '[ErrorBoundary]')
}

describe('ErrorBoundary', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders children when no error occurs', () => {
    const { getByText } = render(
      <ErrorBoundary><div>content</div></ErrorBoundary>
    )
    expect(getByText('content')).toBeDefined()
  })

  it('shows alert and retry button when a child throws', () => {
    const { getByRole, getByText } = render(
      <ErrorBoundary><Bomb /></ErrorBoundary>
    )
    expect(getByRole('alert')).toBeDefined()
    expect(getByText('error.retry')).toBeDefined()
  })

  it('reports the error in production too, not only in a dev build', () => {
    // The whole point of this finding: in production componentDidCatch threw the error away, so a
    // crash on a user's machine left nothing to go on — no message, no name, nothing to ask them for.
    vi.stubEnv('DEV', false)

    render(<ErrorBoundary><Bomb /></ErrorBoundary>)

    expect(boundaryLogs(console.error as unknown as MockInstance)).toHaveLength(1)
    vi.unstubAllEnvs()
  })

  it('logs the error itself and where it came from', () => {
    render(<ErrorBoundary><Bomb /></ErrorBoundary>)

    const [logged] = boundaryLogs(console.error as unknown as MockInstance)
    expect((logged?.[1] as Error).message).toBe('test error')
    expect(String(logged?.[2])).toContain('Bomb')
  })

  it('names the failure on screen, so a user can report it without opening a console', () => {
    // Asking a non-technical user for devtools output is how a bug stays undiagnosed. The fallback
    // carries the name and the message; that is enough to classify a crash from a screenshot.
    const { getByText } = render(<ErrorBoundary><Bomb /></ErrorBoundary>)

    expect(getByText(/Error: test error/)).toBeDefined()
  })

  it('keeps the reason out of the way until it is asked for', () => {
    // It sits in a collapsed <details>: an error message is for whoever is debugging, and the person
    // looking at the screen is first told what to do, not what broke.
    const { getByText } = render(<ErrorBoundary><Bomb /></ErrorBoundary>)

    const details = getByText(/Error: test error/).closest('details')
    expect(details).not.toBeNull()
    expect(details?.open).toBe(false)
  })

  it('shows reload button instead of retry after MAX_RETRIES retries', () => {
    const { getByText } = render(
      <ErrorBoundary><Bomb /></ErrorBoundary>
    )
    fireEvent.click(getByText('error.retry'))
    fireEvent.click(getByText('error.retry'))
    expect(getByText('error.reload')).toBeDefined()
  })
})