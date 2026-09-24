import { useTranslation } from 'react-i18next'
import type { SpaceMember } from '@/entities/space'
import { UserAvatar } from '@/entities/user'

interface ParticipantPickerProps {
  members: SpaceMember[]
  selected: string[]
  onChange: (participantIds: string[]) => void
}

/**
 * Who takes part, picked among the space's members — one avatar each, pressed when taking part.
 * Only ever shown in a shared context: in a personal one its owner always takes part.
 */
export function ParticipantPicker({ members, selected, onChange }: ParticipantPickerProps) {
  const { t } = useTranslation('calendar')
  return (
    <div>
      <span className="mb-1 block text-xs font-semibold text-fg-2">{t('form.participants')}</span>
      <div className="flex flex-wrap gap-1.5">
        {members.map((member) => {
          const isSelected = selected.includes(member.userId)
          return (
            <button key={member.userId} type="button" aria-pressed={isSelected} aria-label={member.username ?? '?'}
              onClick={() => onChange(isSelected
                ? selected.filter((id) => id !== member.userId)
                : [...selected, member.userId])}
              className={`rounded-full border p-0.5 ${isSelected ? 'border-accent' : 'border-transparent opacity-50'}`}>
              <UserAvatar username={member.username ?? '?'} role="USER" className="size-7 rounded-full text-[11px]" />
            </button>
          )
        })}
      </div>
    </div>
  )
}
