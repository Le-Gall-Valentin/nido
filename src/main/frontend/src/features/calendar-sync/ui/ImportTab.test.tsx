import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { CalendarApiProvider, CalendarSyncError, MAX_IMPORT_BYTES, type CalendarApi, type ImportReport } from '@/entities/calendar'
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

  it('keeps one status region holding the totals after an analysis and the result after an import', async () => {
    renderTab({ analyseImport: vi.fn().mockResolvedValue(ANALYSIS), importFile: vi.fn().mockResolvedValue({ ...ANALYSIS, written: true }) })
    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [ics()] } })
    const region = await screen.findByRole('status')
    expect(region.textContent).toContain('import.summary.events{"count":3}')
    expect(region.querySelector('[role="status"]')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'import.confirm{"count":4}' }))
    await waitFor(() => expect(screen.getByRole('status').textContent).toContain('import.done'))
    expect(screen.getByRole('status')).toBe(region)
  })

  it('moves focus to the status region when work starts, and back to the picker on starting over', async () => {
    renderTab({ analyseImport: vi.fn().mockResolvedValue(ANALYSIS) })
    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [ics()] } })
    await screen.findByText('import.summary.events{"count":3}')
    expect(document.activeElement).toBe(screen.getByRole('status'))
    fireEvent.click(screen.getByRole('button', { name: 'import.again' }))
    await waitFor(() => expect(document.activeElement).toBe(screen.getByLabelText('import.choose')))
  })

  const analysedWith = async (patch: Partial<ImportReport>) => {
    renderTab({ analyseImport: vi.fn().mockResolvedValue({ ...ANALYSIS, ...patch }) })
    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [ics()] } })
    await screen.findByText('import.summary.events{"count":3}')
  }

  it('shows the unfolded line when occurrences were unfolded', async () => {
    await analysedWith({})
    expect(screen.getByText('import.summary.unfolded{"count":2}')).toBeTruthy()
  })

  it('says the notes are cut when the server sent the most it sends', async () => {
    await analysedWith({ notes: Array.from({ length: 200 }, (_, i) => ({ kind: 'IGNORED' as const, title: `n${i}`, detail: 'VTODO' })) })
    expect(screen.getByText('import.notes_capped{"count":200}')).toBeTruthy()
  })

  it('does not say the notes are cut when many events are ignored but the notes are short', async () => {
    await analysedWith({ ignored: 450 })
    expect(screen.queryByText(/import\.notes_capped/)).toBeNull()
  })

  it('words a zone note that names no zone', async () => {
    await analysedWith({ notes: [{ kind: 'UNKNOWN_ZONE', title: 'b', detail: null }] })
    expect(screen.getByText('import.note.UNKNOWN_ZONE_unnamed{"title":"b","detail":""}')).toBeTruthy()
  })

  it('words a partial unfolding with nothing in the window', async () => {
    await analysedWith({ notes: [{ kind: 'UNFOLDED_PARTIAL', title: 'c', detail: '0' }] })
    expect(screen.getByText('import.note.UNFOLDED_PARTIAL_zero{"title":"c","detail":"0"}')).toBeTruthy()
  })

  it('disables the import button while the import runs', async () => {
    renderTab({ analyseImport: vi.fn().mockResolvedValue(ANALYSIS), importFile: vi.fn().mockReturnValue(new Promise(() => {})) })
    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [ics()] } })
    fireEvent.click(await screen.findByRole('button', { name: 'import.confirm{"count":4}' }))
    await waitFor(() => expect((screen.getByRole('button', { name: 'import.confirm{"count":4}' }) as HTMLButtonElement).disabled).toBe(true))
    expect(screen.getByText('import.importing')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'import.again' })).toBeNull()
  })

  it.each(['file_too_large', 'upload_too_large', 'import_too_large', 'network'] as const)('says %s', async (reason) => {
    renderTab({ analyseImport: vi.fn().mockRejectedValue(new CalendarSyncError(reason)) })
    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [ics()] } })
    expect((await screen.findByRole('alert')).textContent).toBe(`errors.${reason}`)
  })

  it('analyses a file dropped on the zone', async () => {
    const analyseImport = vi.fn().mockResolvedValue(ANALYSIS)
    renderTab({ analyseImport })
    const file = ics()

    fireEvent.drop(screen.getByTestId('import-drop'), { dataTransfer: { files: [file] } })

    await waitFor(() => expect(analyseImport).toHaveBeenCalledWith('s1', file))
  })

  it('refuses a file over the server\'s limit before sending anything', () => {
    const analyseImport = vi.fn()
    renderTab({ analyseImport })
    const big = new File(['x'], 'big.ics', { type: 'text/calendar' })
    Object.defineProperty(big, 'size', { value: MAX_IMPORT_BYTES + 1 })

    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [big] } })

    expect(screen.getByRole('alert').textContent).toBe('errors.file_too_large')
    expect(analyseImport).not.toHaveBeenCalled()
  })

  it('announces the totals and the outcome, and leaves the notes to be read at will', async () => {
    renderTab({ analyseImport: vi.fn().mockResolvedValue(ANALYSIS) })
    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [ics()] } })

    const region = await screen.findByRole('status')
    await waitFor(() => expect(region.textContent).toContain('import.summary.events'))
    expect(region.textContent).not.toContain('import.note.IGNORED')
    expect(screen.getByText(/import\.note\.IGNORED/)).toBeTruthy()
  })

  it('says the unfolded occurrences under the events, and the series unfolded', async () => {
    renderTab({ analyseImport: vi.fn().mockResolvedValue(ANALYSIS) })
    fireEvent.change(screen.getByLabelText('import.choose'), { target: { files: [ics()] } })

    const lines = (await screen.findAllByRole('listitem')).map((item) => item.textContent)
    expect(lines.slice(0, 4)).toEqual(['import.summary.events{"count":3}', 'import.summary.unfolded{"count":2}',
      'import.summary.series{"count":1}', 'import.summary.series_unfolded{"count":1}'])
  })
})
