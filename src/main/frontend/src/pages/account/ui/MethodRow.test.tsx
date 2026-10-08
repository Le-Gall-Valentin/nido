import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MethodRow } from './MethodRow'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: Record<string, unknown>) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

function row(method: 'APP' | 'MAIL', enabled: boolean, usable: boolean) {
  const handlers = { onEnable: vi.fn(), onDisable: vi.fn() }
  render(<MethodRow state={{ method, enabled, usable }} email="camille@exemple.fr" busy={false} {...handlers} />)
  return handlers
}

describe('MethodRow', () => {
  it('the app on can be turned off', () => {
    const { onDisable } = row('APP', true, true)

    expect(screen.getByText('twofa.status_enabled')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'twofa.btn_disable' }))
    expect(onDisable).toHaveBeenCalled()
  })

  it('the mail off, with mail working, can be turned on and says where codes go', () => {
    const { onEnable } = row('MAIL', false, true)

    expect(screen.getByText('twofa.status_disabled')).toBeTruthy()
    expect(screen.getByText('twofa.mail_desc:{"address":"camille@exemple.fr"}')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'twofa.btn_enable' }))
    expect(onEnable).toHaveBeenCalled()
  })

  it('the mail on while mail is off is paused, and can still be turned off', () => {
    const { onDisable } = row('MAIL', true, false)

    expect(screen.getByText('twofa.status_paused')).toBeTruthy()
    expect(screen.getByText('twofa.mail_paused')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'twofa.btn_disable' }))
    expect(onDisable).toHaveBeenCalled()
  })

  it('the mail off while mail is off is unavailable, with nothing to press', () => {
    row('MAIL', false, false)

    expect(screen.getByText('twofa.status_unavailable')).toBeTruthy()
    expect(screen.getByText('twofa.mail_unavailable')).toBeTruthy()
    expect(screen.queryByRole('button')).toBeNull()
  })
})
