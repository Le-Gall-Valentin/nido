import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ConfirmDeleteModal } from '@/shared/ui'
import type { SpaceMember } from '@/entities/space'
import {
  useSavingsGoals, useCreateSavingsGoal, useUpdateSavingsGoal, useDeleteSavingsGoal,
  useAddSavingsContribution, type SavingsGoal,
} from '@/entities/finance'
import { SavingsGoalsSection } from './SavingsGoalsSection'
import { SavingsGoalFormModal, type SavingsGoalFormInput } from '@/widgets/savings-goal'
import { AddContributionModal } from './AddContributionModal'
import { SavingsGoalContributionsModal } from './SavingsGoalContributionsModal'

interface SavingsGoalsPanelProps {
  spaceId: string
  spaceTimezone: string | undefined
  members: SpaceMember[]
  canWrite: boolean
  memberLabel: (memberId: string) => string
}

/**
 * The savings goals and the four things one can do with them: add one, edit one, put money in, look at
 * what has been put in — plus deleting one.
 *
 * <p>The section and its modals move together on purpose: the four slices of state are all answers to
 * "which goal, and what am I doing to it", and nothing outside this block ever asks. The page was
 * carrying them, along with four mutations, to hand them straight back down.
 */
export function SavingsGoalsPanel({ spaceId, spaceTimezone, members, canWrite, memberLabel }: SavingsGoalsPanelProps) {
  const { t } = useTranslation('finance')
  const { data: savingsGoals } = useSavingsGoals(spaceId)
  const createGoal = useCreateSavingsGoal(spaceId)
  const updateGoal = useUpdateSavingsGoal(spaceId)
  const deleteGoal = useDeleteSavingsGoal(spaceId)
  const addContribution = useAddSavingsContribution(spaceId)

  const [formState, setFormState] = useState<{ mode: 'create' } | { mode: 'edit'; goal: SavingsGoal } | null>(null)
  const [contributingTo, setContributingTo] = useState<SavingsGoal | null>(null)
  const [viewing, setViewing] = useState<SavingsGoal | null>(null)
  const [deleting, setDeleting] = useState<SavingsGoal | null>(null)

  function handleFormSubmit(input: SavingsGoalFormInput) {
    if (formState?.mode === 'edit') {
      updateGoal.mutate({ goalId: formState.goal.id, ...input }, { onSuccess: () => setFormState(null) })
      return
    }
    createGoal.mutate(input, { onSuccess: () => setFormState(null) })
  }

  return (
    <>
      <SavingsGoalsSection
        savingsGoals={savingsGoals ?? []}
        canWrite={canWrite}
        memberLabel={memberLabel}
        onCreate={() => setFormState({ mode: 'create' })}
        onEdit={(goal) => setFormState({ mode: 'edit', goal })}
        onDelete={(goalId) => setDeleting((savingsGoals ?? []).find((g) => g.id === goalId) ?? null)}
        onContribute={setContributingTo}
        onView={setViewing}
      />

      {formState && (
        <SavingsGoalFormModal
          mode={formState.mode}
          goal={formState.mode === 'edit' ? formState.goal : undefined}
          onSubmit={handleFormSubmit}
          onCancel={() => {
            setFormState(null)
            createGoal.reset()
            updateGoal.reset()
          }}
          submitError={(createGoal.isError || updateGoal.isError) ? t('form.submit_error') : null}
        />
      )}

      {contributingTo && (
        <AddContributionModal
          spaceTimezone={spaceTimezone}
          goalName={contributingTo.name}
          remaining={contributingTo.targetAmount - contributingTo.totalContributed}
          members={members}
          isPending={addContribution.isPending}
          onCancel={() => {
            setContributingTo(null)
            addContribution.reset()
          }}
          onSubmit={(input) => addContribution.mutate(
            { goalId: contributingTo.id, ...input },
            { onSuccess: () => setContributingTo(null) }
          )}
          submitError={addContribution.isError ? t('form.submit_error') : null}
        />
      )}

      {viewing && (
        <SavingsGoalContributionsModal
          goalName={viewing.name}
          color={viewing.color}
          glyph={viewing.glyph}
          contributions={viewing.contributions}
          memberLabel={memberLabel}
          onClose={() => setViewing(null)}
        />
      )}

      {deleting && (
        <ConfirmDeleteModal
          title={t('delete_confirm.title', { label: deleting.name })}
          isPending={deleteGoal.isPending}
          error={deleteGoal.isError ? t('delete_confirm.error') : null}
          onCancel={() => {
            setDeleting(null)
            deleteGoal.reset()
          }}
          onConfirm={() => deleteGoal.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        />
      )}
    </>
  )
}
