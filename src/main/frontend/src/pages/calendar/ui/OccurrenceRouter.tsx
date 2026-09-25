import { useTranslation } from 'react-i18next'
import { Dialog, Button, Spinner } from '@/shared/ui'
import type { SpaceMember } from '@/entities/space'
import { useTasks, useRecurringTaskSeries } from '@/entities/tasks'
import { useRecurringSeries, useCategories, useSavingsGoals, useUpdateSavingsGoal } from '@/entities/finance'
import { TaskDetailModal } from '@/widgets/task-management'
import { FinanceRecurringSeriesPanel } from '@/widgets/finance-recurring-series'
import { SavingsGoalFormModal } from '@/widgets/savings-goal'
import type { CalendarOccurrence } from '@/entities/calendar'

interface OccurrenceRouterProps {
  spaceId: string
  occurrence: CalendarOccurrence
  members: SpaceMember[]
  /** Navigates to the module that owns this item, for the sources the calendar cannot edit. */
  onOpenInModule: (occurrence: CalendarOccurrence) => void
  onClose: () => void
}

/**
 * Opens the right editor for any occurrence, whatever produced it.
 *
 * Deliberately nothing but a dispatcher: each branch renders a component that fetches its own
 * data, so this file stays readable as a list of sources rather than growing a branch's worth of
 * state each time one is added.
 */
export function OccurrenceRouter({
  spaceId, occurrence, members, onOpenInModule, onClose,
}: OccurrenceRouterProps) {
  switch (occurrence.source) {
    case 'TASK':
      return <TaskOccurrence spaceId={spaceId} occurrence={occurrence} members={members}
        onOpenInModule={onOpenInModule} onClose={onClose} />
    case 'FINANCE':
      // Both a materialized transaction and a projected one belong to a series, and the series is
      // what a reader wants to change — a single past debit is an accounting fact, not a plan.
      return <FinanceOccurrence spaceId={spaceId} members={members} onClose={onClose} />
    case 'SAVINGS':
      return <SavingsOccurrence spaceId={spaceId} occurrence={occurrence} onClose={onClose} />
    case 'MEAL':
      return <ExternalOccurrence occurrence={occurrence} onOpenInModule={onOpenInModule} onClose={onClose} />
    case 'EVENT':
      // Events are handled by the page itself, which owns the form and delete state.
      return null
  }
}

/**
 * A task occurrence. Only a materialized one has a row to open: a projected future occurrence is
 * a date computed from a series and has no task to show, so its series is what the reader is sent
 * to instead.
 */
function TaskOccurrence({
  spaceId, occurrence, members, onOpenInModule, onClose,
}: Omit<OccurrenceRouterProps, 'occurrence'> & { occurrence: CalendarOccurrence }) {
  const { t } = useTranslation('calendar')
  const { data: tasks, isPending } = useTasks(spaceId)
  const { data: series } = useRecurringTaskSeries(spaceId)

  if (!occurrence.materialized) {
    return <ProjectedNotice occurrence={occurrence} onOpenInModule={onOpenInModule} onClose={onClose} />
  }
  if (isPending) {
    return (
      <Dialog open onClose={onClose} title={t('detail.title')} showCloseButton>
        <div className="flex justify-center py-6"><Spinner /></div>
      </Dialog>
    )
  }

  const task = tasks?.find((candidate) => candidate.id === occurrence.sourceId)
  if (!task) {
    return <MissingNotice onClose={onClose} />
  }
  return (
    <TaskDetailModal
      task={task}
      series={series?.find((s) => s.id === task.recurringSeriesId) ?? null}
      members={members}
      onClose={onClose}
    />
  )
}

