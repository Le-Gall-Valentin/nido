import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Calendar, ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import { Alert, Spinner } from '@/shared/ui'
import { todayIso, usePaletteItems } from '@/shared/lib'
import { canWrite } from '@/entities/space'
import { useMySpaces, useSpaceTimezone } from '@/features/space-switcher'
import {
  calendarApi, CalendarApiProvider, useOccurrences,
  type CalendarApi, type CalendarOccurrence,
} from '@/entities/calendar'
import { windowFor, type CalendarView } from '../lib/calendarWindow'
import { useCalendarUrlState } from '../model/useCalendarUrlState'
import { useCalendarFilters } from '../model/useCalendarFilters'
import { MonthGrid } from './MonthGrid'
import { WeekGrid } from './WeekGrid'
import { DayAgenda } from './DayAgenda'
import { SourceFilters } from './SourceFilters'

const VIEWS: CalendarView[] = ['month', 'week', 'day']

interface CalendarPageProps {
  api?: CalendarApi
}

export function CalendarPage({ api = calendarApi }: CalendarPageProps = {}) {
  return (
    <CalendarApiProvider api={api}>
      <CalendarPageContent />
    </CalendarApiProvider>
  )
}

function CalendarPageContent() {
  const { t } = useTranslation('calendar')
  const { spaceId = '' } = useParams<{ spaceId: string }>()

  // The household's date, not the browser's: a calendar anchored on the reader's timezone would
  // highlight the wrong "today" for anyone travelling.
  const today = todayIso(new Date(), useSpaceTimezone(spaceId))

  const { view, date, setView, goToToday, shiftPeriod } = useCalendarUrlState(today)
  const { isEnabled, toggle, filter } = useCalendarFilters(spaceId)

  const { data: mySpaces } = useMySpaces()
  const currentSpace = mySpaces?.find((space) => space.id === spaceId)
  // Defaults to false while the role is unresolved, so a write affordance never flashes visible
  // before disappearing — the correction already made once on the recipes page.
  const canWriteHere = currentSpace ? canWrite(currentSpace.myRole) : false

  const window = useMemo(() => windowFor(view, date), [view, date])
  const { data, isPending, isError } = useOccurrences(spaceId, window.from, window.to)
  const occurrences = useMemo(() => filter(data ?? []), [filter, data])

  const [selectedDay, setSelectedDay] = useState<string | null>(null)
  const [selectedOccurrence, setSelectedOccurrence] = useState<CalendarOccurrence | null>(null)
  const [creatingEvent, setCreatingEvent] = useState(false)
  const [managingSeries, setManagingSeries] = useState(false)

  const paletteEntries = useMemo(
    () => (canWriteHere
      ? [
          { id: 'calendar:new-event', label: t('new_event'), icon: Plus, action: () => setCreatingEvent(true) },
          { id: 'calendar:today', label: t('palette.today'), icon: Calendar, action: goToToday },
        ]
      : []),
    // Must be memoised: usePaletteItems re-registers whenever this array changes identity, and an
    // inline literal would loop forever.
    [canWriteHere, t, goToToday])
  usePaletteItems('calendar', paletteEntries)

  return (
    <div className="mx-auto max-w-[1100px] px-5 py-6 md:px-10 md:py-[34px]">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-fg-0">{t('title')}</h1>
        {canWriteHere && (
          <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row sm:flex-wrap sm:items-center">
            <button type="button" onClick={() => setManagingSeries(true)}
              className="text-center text-sm font-semibold text-accent sm:text-left">
              {t('recurring_series.manage')}
            </button>
            <button type="button" onClick={() => setCreatingEvent(true)}
              className="flex w-full items-center justify-center gap-1.5 rounded-[10px] bg-accent px-4 py-2.5 text-sm font-semibold text-white sm:w-auto">
              <Plus className="size-4" /> {t('new_event')}
            </button>
          </div>
        )}
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-2">
        <div className="flex items-center gap-1">
          <button type="button" aria-label={t('previous_period')} onClick={() => shiftPeriod(-1)}
            className="grid size-8 place-items-center rounded-lg border border-border text-fg-2 hover:bg-bg-2">
            <ChevronLeft className="size-4" />
          </button>
          <button type="button" onClick={goToToday}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-semibold text-fg-1 hover:bg-bg-2">
            {t('today')}
          </button>
          <button type="button" aria-label={t('next_period')} onClick={() => shiftPeriod(1)}
            className="grid size-8 place-items-center rounded-lg border border-border text-fg-2 hover:bg-bg-2">
            <ChevronRight className="size-4" />
          </button>
        </div>

        <div role="group" className="flex w-full rounded-lg border border-border sm:w-auto">
          {VIEWS.map((candidate) => (
            <button
              key={candidate}
              type="button"
              aria-pressed={view === candidate}
              onClick={() => setView(candidate)}
              className={`flex-1 px-3 py-1.5 text-sm font-semibold first:rounded-l-lg last:rounded-r-lg sm:flex-none
                ${view === candidate ? 'bg-accent text-white' : 'text-fg-2 hover:bg-bg-2'}`}
            >
              {t(`view.${candidate}`)}
            </button>
          ))}
        </div>

        <div className="w-full sm:ml-auto sm:w-auto">
          <SourceFilters isEnabled={isEnabled} onToggle={toggle} />
        </div>
      </div>

      {isError && <Alert variant="error" className="mb-4">{t('error')}</Alert>}
      {isPending && <div className="flex justify-center py-10"><Spinner /></div>}

      {!isPending && !isError && (
        <div className="rounded-2xl border border-border bg-bg-1 p-1 md:p-2">
          {view === 'month' && (
            <MonthGrid date={date} occurrences={occurrences} today={today}
              onSelectDay={setSelectedDay} onSelectOccurrence={setSelectedOccurrence} />
          )}
          {view === 'week' && (
            <WeekGrid date={date} occurrences={occurrences} today={today}
              onSelectDay={setSelectedDay} onSelectOccurrence={setSelectedOccurrence} />
          )}
          {view === 'day' && (
            <DayAgenda date={date} occurrences={occurrences} onSelectOccurrence={setSelectedOccurrence} />
          )}
        </div>
      )}

      {/* Detail and form modals arrive in the next tasks; the state they read is already here. */}
      {selectedDay !== null && <span data-testid="pending-day-detail" hidden>{selectedDay}</span>}
      {selectedOccurrence !== null && <span data-testid="pending-occurrence-detail" hidden>{selectedOccurrence.title}</span>}
      {creatingEvent && <span data-testid="pending-event-form" hidden />}
      {managingSeries && <span data-testid="pending-series-manager" hidden />}
    </div>
  )
}
