import { useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { Calendar, Plus } from 'lucide-react'
import { Alert, Spinner } from '@/shared/ui'
import { todayIso, usePaletteItems, resolveLocale } from '@/shared/lib'
import { canWrite, isPersonal } from '@/entities/space'
import { useMySpaces, useSpaceTimezone } from '@/features/space-switcher'
import { useRescheduleOccurrence } from '@/features/reschedule-occurrence'
import {
  calendarApi, CalendarApiProvider, useOccurrences, type CalendarApi, type ScheduleChange,
} from '@/entities/calendar'
import { FinanceApiProvider, financeApi as defaultFinanceApi, type IFinanceApi } from '@/entities/finance'
import { KitchenApiProvider, kitchenApi as defaultKitchenApi, type IKitchenApi } from '@/entities/kitchen'
import { windowFor } from '../lib/calendarWindow'
import { useCalendarUrlState } from '../model/useCalendarUrlState'
import { useCalendarFilters } from '../model/useCalendarFilters'
import { useCalendarDialog } from '../model/useCalendarDialog'
import { useSwipePeriod } from '../model/useSwipePeriod'
import { useRefreshAfterWrites } from '../model/useRefreshAfterWrites'
import { formatPeriodLabel } from '../lib/periodLabel'
import { CalendarToolbar } from './CalendarToolbar'
import { CalendarDragLayer } from './CalendarDragLayer'
import { CalendarDialogs } from './CalendarDialogs'
import { MonthGrid } from './MonthGrid'
import { WeekGrid } from './WeekGrid'
import { DayAgenda } from './DayAgenda'

interface CalendarPageProps {
  api?: CalendarApi
  /**
   * The calendar opens other modules' editors — a savings goal, a finance series, a meal — so it
   * needs their APIs too. Every module page mounts its own provider; this one mounts three, and
   * forgetting one crashed the whole page the first time such an item was clicked.
   */
  financeApi?: IFinanceApi
  kitchenApi?: IKitchenApi
}

export function CalendarPage({
  api = calendarApi, financeApi = defaultFinanceApi, kitchenApi = defaultKitchenApi,
}: CalendarPageProps = {}) {
  return (
    <CalendarApiProvider api={api}>
      <FinanceApiProvider api={financeApi}>
        <KitchenApiProvider api={kitchenApi}>
          <CalendarPageContent />
        </KitchenApiProvider>
      </FinanceApiProvider>
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
  const spaceIsPersonal = currentSpace ? isPersonal(currentSpace) : false
  const { reschedule, failed: dropFailed, dismissFailure } = useRescheduleOccurrence(spaceId)
  useRefreshAfterWrites(spaceId)

  const swipe = useSwipePeriod(shiftPeriod)
  const periodLabel = formatPeriodLabel(view, date, resolveLocale(i18next.language))

  const window = useMemo(() => windowFor(view, date), [view, date])
  const { data, isPending, isError } = useOccurrences(spaceId, window.from, window.to)
  const occurrences = useMemo(() => filter(data ?? []), [filter, data])

  const dialog = useCalendarDialog()
  const { show } = dialog
  const showDay = (day: string) => show({ kind: 'day', date: day })
  const showOccurrence = (occurrence: { sourceId: string }) => show({ kind: 'occurrence', sourceId: occurrence.sourceId })
  const createOver = (schedule: ScheduleChange) =>
    show({ kind: 'create', date: schedule.startDate, returnToDay: false, schedule })

  const paletteEntries = useMemo(
    () => (canWriteHere
      ? [
          { id: 'calendar:new-event', label: t('new_event'), icon: Plus, action: () => show({ kind: 'create', date, returnToDay: false }) },
          { id: 'calendar:today', label: t('palette.today'), icon: Calendar, action: goToToday },
        ]
      : []),
    // Must be memoised: usePaletteItems re-registers whenever this array changes identity, and an
    // inline literal would loop forever.
    [canWriteHere, t, show, goToToday, date])
  usePaletteItems('calendar', paletteEntries)

  return (
    <div className="mx-auto max-w-[1100px] px-5 py-6 md:px-10 md:py-[34px]">
      <CalendarToolbar canWrite={canWriteHere} view={view} periodLabel={periodLabel}
        onShift={shiftPeriod} onToday={goToToday} onView={setView} isEnabled={isEnabled} onToggle={toggle}
        onManageSeries={() => show({ kind: 'manage-series' })}
        onNewEvent={() => show({ kind: 'create', date, returnToDay: false })} />

      {isError && <Alert variant="error" className="mb-4">{t('error')}</Alert>}
      {dropFailed && (
        <Alert variant="error" className="mb-4" onDismiss={dismissFailure} dismissLabel={t('form.cancel')}>
          {t('drag.failed')}
        </Alert>
      )}
      {isPending && <div className="flex justify-center py-10"><Spinner /></div>}

      {!isPending && !isError && (
        <div className="rounded-2xl border border-border bg-bg-1 p-1 md:p-2" {...swipe.handlers}>
          <CalendarDragLayer enabled={canWriteHere} view={view} onShift={shiftPeriod}
            onDragStart={swipe.cancel} onApply={(o, change) => { void reschedule(o, change) }}>
            {view === 'month' && (
              <MonthGrid date={date} occurrences={occurrences} today={today} canWrite={canWriteHere}
                onSelectDay={showDay} onSelectOccurrence={showOccurrence} />
            )}
            {view === 'week' && (
              <WeekGrid date={date} occurrences={occurrences} today={today} canWrite={canWriteHere}
                onSelectDay={showDay} onSelectOccurrence={showOccurrence}
                onCreateRange={canWriteHere ? createOver : undefined} />
            )}
            {view === 'day' && (
              <DayAgenda date={date} occurrences={occurrences} canWrite={canWriteHere}
                onSelectOccurrence={showOccurrence} onCreateRange={canWriteHere ? createOver : undefined} />
            )}
          </CalendarDragLayer>
        </div>
      )}

      <CalendarDialogs spaceId={spaceId} dialog={dialog} occurrences={occurrences} date={date} today={today}
        canWrite={canWriteHere} isPersonal={spaceIsPersonal} />
    </div>
  )
}
