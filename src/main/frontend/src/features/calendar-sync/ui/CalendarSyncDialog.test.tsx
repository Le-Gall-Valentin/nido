import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { CalendarSyncDialog } from './CalendarSyncDialog'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key, i18n: { language: 'fr' } }) }))
vi.mock('./ExportTab', () => ({ ExportTab: () => <p>export-panel</p> }))
vi.mock('./SubscriptionsTab', () => ({ SubscriptionsTab: () => <p>subscribe-panel</p> }))
vi.mock('./ImportTab', () => ({ ImportTab: () => <p>import-panel</p> }))
vi.mock('./ExternalFeedsTab', () => ({ ExternalFeedsTab: () => <p>feeds-panel</p> }))

describe('CalendarSyncDialog', () => {
  it('opens on the export, and goes from tab to tab', () => {
    render(<CalendarSyncDialog spaceId="s1" canWrite isPersonal={false} onClose={vi.fn()} />)
    expect(screen.getByText('export-panel')).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: 'tabs.feeds' }))
    expect(screen.getByText('feeds-panel')).toBeTruthy()
    expect(screen.getByRole('tab', { name: 'tabs.feeds' }).getAttribute('aria-selected')).toBe('true')
  })

  it('offers no import to a viewer', () => {
    render(<CalendarSyncDialog spaceId="s1" canWrite={false} isPersonal={false} onClose={vi.fn()} />)
    expect(screen.getAllByRole('tab').map((tab) => tab.textContent)).toEqual(['tabs.export', 'tabs.subscribe', 'tabs.feeds'])
  })

  it('moves between tabs with the arrow keys, wrapping, Home and End', () => {
    render(<CalendarSyncDialog spaceId="s1" canWrite={false} isPersonal={false} onClose={vi.fn()} />)
    const tabs = () => screen.getAllByRole('tab')
    expect(tabs().map((tab) => tab.getAttribute('tabindex'))).toEqual(['0', '-1', '-1'])
    tabs()[0].focus()
    fireEvent.keyDown(tabs()[0], { key: 'ArrowRight' })
    expect(tabs()[1].getAttribute('aria-selected')).toBe('true')
    expect(document.activeElement).toBe(tabs()[1])
    fireEvent.keyDown(tabs()[1], { key: 'End' })
    expect(document.activeElement).toBe(tabs()[2])
    fireEvent.keyDown(tabs()[2], { key: 'ArrowRight' })
    expect(document.activeElement).toBe(tabs()[0])
    fireEvent.keyDown(tabs()[0], { key: 'ArrowLeft' })
    expect(document.activeElement).toBe(tabs()[2])
    fireEvent.keyDown(tabs()[2], { key: 'Home' })
    expect(tabs()[0].getAttribute('aria-selected')).toBe('true')
    expect(document.activeElement).toBe(tabs()[0])
  })

  it('lets the keyboard reach the panel itself, whose first content is text', () => {
    render(<CalendarSyncDialog spaceId="s1" canWrite isPersonal={false} onClose={vi.fn()} />)
    expect(screen.getByRole('tabpanel').getAttribute('tabindex')).toBe('0')
  })
})
