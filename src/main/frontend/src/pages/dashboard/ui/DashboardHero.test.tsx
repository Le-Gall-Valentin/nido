import { fireEvent, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { DashboardHero } from './DashboardHero'
import { renderWithActions } from './cardTestHarness'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

describe('DashboardHero', () => {
  it('titles the page with the day and counts what needs attention', () => {
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={4} username="valentin" onAddTask={() => {}} />)

    expect(screen.getByRole('heading', { level: 1, name: 'Samedi 26 septembre' })).toBeDefined()
    expect(screen.getByText('hero.digest:{"count":4,"name":"valentin"}')).toBeDefined()
  })

  it('says all is clear when nothing needs attention', () => {
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={0} username="valentin" onAddTask={() => {}} />)
    expect(screen.getByText('hero.all_clear:{"name":"valentin"}')).toBeDefined()
  })

  it('adds a task here and sends the other creations to the page that owns them', () => {
    const onAddTask = vi.fn()
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={0} username="valentin" onAddTask={onAddTask} />)

    fireEvent.click(screen.getByRole('button', { name: 'hero.add' }))
    expect(screen.getByRole('menuitem', { name: 'add_menu.transaction' }).getAttribute('href')).toBe('/s/space-1/finance?create=transaction')
    expect(screen.getByRole('menuitem', { name: 'add_menu.item' }).getAttribute('href')).toBe('/s/space-1/organisation/courses?create=item')
    expect(screen.getByRole('menuitem', { name: 'add_menu.event' }).getAttribute('href')).toBe('/s/space-1/organisation/calendar?create=event')

    fireEvent.click(screen.getByRole('menuitem', { name: 'add_menu.task' }))
    expect(onAddTask).toHaveBeenCalled()
    expect(screen.queryByRole('menu')).toBeNull()
  })

  it('closes the menu on Escape', () => {
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={0} username="valentin" onAddTask={() => {}} />)
    fireEvent.click(screen.getByRole('button', { name: 'hero.add' }))

    fireEvent.keyDown(document, { key: 'Escape' })

    expect(screen.queryByRole('menu')).toBeNull()
  })

  it('offers nothing to add to a viewer', () => {
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={0} username="valentin" onAddTask={() => {}} />, { actions: { canWrite: false } })
    expect(screen.queryByRole('button', { name: 'hero.add' })).toBeNull()
  })
})
