import { render, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { TotpEnrollProposal } from './TotpEnrollProposal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (k: string, opts?: Record<string, unknown>) =>
      opts ? `${k}:${JSON.stringify(opts)}` : k,
  }),
}))

describe('TotpEnrollProposal', () => {
  it('renders heading, subtitle with username, why card, activate button, skip link', () => {
    const { getByText, getByRole } = render(
      <TotpEnrollProposal username="alice" onActivate={vi.fn()} onSkip={vi.fn()} />
    )

    expect(getByText('enroll.title')).toBeTruthy()
    expect(getByText(`enroll.subtitle:${JSON.stringify({ username: 'alice' })}`)).toBeTruthy()
    expect(getByText('enroll.why_title')).toBeTruthy()
    expect(getByText('enroll.why_text')).toBeTruthy()
    expect(getByRole('button', { name: /enroll\.activate/i })).toBeTruthy()
    expect(getByText('enroll.skip')).toBeTruthy()
  })

  it('calls onActivate when activate button is clicked', () => {
    const onActivate = vi.fn()
    const { getByRole } = render(
      <TotpEnrollProposal username="alice" onActivate={onActivate} onSkip={vi.fn()} />
    )

    fireEvent.click(getByRole('button', { name: /enroll\.activate/i }))
    expect(onActivate).toHaveBeenCalledTimes(1)
  })

  it('calls onSkip when skip link is clicked', () => {
    const onSkip = vi.fn()
    const { getByText } = render(
      <TotpEnrollProposal username="alice" onActivate={vi.fn()} onSkip={onSkip} />
    )

    fireEvent.click(getByText('enroll.skip'))
    expect(onSkip).toHaveBeenCalledTimes(1)
  })

  it('does not call onActivate when skip is clicked (and vice versa)', () => {
    const onActivate = vi.fn()
    const onSkip = vi.fn()
    const { getByText, getByRole } = render(
      <TotpEnrollProposal username="alice" onActivate={onActivate} onSkip={onSkip} />
    )

    fireEvent.click(getByText('enroll.skip'))
    expect(onActivate).not.toHaveBeenCalled()

    fireEvent.click(getByRole('button', { name: /enroll\.activate/i }))
    expect(onSkip).toHaveBeenCalledTimes(1)
    expect(onActivate).toHaveBeenCalledTimes(1)
  })
})