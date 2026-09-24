import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { Calendar, ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import { Alert, Spinner } from '@/shared/ui'
import { todayIso, usePaletteItems, resolveLocale } from '@/shared/lib'
import { canWrite, isPersonal, useSpaceMembers } from '@/entities/space'
import { useAuth } from '@/features/auth'
import { useMySpaces, useSpaceTimezone } from '@/features/space-switcher'
import {
  calendarApi, CalendarApiProvider, useOccurrences, useJoinEvent, useLeaveEvent,
  type CalendarApi, type CalendarOccurrence,
} from '@/entities/calendar'
import { FinanceApiProvider, financeApi as defaultFinanceApi, type IFinanceApi } from '@/entities/finance'
import { KitchenApiProvider, kitchenApi as defaultKitchenApi, type IKitchenApi } from '@/entities/kitchen'
import { windowFor, type CalendarView } from '../lib/calendarWindow'
import type { ScheduleChange } from '../lib/dragTypes'
import { useCalendarUrlState } from '../model/useCalendarUrlState'
import { useCalendarFilters } from '../model/useCalendarFilters'
import { useSwipePeriod } from '../model/useSwipePeriod'
import { useApplyDrop } from '../model/useApplyDrop'
import { formatPeriodLabel } from '../lib/periodLabel'
import { CalendarDragLayer } from './CalendarDragLayer'
import { MonthGrid } from './MonthGrid'
import { WeekGrid } from './WeekGrid'
import { DayAgenda } from './DayAgenda'
import { SourceFilters } from './SourceFilters'
import { DayDetailModal } from './DayDetailModal'
import { OccurrenceRouter } from './OccurrenceRouter'
import { EventDetailModal } from './EventDetailModal'
import { EventFormPanel } from './EventFormPanel'
import { DeleteEventPanel } from './DeleteEventPanel'
import { TransferEventPanel } from './TransferEventPanel'
import { MealEntryModal } from './MealEntryModal'
import { OccurrenceScopeDialog, type OccurrenceScope } from './OccurrenceScopeDialog'
import { RecurringEventSeriesPanel } from './RecurringEventSeriesPanel'

const VIEWS: CalendarView[] = ['month', 'week', 'day']

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
  const { data: members } = useSpaceMembers(spaceId)
  // Whether the caller is already a participant decides between offering "join" and "leave".
  const currentUserId = useAuth((state) => state.user?.id) ?? ''
  const joinEvent = useJoinEvent(spaceId)
  const leaveEvent = useLeaveEvent(spaceId)
  const { apply: applyDrop, failed: dropFailed, dismissFailure } = useApplyDrop(spaceId)

  const swipe = useSwipePeriod(shiftPeriod)

  // Says where paging has arrived. Each view names itself at the granularity it shows.
  const periodLabel = formatPeriodLabel(view, date, resolveLocale(i18next.language))

  const window = useMemo(() => windowFor(view, date), [view, date])
  const { data, isPending, isError } = useOccurrences(spaceId, window.from, window.to)
  const occurrences = useMemo(() => filter(data ?? []), [filter, data])

  const [selectedDay, setSelectedDay] = useState<string | null>(null)
  // The id, not the object: an occurrence held in state is a snapshot, and every mutation that
  // refetches the window would leave the open modal showing what was true before the write —
  // joining an event and watching "Nobody" stay on screen is how that shows up.
  const [selectedSourceId, setSelectedSourceId] = useState<string | null>(null)
  // Where a new event starts, and whether closing its form should hand back to the day it was
  // opened from — so an event added from a day's detail is seen landing in that day.
  // `schedule` when a time was picked out in the week or day grid: the form opens on exactly it.
  const [creating, setCreating] = useState<{ date: string; returnToDay: boolean; schedule?: ScheduleChange } | null>(null)
  const createOver = (schedule: ScheduleChange) => setCreating({ date: schedule.startDate, returnToDay: false, schedule })
  const [managingSeries, setManagingSeries] = useState(false)
  const [editing, setEditing] = useState<{ occurrence: CalendarOccurrence; detachSlot: { seriesId: string; date: string } | null } | null>(null)
  const [deleting, setDeleting] = useState<CalendarOccurrence | null>(null)
  const [transferring, setTransferring] = useState<{ occurrence: CalendarOccurrence; operation: 'copy' | 'move' } | null>(null)
  const [planningMeal, setPlanningMeal] = useState<string | null>(null)
  // Set when an action landed on an occurrence of a series and the scope question is still open.
  const [pendingScope, setPendingScope] = useState<{ occurrence: CalendarOccurrence; action: 'edit' | 'delete' } | null>(null)

  /** An occurrence of a series must be asked about before it is edited or deleted. */
  const startScopedAction = (occurrence: CalendarOccurrence, action: 'edit' | 'delete') => {
    if (occurrence.seriesId && occurrence.source === 'EVENT') {
      setPendingScope({ occurrence, action })
      return
    }
    if (action === 'edit') setEditing({ occurrence, detachSlot: null })
    else setDeleting(occurrence)
  }

  const resolveScope = (scope: OccurrenceScope) => {
    if (!pendingScope) return
    const { occurrence, action } = pendingScope
    setPendingScope(null)
    if (scope === 'series') {
      // Editing or deleting the template is what the series manager is for.
      setSelectedOccurrence(null)
      setManagingSeries(true)
      return
    }
    const slot = occurrence.seriesId && occurrence.originalDate
      ? { seriesId: occurrence.seriesId, date: occurrence.originalDate }
      : null
    if (action === 'edit') setEditing({ occurrence, detachSlot: slot })
    else setDeleting(occurrence)
  }

  const selectedOccurrence = selectedSourceId === null
    ? null
    : occurrences.find((candidate) => candidate.sourceId === selectedSourceId) ?? null

  const setSelectedOccurrence = (occurrence: CalendarOccurrence | null) =>
    setSelectedSourceId(occurrence?.sourceId ?? null)

  const paletteEntries = useMemo(
    () => (canWriteHere
      ? [
          { id: 'calendar:new-event', label: t('new_event'), icon: Plus, action: () => setCreating({ date, returnToDay: false }) },
          { id: 'calendar:today', label: t('palette.today'), icon: Calendar, action: goToToday },
        ]
      : []),
    // Must be memoised: usePaletteItems re-registers whenever this array changes identity, and an
    // inline literal would loop forever.
    [canWriteHere, t, goToToday, date])
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
            <button type="button" onClick={() => setCreating({ date, returnToDay: false })}
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
          <button type="button" aria-label={t('next_period')} onClick={() => shiftPeriod(1)}
            className="grid size-8 place-items-center rounded-lg border border-border text-fg-2 hover:bg-bg-2">
            <ChevronRight className="size-4" />
          </button>
          <button type="button" onClick={goToToday}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-semibold text-fg-1 hover:bg-bg-2">
            {t('today')}
          </button>
        </div>

        {/* aria-live, because paging is what changes it and the grid gives a screen reader no
            other way to hear where it landed. */}
        <h2 aria-live="polite" className="order-first w-full text-lg font-semibold text-fg-0 sm:order-none sm:w-auto">
          {periodLabel}
        </h2>

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
      {dropFailed && (
        <Alert variant="error" className="mb-4" onDismiss={dismissFailure} dismissLabel={t('form.cancel')}>
          {t('drag.failed')}
        </Alert>
      )}
      {isPending && <div className="flex justify-center py-10"><Spinner /></div>}

      {!isPending && !isError && (
        <div className="rounded-2xl border border-border bg-bg-1 p-1 md:p-2" {...swipe.handlers}>
          <CalendarDragLayer enabled={canWriteHere} view={view} onShift={shiftPeriod}
            onDragStart={swipe.cancel} onApply={(o, change) => { void applyDrop(o, change) }}>
            {view === 'month' && (
              <MonthGrid date={date} occurrences={occurrences} today={today} canWrite={canWriteHere}
                onSelectDay={setSelectedDay} onSelectOccurrence={setSelectedOccurrence} />
            )}
            {view === 'week' && (
              <WeekGrid date={date} occurrences={occurrences} today={today} canWrite={canWriteHere}
                onSelectDay={setSelectedDay} onSelectOccurrence={setSelectedOccurrence}
                onCreateRange={canWriteHere ? createOver : undefined} />
            )}
            {view === 'day' && (
              <DayAgenda date={date} occurrences={occurrences} canWrite={canWriteHere}
                onSelectOccurrence={setSelectedOccurrence} onCreateRange={canWriteHere ? createOver : undefined} />
            )}
          </CalendarDragLayer>
        </div>
      )}

      {selectedDay !== null && (
        <DayDetailModal
          date={selectedDay}
          occurrences={occurrences}
          canWrite={canWriteHere}
          onCreateEvent={(day) => { setSelectedDay(null); setCreating({ date: day, returnToDay: true }) }}
          onSelectOccurrence={(occurrence) => { setSelectedDay(null); setSelectedOccurrence(occurrence) }}
          onClose={() => setSelectedDay(null)}
        />
      )}

      {selectedOccurrence?.source === 'EVENT' && (
        <EventDetailModal
          occurrence={selectedOccurrence}
          members={members ?? []}
          currentUserId={currentUserId}
          canWrite={canWriteHere}
          isPersonal={spaceIsPersonal}
          onEdit={() => { const o = selectedOccurrence; setSelectedOccurrence(null); startScopedAction(o, 'edit') }}
          onDelete={() => { const o = selectedOccurrence; setSelectedOccurrence(null); startScopedAction(o, 'delete') }}
          onCopy={() => { setTransferring({ occurrence: selectedOccurrence, operation: 'copy' }); setSelectedOccurrence(null) }}
          onMove={() => { setTransferring({ occurrence: selectedOccurrence, operation: 'move' }); setSelectedOccurrence(null) }}
          onJoin={() => joinEvent.mutate(selectedOccurrence.sourceId)}
          onLeave={() => leaveEvent.mutate(selectedOccurrence.sourceId)}
          onClose={() => setSelectedOccurrence(null)}
        />
      )}

      {selectedOccurrence && selectedOccurrence.source !== 'EVENT' && (
        <OccurrenceRouter
          spaceId={spaceId}
          occurrence={selectedOccurrence}
          members={members ?? []}
          onOpenInModule={(occurrence) => {
            if (occurrence.source === 'MEAL') setPlanningMeal(occurrence.startDate)
            setSelectedOccurrence(null)
          }}
          onClose={() => setSelectedOccurrence(null)}
        />
      )}

      {pendingScope && (
        <OccurrenceScopeDialog
          action={pendingScope.action}
          onChoose={resolveScope}
          onCancel={() => setPendingScope(null)}
        />
      )}

      {(creating || editing) && (
        <EventFormPanel
          spaceId={spaceId}
          occurrence={editing?.occurrence ?? null}
          detachSlot={editing?.detachSlot ?? null}
          defaultDate={creating?.date ?? date}
          defaultSchedule={creating?.schedule ?? null}
          currentUserId={currentUserId}
          members={members ?? []}
          isPersonal={spaceIsPersonal}
          onClose={() => {
            if (creating?.returnToDay) setSelectedDay(creating.date)
            setCreating(null)
            setEditing(null)
          }}
        />
      )}

      {deleting && (
        <DeleteEventPanel spaceId={spaceId} occurrence={deleting} onClose={() => setDeleting(null)} />
      )}

      {transferring && (
        <TransferEventPanel
          spaceId={spaceId}
          occurrence={transferring.occurrence}
          operation={transferring.operation}
          onClose={() => setTransferring(null)}
        />
      )}

      {planningMeal && (
        <MealEntryModal spaceId={spaceId} date={planningMeal} onClose={() => setPlanningMeal(null)} />
      )}

      {managingSeries && (
        <RecurringEventSeriesPanel spaceId={spaceId} members={members ?? []} currentUserId={currentUserId}
          isPersonal={spaceIsPersonal} onClose={() => setManagingSeries(false)} />
      )}
    </div>
  )
}
