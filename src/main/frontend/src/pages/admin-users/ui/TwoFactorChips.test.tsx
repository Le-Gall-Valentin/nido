import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { TwoFactorChips } from './TwoFactorChips'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

describe('TwoFactorChips', () => {
  it('one chip per method on, app first', () => {
    render(<TwoFactorChips methods={['APP', 'MAIL']} mail="available" />)

    const chips = screen.getAllByText(/table\.two_factor_/).map(chip => chip.textContent)
    expect(chips).toEqual(['table.two_factor_app', 'table.two_factor_mail'])
  })

  it('the mail on while mail is off is paused', () => {
    render(<TwoFactorChips methods={['MAIL']} mail="unavailable" />)

    expect(screen.getByText('table.two_factor_mail_paused')).toBeTruthy()
  })

  it('no method says so', () => {
    render(<TwoFactorChips methods={[]} mail="available" />)

    expect(screen.getByText('table.two_factor_none')).toBeTruthy()
  })
})
