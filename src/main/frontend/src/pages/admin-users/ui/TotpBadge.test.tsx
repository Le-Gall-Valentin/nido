import { render } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { TotpBadge } from './TotpBadge'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

describe('TotpBadge', () => {
  it('renders the enabled label when totp is enabled', () => {
    const { getByText, queryByText } = render(<TotpBadge enabled />)
    expect(getByText('table.totp_on')).toBeDefined()
    expect(queryByText('table.totp_off')).toBeNull()
  })

  it('renders the disabled label when totp is disabled', () => {
    const { getByText, queryByText } = render(<TotpBadge enabled={false} />)
    expect(getByText('table.totp_off')).toBeDefined()
    expect(queryByText('table.totp_on')).toBeNull()
  })
})
