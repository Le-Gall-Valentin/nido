import { describe, expect, it } from 'vitest'
import { NAV_CONFIG, isNavItemVisible } from './navConfig'

const item = (id: string) => NAV_CONFIG.find((entry) => entry.id === id)!

describe('isNavItemVisible', () => {
  it('shows the instance settings to the super admin alone', () => {
    expect(isNavItemVisible(item('nav:instance-settings'), 'SUPER_ADMIN')).toBe(true)
    expect(isNavItemVisible(item('nav:instance-settings'), 'ADMIN')).toBe(false)
    expect(isNavItemVisible(item('nav:instance-settings'), 'USER')).toBe(false)
  })

  it('shows the administration to every admin and the rest to everyone', () => {
    expect(isNavItemVisible(item('nav:users'), 'ADMIN')).toBe(true)
    expect(isNavItemVisible(item('nav:users'), 'USER')).toBe(false)
    expect(isNavItemVisible(item('nav:dashboard'), undefined)).toBe(true)
  })
})
