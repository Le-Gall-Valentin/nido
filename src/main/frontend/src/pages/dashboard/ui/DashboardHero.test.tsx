import { fireEvent, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { DashboardHero } from './DashboardHero'
import { renderWithActions } from '../test/renderWithActions'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

describe('DashboardHero', () => {
  it('titles the page with the day and counts what needs attention', () => {
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={4} complete username="valentin" onAddTask={() => {}} />)

    expect(screen.getByRole('heading', { level: 1, name: 'Samedi 26 septembre' })).toBeDefined()
    expect(screen.getByText('hero.digest:{"count":4,"name":"valentin"}')).toBeDefined()
  })

  it('never says all is clear when part of the dashboard could not be read', () => {
    // A failed source (the invitations have no card to say so) may have kept items out of "À traiter".
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={0} complete={false} username="valentin" onAddTask={() => {}} />)

    expect(screen.getByText('hero.partial:{"name":"valentin"}')).toBeDefined()
    expect(screen.queryByText('hero.all_clear:{"name":"valentin"}')).toBeNull()
  })

  it('says all is clear when nothing needs attention', () => {
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={0} complete username="valentin" onAddTask={() => {}} />)
    expect(screen.getByText('hero.all_clear:{"name":"valentin"}')).toBeDefined()
  })

  it('adds a task here and sends the other creations to the page that owns them', () => {
    const onAddTask = vi.fn()
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={0} complete username="valentin" onAddTask={onAddTask} />)

    fireEvent.click(screen.getByRole('button', { name: 'hero.add' }))
    expect(screen.getByRole('menuitem', { name: 'add_menu.transaction' }).getAttribute('href')).toBe('/s/space-1/finance?create=transaction')
    expect(screen.getByRole('menuitem', { name: 'add_menu.item' }).getAttribute('href')).toBe('/s/space-1/organisation/courses?create=item')
    expect(screen.getByRole('menuitem', { name: 'add_menu.event' }).getAttribute('href')).toBe('/s/space-1/organisation/calendar?create=event')

    fireEvent.click(screen.getByRole('menuitem', { name: 'add_menu.task' }))
    expect(onAddTask).toHaveBeenCalled()
    expect(screen.queryByRole('menu')).toBeNull()
  })

  it('closes the menu on Escape', () => {
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={0} complete username="valentin" onAddTask={() => {}} />)
    fireEvent.click(screen.getByRole('button', { name: 'hero.add' }))

    fireEvent.keyDown(document, { key: 'Escape' })

    expect(screen.queryByRole('menu')).toBeNull()
  })

  it('moves through the menu with the arrows, Home and End, as a menu does', () => {
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={0} complete username="valentin" onAddTask={() => {}} />)
    const item = (name: string) => screen.getByRole('menuitem', { name })
    const press = (key: string) => fireEvent.keyDown(document.activeElement ?? document.body, { key })

    fireEvent.click(screen.getByRole('button', { name: 'hero.add' }))
    expect(document.activeElement).toBe(item('add_menu.task'))

    press('ArrowDown')
    expect(document.activeElement).toBe(item('add_menu.transaction'))
    press('End')
    expect(document.activeElement).toBe(item('add_menu.event'))
    press('ArrowDown')
    expect(document.activeElement).toBe(item('add_menu.task'))
    press('ArrowUp')
    expect(document.activeElement).toBe(item('add_menu.event'))
    press('Home')
    expect(document.activeElement).toBe(item('add_menu.task'))
  })

  it('gives the focus back to its button when Escape closes the menu', () => {
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={0} complete username="valentin" onAddTask={() => {}} />)
    const button = screen.getByRole('button', { name: 'hero.add' })
    fireEvent.click(button)

    fireEvent.keyDown(document.activeElement ?? document.body, { key: 'Escape' })

    expect(screen.queryByRole('menu')).toBeNull()
    expect(document.activeElement).toBe(button)
  })

  it('closes the menu when Tab leaves it', () => {
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={0} complete username="valentin" onAddTask={() => {}} />)
    fireEvent.click(screen.getByRole('button', { name: 'hero.add' }))

    fireEvent.keyDown(document.activeElement ?? document.body, { key: 'Tab' })

    expect(screen.queryByRole('menu')).toBeNull()
  })

  it('offers nothing to add to a viewer', () => {
    renderWithActions(<DashboardHero date="2026-09-26" attentionCount={0} complete username="valentin" onAddTask={() => {}} />, { actions: { canWrite: false } })
    expect(screen.queryByRole('button', { name: 'hero.add' })).toBeNull()
  })
})
