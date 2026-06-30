import { render, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { ErrorBoundary } from './ErrorBoundary'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

function Bomb({ shouldThrow = true }: { shouldThrow?: boolean }) {
  if (shouldThrow) throw new Error('test error')
  return <div>safe</div>
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

  it('shows reload button instead of retry after MAX_RETRIES retries', () => {
    const { getByText } = render(
      <ErrorBoundary><Bomb /></ErrorBoundary>
    )
    fireEvent.click(getByText('error.retry'))
    fireEvent.click(getByText('error.retry'))
    expect(getByText('error.reload')).toBeDefined()
  })
})