import { useTranslation } from 'react-i18next'
import { useMemberName, type SpaceMember } from '@/entities/space'
import { UserAvatar } from '@/entities/user'

interface ParticipantPickerProps {
  members: SpaceMember[]
  selected: string[]
  onChange: (participantIds: string[]) => void
}

/**
 * Who takes part, picked among the space's members — one avatar and name each, pressed when taking
 * part: initials alone cannot tell two people apart. Only ever shown in a shared context: in a personal
 * one its owner always takes part.
 */
export function ParticipantPicker({ members, selected, onChange }: ParticipantPickerProps) {
  const { t } = useTranslation('calendar')
  const memberName = useMemberName(members)
  return (
    <div>
      <span className="mb-1 block text-xs font-semibold text-fg-2">{t('form.participants')}</span>
      <div className="flex flex-wrap gap-1.5">
        {members.map((member) => {
          const isSelected = selected.includes(member.userId)
          return (
            <button key={member.userId} type="button" aria-pressed={isSelected}
              onClick={() => onChange(isSelected
                ? selected.filter((id) => id !== member.userId)
                : [...selected, member.userId])}
              className={`flex max-w-full items-center gap-1.5 rounded-full border py-0.5 pl-0.5 pr-2.5 text-[13px] font-medium transition-colors ${
                isSelected ? 'border-accent bg-accent-dim text-fg-0' : 'border-border text-fg-2 hover:bg-bg-2'}`}>
              <UserAvatar userId={member.userId} username={memberName(member.userId)} className="size-6 rounded-full text-[10px]" />
              <span className="truncate">{memberName(member.userId)}</span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
