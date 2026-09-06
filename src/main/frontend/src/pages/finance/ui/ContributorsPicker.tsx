import { useTranslation } from 'react-i18next'
import { UserAvatar } from '@/entities/user'
import type { SpaceMember } from '@/entities/space'

const SELECT_CLASSNAME = 'rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] text-fg-0 outline-none focus:border-accent'

interface ContributorsPickerProps {
  members: SpaceMember[]
  payerId: string
  onPayerChange: (memberId: string) => void
  contributorIds: string[]
  onToggleContributor: (memberId: string) => void
  customizeShares: boolean
  onCustomizeSharesChange: (value: boolean) => void
  customShares: Record<string, number>
  onCustomShareChange: (memberId: string, value: number) => void
}

/** Payer select + contributor toggles + optional custom-share amounts, shared by TransactionFormModal and RecurringSeriesFormModal. */
export function ContributorsPicker({
  members, payerId, onPayerChange, contributorIds, onToggleContributor,
  customizeShares, onCustomizeSharesChange, customShares, onCustomShareChange,
}: ContributorsPickerProps) {
  const { t } = useTranslation('finance')

  return (
    <>
      <div className="flex flex-col gap-1.5">
        <label htmlFor="finance-payer" className="text-[13px] font-semibold text-fg-1">{t('form.payer_label')}</label>
        <select id="finance-payer" value={payerId} onChange={(e) => onPayerChange(e.target.value)} className={SELECT_CLASSNAME}>
          {members.map((m) => <option key={m.userId} value={m.userId}>{m.username ?? m.email}</option>)}
        </select>
      </div>

      <div className="flex flex-col gap-1.5">
        <span className="text-[13px] font-semibold text-fg-1">{t('form.contributors_label')}</span>
        <div className="flex flex-col gap-1">
          {members.map((member) => (
            <button key={member.userId} type="button" onClick={() => onToggleContributor(member.userId)}
              className={`flex items-center gap-2 rounded-[9px] p-1.5 text-left text-sm ${contributorIds.includes(member.userId) ? 'bg-accent-dim' : 'hover:bg-bg-2'}`}>
              <UserAvatar username={member.username ?? '?'} role="USER" className="size-6 rounded-full text-[10px]" />
              <span className="text-fg-1">{member.username ?? member.email}</span>
            </button>
          ))}
        </div>
      </div>

      {contributorIds.length > 0 && (
        <label className="flex items-center gap-2 text-sm font-semibold text-fg-1">
          <input type="checkbox" checked={customizeShares} onChange={(e) => onCustomizeSharesChange(e.target.checked)} />
          {t('form.customize_shares_label')}
        </label>
      )}
      {customizeShares && contributorIds.map((id) => {
        const member = members.find((m) => m.userId === id)
        return (
          <div key={id} className="flex items-center gap-2">
            <span className="w-32 text-sm text-fg-1">{member?.username ?? member?.email}</span>
            <input type="number" step="0.01" value={customShares[id] ?? ''}
              onChange={(e) => onCustomShareChange(id, Number(e.target.value))}
              className="w-24 rounded-[8px] border-[1.5px] border-border bg-bg-1 px-2 py-1 text-sm text-fg-0 outline-none focus:border-accent" />
          </div>
        )
      })}
    </>
  )
}
