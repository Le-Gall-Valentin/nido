import { describe, it, expect, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { createElement } from 'react'
import { useBreadcrumbs } from './useBreadcrumbs'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

function wrapper(initialPath: string) {
  return ({ children }: { children: React.ReactNode }) =>
    createElement(MemoryRouter, { initialEntries: [initialPath] }, children)
}

function crumbs(path: string) {
  const { result } = renderHook(() => useBreadcrumbs(), { wrapper: wrapper(path) })
  return result.current
}

describe('useBreadcrumbs — administration section', () => {
  it('/administration returns section root only', () => {
    expect(crumbs('/administration')).toEqual([
      { label: 'breadcrumb.administration', to: '/administration' },
    ])
  })

  it('/administration/users', () => {
    expect(crumbs('/administration/users')).toEqual([
      { label: 'breadcrumb.administration', to: '/administration' },
      { label: 'breadcrumb.admin_users' },
    ])
  })

  it('/administration with unknown sub-route falls back to the raw segment', () => {
    const result = crumbs('/administration/unknown')
    expect(result[1]).toEqual({ label: 'unknown' })
  })
})

describe('useBreadcrumbs — other routes', () => {
  it('/account returns standalone label', () => {
    expect(crumbs('/account')).toEqual([{ label: 'breadcrumb.account' }])
  })

  it('unknown path returns empty array', () => {
    expect(crumbs('/unknown/path')).toEqual([])
  })
})