import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { CalendarApiProvider, CalendarSyncError, type CalendarApi } from '@/entities/calendar'
import { ExportTab } from './ExportTab'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key, i18n: { language: 'fr' } }) }))
const saveBlob = vi.fn()
vi.mock('@/shared/lib', async (original) => ({ ...(await original<object>()), saveBlob: (...args: unknown[]) => saveBlob(...args) }))

function renderTab(api: Partial<CalendarApi>, isPersonal = false) {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <CalendarApiProvider api={api as CalendarApi}><ExportTab spaceId="s1" isPersonal={isPersonal} /></CalendarApiProvider>
    </QueryClientProvider>)
}

describe('ExportTab', () => {
  it('downloads the events by default, and the sources ticked', async () => {
    const blob = new Blob(['x'])
    const exportCalendar = vi.fn().mockResolvedValue({ blob, filename: 'nido.ics' })
    renderTab({ exportCalendar })

    fireEvent.click(screen.getByLabelText('sources.TASK'))
    fireEvent.click(screen.getByLabelText('only_mine'))
    fireEvent.click(screen.getByRole('button', { name: 'export.download' }))

    await waitFor(() => expect(saveBlob).toHaveBeenCalledWith(blob, 'nido.ics'))
    expect(exportCalendar).toHaveBeenCalledWith('s1', ['EVENT', 'TASK'], true)
  })

  it('holds the button while the file is made, focus kept on it, and makes it once', async () => {
    const exportCalendar = vi.fn().mockReturnValue(new Promise(() => {}))
    renderTab({ exportCalendar })
    const download = screen.getByRole('button', { name: 'export.download' }) as HTMLButtonElement
    download.focus()
    fireEvent.click(download)
    await waitFor(() => expect(download.getAttribute('aria-disabled')).toBe('true'))
    expect(download.disabled).toBe(false)
    expect(document.activeElement).toBe(download)
    fireEvent.click(download)
    expect(exportCalendar).toHaveBeenCalledTimes(1)
  })

  it('cannot download nothing', () => {
    renderTab({ exportCalendar: vi.fn() })
    fireEvent.click(screen.getByLabelText('sources.EVENT'))
    expect((screen.getByRole('button', { name: 'export.download' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('has nobody else to leave out in a personal space', () => {
    renderTab({ exportCalendar: vi.fn() }, true)
    expect(screen.queryByLabelText('only_mine')).toBeNull()
  })

  it('says why an export failed, and takes it back once the choice changes', async () => {
    renderTab({ exportCalendar: vi.fn().mockRejectedValue(new CalendarSyncError('rate_limited')) })
    fireEvent.click(screen.getByRole('button', { name: 'export.download' }))

    expect((await screen.findByRole('alert')).textContent).toBe('errors.rate_limited')
    fireEvent.click(screen.getByLabelText('sources.TASK'))
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('says the file is being prepared', async () => {
    renderTab({ exportCalendar: vi.fn(() => new Promise<{ blob: Blob; filename: string }>(() => {})) })
    fireEvent.click(screen.getByRole('button', { name: 'export.download' }))

    await waitFor(() => expect(screen.getByRole('status').textContent).toBe('export.preparing'))
  })
})
