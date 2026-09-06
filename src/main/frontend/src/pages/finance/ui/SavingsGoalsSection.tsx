import { useTranslation } from 'react-i18next'
import { CheckCircle2, Pencil, Plus, Trash2 } from 'lucide-react'
import { UserAvatar } from '@/entities/user'
import type { SavingsGoal } from '@/entities/finance'
import { formatAmount } from '../lib/formatAmount'
import { safeSavingsGoalColor, safeSavingsGoalGlyph } from '../lib/savingsGoalAppearance'

interface SavingsGoalsSectionProps {
  savingsGoals: SavingsGoal[]
  canWrite: boolean
  memberLabel: (memberId: string) => string
  onCreate: () => void
  onEdit: (goal: SavingsGoal) => void
  onDelete: (goalId: string) => void
  onContribute: (goal: SavingsGoal) => void
  onView: (goal: SavingsGoal) => void
}

export function SavingsGoalsSection({
  savingsGoals, canWrite, memberLabel, onCreate, onEdit, onDelete, onContribute, onView,
}: SavingsGoalsSectionProps) {
  const { t } = useTranslation('finance')

  return (
    <section className="mt-4 rounded-2xl border border-border bg-bg-1 p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-[15px] font-semibold text-fg-0">{t('savings.title')}</h2>
          <p className="mt-0.5 text-xs text-fg-3">{t('savings.subtitle')}</p>
        </div>
        {canWrite && (
          <button
            type="button"
            onClick={onCreate}
            className="flex shrink-0 items-center gap-1.5 rounded-full bg-accent-dim px-3 py-1.5 text-sm font-semibold text-accent"
          >
            <Plus size={16} />
            {t('savings.new_goal')}
          </button>
        )}
      </div>

      {savingsGoals.length === 0 ? (
        <p className="py-6 text-center text-sm text-fg-3">{t('savings.empty')}</p>
      ) : (
        <div className="grid grid-cols-[repeat(auto-fit,minmax(min(260px,100%),1fr))] gap-3.5">
          {savingsGoals.map((goal) => {
            const percent = goal.targetAmount > 0 ? Math.min(100, (goal.totalContributed / goal.targetAmount) * 100) : 0
            const done = percent >= 100
            const contributors = [...new Map(goal.contributions.map((c) => [c.memberId, c])).values()].slice(0, 4)
            return (
              <div key={goal.id} className="relative rounded-[14px] border border-border transition-colors hover:bg-bg-2">
                <button
                  type="button"
                  onClick={() => onView(goal)}
                  aria-label={t('savings.view_details', { name: goal.name })}
                  className="absolute inset-0 rounded-[14px]"
                />
                <div className="pointer-events-none p-4">
                  <div className="flex items-start gap-3">
                    <div className="grid size-10 shrink-0 place-items-center rounded-[11px] text-xl" style={{ background: safeSavingsGoalColor(goal.color) }}>
                      {safeSavingsGoalGlyph(goal.glyph)}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold text-fg-0">{goal.name}</p>
                      <p className="truncate text-xs text-fg-3">
                        {t('savings.progress', { contributed: formatAmount(goal.totalContributed), target: formatAmount(goal.targetAmount) })}
                      </p>
                    </div>
                    {canWrite && (
                      <div className="pointer-events-auto flex shrink-0 gap-1">
                        <button
                          type="button"
                          onClick={() => onEdit(goal)}
                          aria-label={t('savings.edit')}
                          className="grid size-7 place-items-center rounded-md text-fg-3 hover:bg-bg-3 hover:text-fg-1"
                        >
                          <Pencil size={14} />
                        </button>
                        <button
                          type="button"
                          onClick={() => onDelete(goal.id)}
                          aria-label={t('savings.delete')}
                          className="grid size-7 place-items-center rounded-md text-fg-3 hover:bg-bg-3 hover:text-status-red"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    )}
                  </div>

                  <div className="mt-3 h-[9px] overflow-hidden rounded-full bg-bg-2">
                    <div
                      className={`h-full rounded-full transition-all ${done ? 'bg-status-green' : ''}`}
                      style={{ width: `${percent}%`, background: done ? undefined : safeSavingsGoalColor(goal.color) }}
                    />
                  </div>

                  <div className="mt-2.5 flex items-center justify-between gap-2">
                    <div className="flex items-center">
                      {contributors.map((contribution) => (
                        <UserAvatar
                          key={contribution.memberId}
                          username={memberLabel(contribution.memberId)}
                          role="USER"
                          className="-mr-1.5 size-[22px] rounded-full text-[8.5px] ring-2 ring-bg-1"
                        />
                      ))}
                      {done ? (
                        <span className="ml-2 flex items-center gap-1 text-xs font-semibold text-status-green">
                          <CheckCircle2 size={14} />
                          {t('savings.done')}
                        </span>
                      ) : (
                        <span className="ml-2 text-xs font-semibold text-fg-3">{Math.round(percent)}%</span>
                      )}
                    </div>
                    {canWrite && !done && (
                      <button
                        type="button"
                        onClick={() => onContribute(goal)}
                        className="pointer-events-auto shrink-0 rounded-full bg-accent px-3 py-1 text-xs font-semibold text-white"
                      >
                        {t('savings.contribute')}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </section>
  )
}