/** Finance, meals and savings: read-only here, with a way into the module that owns them. */
function ExternalOccurrence({
  occurrence, onOpenInModule, onClose,
}: Pick<OccurrenceRouterProps, 'occurrence' | 'onOpenInModule' | 'onClose'>) {
  const { t } = useTranslation('calendar')
  return (
    <Dialog open onClose={onClose} title={t('detail.title')} showCloseButton>
      <div className="flex flex-col gap-3">
        <div>
          <p className="pr-8 text-base font-semibold text-fg-0">{occurrence.title}</p>
          <p className="text-sm text-fg-2">
            {occurrence.startDate} · {t(`source.${occurrence.source}`)}
          </p>
        </div>
        {!occurrence.materialized && (
          <p className="text-sm text-fg-3">{t('detail.projected_hint')}</p>
        )}
        <div className="flex justify-end">
          <Button type="button" onClick={() => onOpenInModule(occurrence)}>
            {t(`detail.open_in.${occurrence.source}`)}
          </Button>
        </div>
      </div>
    </Dialog>
  )
}

function ProjectedNotice({
  occurrence, onOpenInModule, onClose,
}: Pick<OccurrenceRouterProps, 'occurrence' | 'onOpenInModule' | 'onClose'>) {
  const { t } = useTranslation('calendar')
  return (
    <Dialog open onClose={onClose} title={t('detail.title')} showCloseButton>
      <div className="flex flex-col gap-3">
        <p className="pr-8 text-base font-semibold text-fg-0">{occurrence.title}</p>
        <p className="text-sm text-fg-2">{occurrence.startDate}</p>
        <p className="text-sm text-fg-3">{t('detail.projected_hint')}</p>
        <div className="flex justify-end">
          <Button type="button" onClick={() => onOpenInModule(occurrence)}>
            {t(`detail.open_in.${occurrence.source}`)}
          </Button>
        </div>
      </div>
    </Dialog>
  )
}

function MissingNotice({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation('calendar')
  return (
    <Dialog open onClose={onClose} title={t('detail.title')} showCloseButton>
      <p className="text-sm text-fg-3">{t('detail.missing')}</p>
    </Dialog>
  )
}

/** The space's recurring money, opened on the series the occurrence came from. */
function FinanceOccurrence({
  spaceId, members, onClose,
}: { spaceId: string; members: SpaceMember[]; onClose: () => void }) {
  const { t } = useTranslation('calendar')
  const { data: series, isPending: seriesPending } = useRecurringSeries(spaceId)
  const { data: categories, isPending: categoriesPending } = useCategories(spaceId)

  if (seriesPending || categoriesPending) {
    return (
      <Dialog open onClose={onClose} title={t('detail.title')} showCloseButton>
        <div className="flex justify-center py-6"><Spinner /></div>
      </Dialog>
    )
  }
  return (
    <FinanceRecurringSeriesPanel
      spaceId={spaceId}
      series={series ?? []}
      categories={categories ?? []}
      members={members}
      canPickContributors={members.length > 1}
      onClose={onClose}
    />
  )
}

/** A savings deadline, opened on the goal's own form so the date can be moved from here. */
function SavingsOccurrence({
  spaceId, occurrence, onClose,
}: { spaceId: string; occurrence: CalendarOccurrence; onClose: () => void }) {
  const { t } = useTranslation('calendar')
  const { data: goals, isPending } = useSavingsGoals(spaceId)
  const updateGoal = useUpdateSavingsGoal(spaceId)

  if (isPending) {
    return (
      <Dialog open onClose={onClose} title={t('detail.title')} showCloseButton>
        <div className="flex justify-center py-6"><Spinner /></div>
      </Dialog>
    )
  }

  const goal = goals?.find((candidate) => candidate.id === occurrence.sourceId)
  if (!goal) {
    return <MissingNotice onClose={onClose} />
  }
  return (
    <SavingsGoalFormModal
      mode="edit"
      goal={goal}
      onSubmit={(input) => {
        updateGoal.mutate({ goalId: goal.id, ...input }, { onSuccess: onClose })
      }}
      onCancel={onClose}
      submitError={updateGoal.isError ? t('form.save_failed') : null}
    />
  )
}
