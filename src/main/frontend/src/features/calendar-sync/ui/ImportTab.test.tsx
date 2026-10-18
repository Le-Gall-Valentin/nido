import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { CalendarApiProvider, CalendarSyncError, type CalendarApi, type ImportReport } from '@/entities/calendar'
import { ImportTab } from './ImportTab'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string, o?: Record<string, unknown>) => (o ? `${key}${JSON.stringify(o)}` : key), i18n: { language: 'fr' } }),
}))

const ANALYSIS: ImportReport = {
  eventsCreated: 3, seriesCreated: 1, seriesUnfolded: 1, occurrencesUnfolded: 2, duplicates: 1, ignored: 1,
  notes: [{ kind: 'IGNORED', title: 'Une tâche', detail: 'VTODO' }], written: false,
}

function renderTab(api: Partial<CalendarApi>) {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <CalendarApiProvider api={api as CalendarApi}><ImportTab spaceId="s1" /></CalendarApiProvider>
    </QueryClientProvider>)
}

const ics = () => new File(['BEGIN:VCALENDAR'], 'club.ics', { type: 'text/calendar' })

describe('ImportTab', () => {
  it('analyses the file chosen, says what an import would do, then imports it', async () => {
    const analyseImport = vi.fn().mockResolvedValue(ANALYSIS)
    const importFile = vi.fn().mockResolvedValue({ ...ANALYSIS, written: true })
    renderTab({ analyseImport, importFile })
    const file = ics()

    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [file] } })

    expect(await screen.findByText('import.summary.events{"count":3}')).toBeTruthy()
    expect(screen.getByText('import.summary.duplicates{"count":1}')).toBeTruthy()
    expect(screen.getByText('import.note.IGNORED{"title":"Une tâche","detail":"import.ignored_reason.VTODO"}')).toBeTruthy()
    expect(analyseImport).toHaveBeenCalledWith('s1', file)

    fireEvent.click(screen.getByRole('button', { name: 'import.confirm{"count":4}' }))
    await waitFor(() => expect(importFile).toHaveBeenCalledWith('s1', file))
    expect(await screen.findByText('import.done')).toBeTruthy()
  })

  it('has nothing to import when the file holds nothing new', async () => {
    renderTab({ analyseImport: vi.fn().mockResolvedValue({ ...ANALYSIS, eventsCreated: 0, seriesCreated: 0, notes: [] }) })
    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [ics()] } })
    expect(await screen.findByText('import.nothing')).toBeTruthy()
    expect(screen.queryByRole('button', { name: /import\.confirm/ })).toBeNull()
  })

  it('says why a file cannot be imported', async () => {
    renderTab({ analyseImport: vi.fn().mockRejectedValue(new CalendarSyncError('not_ics')) })
    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [ics()] } })
    expect((await screen.findByRole('alert')).textContent).toBe('errors.not_ics')
  })

  it('words the notes from the totals and falls back on an unknown reason', async () => {
    const analyseImport = vi.fn().mockResolvedValue({
      ...ANALYSIS, ignored: 450, occurrencesUnfolded: 0,
      notes: [{ kind: 'IGNORED', title: null, detail: 'SOMETHING_NEW' },
        { kind: 'TRUNCATED_TEXT', title: 'Fête', detail: 'location' }],
    })
    renderTab({ analyseImport })
    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [ics()] } })
    expect(await screen.findByText('import.summary.ignored{"count":450}')).toBeTruthy()
    expect(screen.getByText('import.note.IGNORED{"title":"import.untitled","detail":"import.ignored_unknown"}')).toBeTruthy()
    expect(screen.getByText('import.note.TRUNCATED_TEXT{"title":"Fête","detail":"import.text_field.location"}')).toBeTruthy()
    expect(screen.queryByText(/import\.summary\.unfolded/)).toBeNull()
  })

  it('imports the file that was analysed, and starting over clears the report and the error', async () => {
    const analyseImport = vi.fn().mockResolvedValueOnce(ANALYSIS).mockRejectedValueOnce(new CalendarSyncError('not_ics'))
    const importFile = vi.fn().mockRejectedValue(new CalendarSyncError('rate_limited'))
    renderTab({ analyseImport, importFile })
    const first = ics()
    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [first] } })
    fireEvent.click(await screen.findByRole('button', { name: 'import.confirm{"count":4}' }))
    expect((await screen.findByRole('alert')).textContent).toBe('errors.rate_limited')
    expect(importFile).toHaveBeenCalledWith('s1', first)

    fireEvent.click(screen.getByRole('button', { name: 'import.again' }))
    await waitFor(() => expect(screen.queryByRole('alert')).toBeNull())
    expect(screen.queryByText('import.summary.events{"count":3}')).toBeNull()

    const second = new File(['x'], 'other.ics', { type: 'text/calendar' })
    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [second] } })
    expect((await screen.findByRole('alert')).textContent).toBe('errors.not_ics')
    expect(screen.queryByRole('button', { name: /import\.confirm/ })).toBeNull()
    expect(analyseImport).toHaveBeenLastCalledWith('s1', second)
  })
})
