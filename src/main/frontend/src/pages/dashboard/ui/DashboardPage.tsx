import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Alert } from '@/shared/ui'
import { useAuth } from '@/features/auth'
import { useSpaceTimezone } from '@/features/space-switcher'
import { useSpaceMembers } from '@/entities/space'
import { dashboardApi, DashboardApiProvider, useDashboard, type IDashboardApi } from '@/entities/dashboard'
import { FinanceApiProvider, financeApi as defaultFinanceApi, type IFinanceApi } from '@/entities/finance'
import { KitchenApiProvider, kitchenApi as defaultKitchenApi, type IKitchenApi } from '@/entities/kitchen'
import { TaskFormPanel } from '@/widgets/task-management'
import { DashboardActionsProvider, type DashboardActions, type PendingSettlement } from '../model/dashboardActions'
import { useRefreshDashboardAfterWrites } from '../model/useRefreshDashboardAfterWrites'
import { useNow } from '../lib/useNow'
import { DashboardSkeleton } from './DashboardSkeleton'
import { DashboardHero } from './DashboardHero'
import { DashboardBoard } from './DashboardBoard'
import { SettleDebtFlow } from './SettleDebtFlow'

interface DashboardPageProps {
  api?: IDashboardApi
  /** Settling a debt writes through the finance API; the page mounts its provider, as the calendar does. */
  financeApi?: IFinanceApi
  /** "Envoyer la semaine aux courses" reads the week's shopping list through the kitchen API. */
  kitchenApi?: IKitchenApi
}

export function DashboardPage({ api = dashboardApi, financeApi = defaultFinanceApi, kitchenApi = defaultKitchenApi }: DashboardPageProps = {}) {
  return (
    <DashboardApiProvider api={api}>
      <FinanceApiProvider api={financeApi}>
        <KitchenApiProvider api={kitchenApi}>
          <DashboardPageContent />
        </KitchenApiProvider>
      </FinanceApiProvider>
    </DashboardApiProvider>
  )
}

function PageFrame({ children }: { children: ReactNode }) {
  return <div className="mx-auto max-w-[1100px] px-4 py-6 md:px-10 md:py-[34px]">{children}</div>
}

function DashboardPageContent() {
  const { t } = useTranslation('dashboard')
  const { spaceId = '' } = useParams<{ spaceId: string }>()
  const user = useAuth((s) => s.user)
  const { data: members } = useSpaceMembers(spaceId)
  const { data: dashboard, isPending, refetch } = useDashboard(spaceId)
  const now = useNow(useSpaceTimezone(spaceId))
  useRefreshDashboardAfterWrites(spaceId)

  const [settling, setSettling] = useState<PendingSettlement | null>(null)
  const [addingTask, setAddingTask] = useState(false)
  const [actionFailed, setActionFailed] = useState(false)

  const memberName = useCallback(
    (memberId: string) => members?.find((member) => member.userId === memberId)?.username ?? t('member_unknown'),
    [members, t])

  const actions = useMemo<DashboardActions>(() => ({
    spaceId,
    // The server's word, not the spaces list's: the response says whether this caller may write here.
    canWrite: dashboard?.canWrite ?? false,
    isShared: dashboard?.spaceType === 'SHARED',
    currentUserId: user?.id ?? null,
    memberName,
    settle: setSettling,
    reportError: () => setActionFailed(true),
  }), [spaceId, dashboard?.canWrite, dashboard?.spaceType, user?.id, memberName])

  if (isPending) return <PageFrame><DashboardSkeleton /></PageFrame>
  if (!dashboard) {
    return (
      <PageFrame>
        <Alert variant="error" className="mb-3">{t('error.load_failed')}</Alert>
        <button type="button" onClick={() => void refetch()}
          className="rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-2 text-sm font-semibold text-fg-2 hover:bg-bg-2">
          {t('error.retry')}
        </button>
      </PageFrame>
    )
  }

  return (
    <DashboardActionsProvider value={actions}>
      <PageFrame>
        <DashboardHero date={dashboard.date} attentionCount={dashboard.attention.length} complete={dashboard.complete} username={user?.username ?? ''}
          onAddTask={() => setAddingTask(true)} />
        {actionFailed && (
          <Alert variant="error" className="mb-4" onDismiss={() => setActionFailed(false)} dismissLabel={t('error.dismiss')}>
            {t('error.action_failed')}
          </Alert>
        )}
        <DashboardBoard dashboard={dashboard} now={now} />
      </PageFrame>
      {settling && <SettleDebtFlow debt={settling} onClose={() => setSettling(null)} />}
      {addingTask && (
        <TaskFormPanel spaceId={spaceId} task={null} members={members ?? []} isPersonal={dashboard.spaceType === 'PERSONAL'}
          onClose={() => setAddingTask(false)} />
      )}
    </DashboardActionsProvider>
  )
}
