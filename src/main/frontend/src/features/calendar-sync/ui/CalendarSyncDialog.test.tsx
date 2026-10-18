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
})
