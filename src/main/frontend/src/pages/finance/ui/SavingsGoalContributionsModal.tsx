import { useTranslation } from 'react-i18next'
import { Dialog } from '@/shared/ui'
import { UserAvatar } from '@/entities/user'
import type { SavingsContribution } from '@/entities/finance'
import { formatAmount } from '../lib/formatAmount'
import { safeSavingsGoalColor, safeSavingsGoalGlyph } from '../lib/savingsGoalAppearance'

interface SavingsGoalContributionsModalProps {
  goalName: string
  color: string
  glyph: string
  contributions: SavingsContribution[]
  memberLabel: (memberId: string) => string
  onClose: () => void
}

export function SavingsGoalContributionsModal({ goalName, color, glyph, contributions, memberLabel, onClose }: SavingsGoalContributionsModalProps) {
  const { t } = useTranslation('finance')

  return (
    <Dialog open onClose={onClose} title={goalName} maxWidth="max-w-lg">
      <div className="mb-4 flex items-center gap-3">
        <div className="grid size-9 shrink-0 place-items-center rounded-[10px] text-lg" style={{ background: safeSavingsGoalColor(color) }}>
          {safeSavingsGoalGlyph(glyph)}
        </div>
        <p className="text-sm text-fg-3">{t('savings.contributions_title')}</p>
      </div>

      {contributions.length === 0 ? (
        <p className="text-sm text-fg-3">{t('savings.contributions_empty')}</p>
      ) : (
        <ul className="max-h-96 space-y-2 overflow-y-auto">
          {contributions.map((contribution) => (
            <li key={contribution.id} className="flex items-center gap-2.5 text-sm">
              <UserAvatar username={memberLabel(contribution.memberId)} role="USER" className="size-7 shrink-0 rounded-full text-[10.5px]" />
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium text-fg-0">{memberLabel(contribution.memberId)}</p>
                <p className="text-xs text-fg-3">{contribution.date}</p>
              </div>
              <span className="shrink-0 font-semibold text-fg-0">{formatAmount(contribution.amount)}</span>
            </li>
          ))}
        </ul>
      )}
    </Dialog>
  )
}
